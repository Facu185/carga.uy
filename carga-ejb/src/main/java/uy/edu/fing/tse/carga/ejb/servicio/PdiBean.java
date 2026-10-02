package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.soap.SOAPBinding;
import uy.edu.fing.tse.carga.api.MetricasLocal;
import uy.edu.fing.tse.carga.api.PdiLocal;
import uy.edu.fing.tse.carga.api.dto.PersonaDTO;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;
import uy.edu.fing.tse.carga.api.observabilidad.Traza;
import uy.edu.fing.tse.carga.api.soap.ServicioDnic;
import uy.edu.fing.tse.carga.ejb.integracion.CircuitBreaker;
import uy.edu.fing.tse.carga.ejb.integracion.EncabezadoCorrelacionHandler;

import javax.xml.namespace.QName;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Cliente SOAP del servicio de DNIC publicado a traves de la PDI (aca, del modulo simulador-pdi).
 * La comunicacion con la PDI debe ser SOAP segun la Seccion 4.1 de la letra.
 *
 * Contrato (docs/contratos.md): devuelve la persona, o null si la PDI no responde. Quien llama sigue
 * en modo degradado (flujo alternativo 7a del caso de uso de autenticacion). Tres protecciones:
 * 1. Timeout: cada llamada tiene un tope (PDI_TIMEOUT_MS), en el cliente SOAP y ademas con un Future,
 *    asi el central nunca queda colgado aunque la libreria ignore el timeout.
 * 2. Reintentos: solo ante fallas tecnicas, con pausa creciente (PDI_REINTENTOS).
 * 3. Circuit breaker: tras varias fallas seguidas deja de llamar un rato y responde al instante.
 *
 * Singleton y no Stateless: el estado del circuito tiene que ser uno solo por instancia del central,
 * no uno por cada bean del pool. El circuito ya es thread-safe, por eso la concurrencia es BEAN.
 * LocalBean: ademas de PdiLocal expone la propia clase, para que el chequeo de salud lea el circuito.
 */
