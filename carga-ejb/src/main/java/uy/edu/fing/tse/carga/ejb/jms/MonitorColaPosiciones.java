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
    private static final String COLA = "cola_posiciones";
    private static final Logger LOG = Logger.getLogger(MonitorColaPosiciones.class.getName());

    @EJB
    MetricasLocal metricas;

    private volatile ObjectName colaEncontrada;
    private volatile boolean avisado;

    @PostConstruct
    void registrar() {
        metricas.registrarMedidor(METRICA, this::pendientes);
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
}
