package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.contract.service.PesajeLocal;
import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoPesajeDTO;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;
import uy.edu.fing.tse.carga.api.observabilidad.Traza;
import uy.edu.fing.tse.carga.ejb.integracion.CircuitBreaker;
import uy.edu.fing.tse.carga.ejb.modelo.NodoPeriferico;
import uy.edu.fing.tse.carga.ejb.modelo.TipoNodo;
import uy.edu.fing.tse.carga.ejb.persistencia.NodoPerifericoDAO;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Cliente del nodo periferico Balanzas: integracion sincronica request-response
 * (Secciones 3.7.2 y 4.1 de la letra). Mismas tres protecciones que PdiBean (Seccion 4.3):
 * 1. Timeout de conexion y de lectura (BALANZAS_TIMEOUT_MS).
 * 2. Reintentos con pausa creciente (BALANZAS_REINTENTOS), solo ante fallas tecnicas: un 4xx es un
 *    pedido mal armado y repetirlo no lo arregla.
 * 3. El CircuitBreaker de integracion/: tras varias fallas seguidas deja de llamar al nodo un rato y
 *    responde al instante, asi un nodo caido no deja hilos del central esperando el timeout.
 *
 * Singleton y no Stateless por lo mismo que PdiBean: el circuito tiene que ser uno solo por central,
 * no uno por cada bean del pool. LocalBean para que salud y monitor lean el estado del circuito.
 */
