package uy.edu.fing.tse.carga.ejb.jms;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MonitorColaPosiciones {

    static final String METRICA = "carga_tracking_cola_pendientes";
    static final String METRICA_SATURADA = "carga_tracking_saturada";
    private static final String COLA = "cola_posiciones";
    private static final Logger LOG = Logger.getLogger(MonitorColaPosiciones.class.getName());

    @EJB
    MetricasLocal metricas;

    private volatile ObjectName colaEncontrada;
    private volatile boolean avisado;
    private long umbralAlto;
    private long umbralBajo;
    private long cacheMs;
    private int reintentarEnSegundos;
    private boolean saturada;
    private long leidoEnMs;
    private boolean medido;

    @PostConstruct
    void registrar() {
        configurar(entero("TRACKING_COLA_UMBRAL_ALTO", 18000), entero("TRACKING_COLA_UMBRAL_BAJO", 9000),
            1000, entero("TRACKING_REINTENTAR_SEG", 30));
        metricas.registrarMedidor(METRICA, this::pendientes);
        metricas.registrarMedidor(METRICA_SATURADA, () -> saturada() ? 1 : 0);
        LOG.info("Saturacion de la ingesta: se rechaza desde " + umbralAlto + " pendientes y se vuelve a"
            + " aceptar en " + umbralBajo + " (Retry-After " + reintentarEnSegundos + " s)");
    }

    void configurar(long umbralAlto, long umbralBajo, long cacheMs, int reintentarEnSegundos) {
        this.umbralAlto = umbralAlto;
        this.umbralBajo = Math.min(umbralBajo, umbralAlto);
        this.cacheMs = cacheMs;
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public synchronized boolean saturada() {
        long ahora = ahoraMs();
        if (!medido || ahora - leidoEnMs >= cacheMs) {
            actualizarEstado(pendientes());
            leidoEnMs = ahora;
            medido = true;
        }
        return saturada;
    }

    public int reintentarEnSegundos() {
        return reintentarEnSegundos;
    }

    private void actualizarEstado(long pendientes) {
        if (pendientes < 0) {
            return;
        }
        if (!saturada && pendientes >= umbralAlto) {
            saturada = true;
            LOG.warning("Ingesta de posiciones SATURADA: " + pendientes + " pendientes (umbral " + umbralAlto
                + "). Se responde 503 hasta bajar a " + umbralBajo + ".");
        } else if (saturada && pendientes <= umbralBajo) {
            saturada = false;
            LOG.warning("Ingesta de posiciones normalizada: " + pendientes + " pendientes. Se vuelve a aceptar.");
        }
    }

    long ahoraMs() {
        return System.nanoTime() / 1_000_000;
    }

    long pendientes() {
        try {
            MBeanServer servidor = ManagementFactory.getPlatformMBeanServer();
            ObjectName cola = colaEncontrada != null ? colaEncontrada : buscar(servidor);
            if (cola == null) {
                return -1;
            }
            return ((Number) servidor.getAttribute(cola, "MessageCount")).longValue();
        } catch (Exception e) {
            colaEncontrada = null;
            return -1;
        }
    }

    private ObjectName buscar(MBeanServer servidor) throws Exception {
        for (ObjectName n : servidor.queryNames(new ObjectName("org.apache.activemq.artemis:*"), null)) {
            String cola = n.getKeyProperty("queue");
            if ("queues".equals(n.getKeyProperty("subcomponent")) && cola != null
                && cola.replace("\"", "").endsWith(COLA)) {
                colaEncontrada = n;
                return n;
            }
        }
        if (!avisado) {
            avisado = true;
            LOG.warning("No se encontro la cola " + COLA + " por JMX: " + METRICA + " va a informar -1."
                + " Revisar jmx-management-enabled en configure.cli.");
        }
        return null;
    }

    private static int entero(String variable, int porDefecto) {
        String v = System.getenv(variable);
        try {
            return v == null || v.isBlank() ? porDefecto : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