@Singleton
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PdiBean implements PdiLocal {

    private static final Logger LOG = Logger.getLogger(PdiBean.class.getName());
    private static final String NAMESPACE = "http://pdi.carga.uy/";
    private static final Duration PAUSA_BASE = Duration.ofMillis(200);

    static final String METRICA_CONSULTAS = "carga_pdi_consultas_total";
    static final String METRICA_DURACION = "carga_pdi_duracion_ms";

    @Resource
    ManagedExecutorService executor;

    @EJB
    MetricasLocal metricas;

    private String endpoint;
    private Duration timeout;
    private int reintentos;
    CircuitBreaker circuito;
    private volatile ServicioDnic puerto;

    @PostConstruct
    void configurar() {
        String wsdl = System.getenv().getOrDefault(
                "SIMULADOR_PDI_URL", "http://localhost:8080/simulador-pdi/ServicioDnicImpl?wsdl");
        configurar(wsdl,
                Duration.ofMillis(entero("PDI_TIMEOUT_MS", 3000)),
                entero("PDI_REINTENTOS", 1),
                new CircuitBreaker("PDI", entero("PDI_CIRCUITO_UMBRAL", 3),
                        Duration.ofSeconds(entero("PDI_CIRCUITO_ESPERA_SEG", 30)), Clock.systemUTC()));
        metricas.registrarMedidor("carga_pdi_circuito_abierto",
                () -> circuito.estado() == CircuitBreaker.Estado.CERRADO ? 0 : 1);
    }

    void configurar(String url, Duration timeout, int reintentos, CircuitBreaker circuito) {
        // La variable trae la URL del WSDL; el endpoint es la misma sin "?wsdl"
        this.endpoint = url.endsWith("?wsdl") ? url.substring(0, url.length() - "?wsdl".length()) : url;
        this.timeout = timeout;
        this.reintentos = reintentos;
        this.circuito = circuito;
    }

    @Override
    public PersonaDTO obtenerPersona(String cedula) {
        if (!circuito.permiteLlamada()) {
            LOG.warning("PDI con fallas sostenidas: circuito abierto, se responde sin consultar");
            metricas.contar(METRICA_CONSULTAS, "resultado", "circuito_abierto");
            return null;
        }
        long inicio = System.nanoTime();
        for (int intento = 0; intento <= reintentos; intento++) {
            if (intento > 0) {
                pausar(PAUSA_BASE.multipliedBy(intento));
            }
            try {
                PersonaDTO persona = invocar(cedula);
                circuito.registrarExito();
                metricas.contar(METRICA_CONSULTAS, "resultado", "ok");
                metricas.observarDuracion(METRICA_DURACION, (System.nanoTime() - inicio) / 1_000_000);
                return persona;
            } catch (Exception e) {
                LOG.warning("Falla consultando la PDI (intento " + (intento + 1) + "): " + e);
            }
        }
        circuito.registrarFalla();
        metricas.contar(METRICA_CONSULTAS, "resultado", "falla");
        return null;
    }

    /** Lo usa el chequeo de salud: con el circuito abierto, la PDI queda DEGRADADA. */
    public CircuitBreaker.Estado estadoCircuito() {
        return circuito.estado();
    }

    /** Llamada SOAP con tope de tiempo. Package-private para poder simular fallas en las pruebas. */
    PersonaDTO invocar(String cedula) throws Exception {
        try (Traza.Tramo tramo = Traza.iniciar("PDI ServicioDnic.obtenerPersona", Traza.Tipo.CLIENTE)
                .atributo("rpc.system", "soap").atributo("peer.service", "pdi")) {
            // La llamada corre en otro hilo: se le pasan el ID de correlacion y el contexto de la traza
            // para que viajen como headers HTTP hasta la PDI
            String idCorrelacion = Correlacion.actual().orElse(null);
            String traceparent = tramo.traceparent();
            Future<PersonaDTO> futuro = executor.submit(() -> Traza.ejecutarCon(traceparent,
                    () -> Correlacion.ejecutarCon(idCorrelacion, () -> puerto().obtenerPersona(cedula))));
            try {
                return futuro.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                futuro.cancel(true);
                tramo.error(e);
                throw e;
            } catch (ExecutionException e) {
                Exception causa = e.getCause() instanceof Exception c ? c : e;
                tramo.error(causa);
                throw causa;
            }
        }
    }

    private ServicioDnic puerto() {
        ServicioDnic actual = puerto;
        if (actual != null) {
            return actual;
        }
        synchronized (this) {
            if (puerto == null) {
                // Sin descargar el WSDL en cada llamada: el contrato ya esta en carga-api
                QName nombrePuerto = new QName(NAMESPACE, "ServicioDnicPort");
                Service servicio = Service.create(new QName(NAMESPACE, "ServicioDnicService"));
                servicio.addPort(nombrePuerto, SOAPBinding.SOAP11HTTP_BINDING, endpoint);
                ServicioDnic nuevo = servicio.getPort(nombrePuerto, ServicioDnic.class);
                Map<String, Object> contexto = ((BindingProvider) nuevo).getRequestContext();
                contexto.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
                String ms = String.valueOf(timeout.toMillis());
                contexto.put("jakarta.xml.ws.client.connectionTimeout", ms);
                contexto.put("jakarta.xml.ws.client.receiveTimeout", ms);
                contexto.put("javax.xml.ws.client.connectionTimeout", ms);
                contexto.put("javax.xml.ws.client.receiveTimeout", ms);
                @SuppressWarnings("rawtypes")
                List<Handler> manejadores = new ArrayList<>();
                manejadores.add(new EncabezadoCorrelacionHandler());
                ((BindingProvider) nuevo).getBinding().setHandlerChain(manejadores);
                puerto = nuevo;
            }
            return puerto;
        }
    }

    private static void pausar(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