@Singleton
@Startup
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PesajeBean implements PesajeLocal {

    private static final Logger LOG = Logger.getLogger(PesajeBean.class.getName());
    private static final Duration PAUSA_BASE = Duration.ofMillis(200);

    /** Con este header el central se identifica ante el nodo (ADR-0007). */
    static final String HEADER_TOKEN = "X-Nodo-Token";
    static final String METRICA_CONSULTAS = "carga_balanzas_consultas_total";
    static final String METRICA_DURACION = "carga_balanzas_duracion_ms";

    @EJB
    MetricasLocal metricas;

    /** Nodos registrados (ADR-0009). null en las pruebas: se usa la URL configurada. */
    @EJB
    NodoPerifericoDAO nodos;

    private String url;
    private String token;
    private Duration timeout;
    private int reintentos;
    CircuitBreaker circuito;

    @PostConstruct
    void configurar() {
        configurar(System.getenv().getOrDefault("NODO_BALANZAS_URL", "http://balanzas:8080/balanzas/api/pesadas"),
                Duration.ofMillis(entero("BALANZAS_TIMEOUT_MS", 3000)),
                entero("BALANZAS_REINTENTOS", 1),
                new CircuitBreaker("Balanzas", entero("BALANZAS_CIRCUITO_UMBRAL", 3),
                        Duration.ofSeconds(entero("BALANZAS_CIRCUITO_ESPERA_SEG", 30)), Clock.systemUTC()));
        token = System.getenv().getOrDefault("NODO_BALANZAS_TOKEN", "").trim();
        if (token.isEmpty()) {
            LOG.warning("NODO_BALANZAS_TOKEN no esta configurado: el nodo Balanzas va a rechazar las consultas");
        }
        metricas.registrarMedidor("carga_balanzas_circuito_abierto",
                () -> circuito.estado() == CircuitBreaker.Estado.CERRADO ? 0 : 1);
    }

    void configurar(String url, Duration timeout, int reintentos, CircuitBreaker circuito) {
        this.url = url;
        this.timeout = timeout;
        this.reintentos = reintentos;
        this.circuito = circuito;
    }

    @Override
    public List<PesadaDTO> obtenerPesadas(String matricula, String fecha) {
        return consultarPesadas(matricula, fecha).getPesadas();
    }

    @Override
    public ResultadoPesajeDTO consultarPesadas(String matricula, String fecha) {
        Optional<String> destino = urlDestino();
        if (destino.isEmpty()) {
            // Desactivado a proposito: no es una falla del nodo, no toca el circuito
            metricas.contar(METRICA_CONSULTAS, "resultado", "desactivado");
            return ResultadoPesajeDTO.noDisponible(
                    "El nodo Balanzas esta desactivado por un administrador: no se lo consulta.", 0);
        }
        if (!circuito.permiteLlamada()) {
            metricas.contar(METRICA_CONSULTAS, "resultado", "circuito_abierto");
            return ResultadoPesajeDTO.noDisponible(
                    "El nodo Balanzas viene fallando: circuito abierto, no se lo consulta por ahora.", 0);
        }
        long inicio = System.nanoTime();
        for (int intento = 0; intento <= reintentos; intento++) {
            if (intento > 0) {
                pausar(PAUSA_BASE.multipliedBy(intento));
            }
            try {
                List<PesadaDTO> pesadas = invocar(destino.get(), matricula, fecha);
                circuito.registrarExito();
                long ms = milisDesde(inicio);
                metricas.contar(METRICA_CONSULTAS, "resultado", "ok");
                metricas.observarDuracion(METRICA_DURACION, ms);
                return ResultadoPesajeDTO.disponible(pesadas, ms);
            } catch (PedidoRechazadoException e) {
                // El nodo esta vivo y contesto: cuenta como exito para el circuito (si no, en estado
                // semiabierto la prueba quedaria colgada). El pedido no se reintenta.
                circuito.registrarExito();
                metricas.contar(METRICA_CONSULTAS, "resultado", "rechazada");
                String motivo = e.estado == 401 || e.estado == 403
                        ? "El nodo Balanzas no acepto las credenciales del central (HTTP " + e.estado
                        + "): revisar NODO_BALANZAS_TOKEN."
                        : "El nodo Balanzas rechazo la consulta (HTTP " + e.estado + ").";
                return ResultadoPesajeDTO.noDisponible(motivo, milisDesde(inicio));
            } catch (Exception e) {
                LOG.warning("Falla consultando el nodo Balanzas (intento " + (intento + 1) + "): " + e);
            }
        }
        circuito.registrarFalla();
        metricas.contar(METRICA_CONSULTAS, "resultado", "falla");
        return ResultadoPesajeDTO.noDisponible(
                "El nodo Balanzas no respondio (" + (reintentos + 1) + " intento(s)).", milisDesde(inicio));
    }

    /** Lo usan el chequeo de salud y el panel: con el circuito abierto, Balanzas queda DEGRADADO. */
    public CircuitBreaker.Estado estadoCircuito() {
        return circuito.estado();
    }

    /**
     * Una llamada HTTP al nodo. Package-private para simular fallas en las pruebas.
     * Propaga el ID de correlacion y el contexto de la traza, asi la consulta se sigue en Jaeger
     * igual que la de la PDI.
     */
    /**
     * A donde consultar. La base manda: la URL del nodo Balanzas activo. Si hay nodos Balanzas
     * registrados pero ninguno activo, no se consulta (vacio). La variable NODO_BALANZAS_URL solo se
     * usa si no hay ninguno registrado. Package-private para las pruebas.
     */
    Optional<String> urlDestino() {
        if (nodos == null) {
            return Optional.of(url);
        }
        Optional<NodoPeriferico> activo = nodos.buscarActivo(TipoNodo.BALANZAS);
        if (activo.isPresent()) {
            return Optional.of(activo.get().getUrl());
        }
        return nodos.hayDeTipo(TipoNodo.BALANZAS) ? Optional.empty() : Optional.of(url);
    }

    List<PesadaDTO> invocar(String url, String matricula, String fecha) throws Exception {
        try (Traza.Tramo tramo = Traza.iniciar("Balanzas GET /pesadas", Traza.Tipo.CLIENTE)
                .atributo("http.method", "GET").atributo("peer.service", "balanzas")) {
            Client cliente = ClientBuilder.newBuilder()
                    .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .build();
            try {
                Invocation.Builder pedido = cliente.target(url)
                        .queryParam("matricula", matricula == null ? "" : matricula)
                        .queryParam("fecha", fecha == null ? "" : fecha)
                        .request(MediaType.APPLICATION_JSON)
                        .header(Traza.HEADER, tramo.traceparent());
                Correlacion.actual().ifPresent(id -> pedido.header(Correlacion.HEADER, id));
                if (token != null && !token.isEmpty()) {
                    pedido.header(HEADER_TOKEN, token);
                }
                try (Response respuesta = pedido.get()) {
                    int estado = respuesta.getStatus();
                    tramo.atributo("http.status_code", estado);
                    if (estado >= 400 && estado < 500) {
                        tramo.marcarError();
                        throw new PedidoRechazadoException(estado);
                    }
                    if (estado >= 500) {
                        tramo.marcarError();
                        throw new IllegalStateException("el nodo respondio HTTP " + estado);
                    }
                    return respuesta.readEntity(new GenericType<List<PesadaDTO>>() { });
                }
            } catch (PedidoRechazadoException e) {
                throw e;
            } catch (RuntimeException e) {
                tramo.error(e);
                throw e;
            } finally {
                cliente.close();
            }
        }
    }

    /** El nodo contesto 4xx: esta vivo, pero el pedido no sirve. */
    static final class PedidoRechazadoException extends RuntimeException {
        final int estado;

        PedidoRechazadoException(int estado) {
            super("HTTP " + estado);
            this.estado = estado;
        }
    }

    private static long milisDesde(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
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
