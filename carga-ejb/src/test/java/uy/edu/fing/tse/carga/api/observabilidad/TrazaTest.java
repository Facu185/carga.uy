package uy.edu.fing.tse.carga.api.observabilidad;

import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** En el paquete de la clase (aunque viva en carga-ejb) para poder ver los datos internos del tramo. */
class TrazaTest {

    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PADRE = "00f067aa0ba902b7";

    private final List<Traza.Tramo> exportados = new CopyOnWriteArrayList<>();

    @BeforeEach
    void capturar() {
        Traza.exportarA(exportados::add);
        Traza.usarServicio("central");
    }

    @AfterEach
    void soltar() {
        Traza.exportarA(t -> { });
    }

    @Test
    void sinContextoEmpiezaUnaTrazaNueva() {
        try (Traza.Tramo t = Traza.iniciarDesde(null, "GET /api/salud", Traza.Tipo.SERVIDOR)) {
            assertTrue(t.traceId.matches("[0-9a-f]{32}"));
            assertTrue(t.spanId.matches("[0-9a-f]{16}"));
            assertNull(t.padreId);
        }
    }

    @Test
    void continuaElTraceparentRecibidoYDescartaUnoInvalido() {
        try (Traza.Tramo t = Traza.iniciarDesde("00-" + TRACE + "-" + PADRE + "-01", "x", Traza.Tipo.SERVIDOR)) {
            assertEquals(TRACE, t.traceId);
            assertEquals(PADRE, t.padreId);
        }
        try (Traza.Tramo t = Traza.iniciarDesde("00-" + "0".repeat(32) + "-" + PADRE + "-01", "x", Traza.Tipo.SERVIDOR)) {
            assertNotEquals("0".repeat(32), t.traceId, "un traceId en ceros es invalido segun W3C");
            assertNull(t.padreId);
        }
    }

    @Test
    void losTramosAnidadosQuedanEncadenadosYAlCerrarSeVuelveAlAnterior() {
        try (Traza.Tramo servidor = Traza.iniciarDesde(null, "GET /api/auth/gubuy/callback", Traza.Tipo.SERVIDOR)) {
            try (Traza.Tramo cliente = Traza.iniciar("PDI ServicioDnic.obtenerPersona", Traza.Tipo.CLIENTE)) {
                assertEquals(servidor.traceId, cliente.traceId);
                assertEquals(servidor.spanId, cliente.padreId);
                assertEquals(cliente.traceparent(), Traza.traceparentActual().orElseThrow());
                assertEquals(cliente.traceId, MDC.get(Traza.CLAVE_MDC_TRAZA));
            }
            assertEquals(servidor.traceparent(), Traza.traceparentActual().orElseThrow());
        }
        assertTrue(Traza.traceparentActual().isEmpty());
        assertNull(MDC.get(Traza.CLAVE_MDC_TRAZA), "no queda pegado al hilo para el proximo pedido");
        assertEquals(2, exportados.size());
    }

    @Test
    void elContextoSePasaAOtroHilo() throws Exception {
        ExecutorService otroHilo = Executors.newSingleThreadExecutor();
        try (Traza.Tramo cliente = Traza.iniciarDesde(null, "PDI", Traza.Tipo.CLIENTE)) {
            String traceparent = cliente.traceparent();
            String recibido = otroHilo.submit(() -> Traza.ejecutarCon(traceparent,
                    () -> Traza.traceparentActual().orElse(null))).get();
            assertEquals(traceparent, recibido);
            assertTrue(otroHilo.submit(() -> Traza.traceparentActual().isEmpty()).get(), "y despues se limpia");
        } finally {
            otroHilo.shutdown();
        }
    }

    @Test
    void elJsonOtlpLlevaServicioPadreTipoYEstado() {
        try (Traza.Tramo s = Traza.iniciarDesde("00-" + TRACE + "-" + PADRE + "-01", "GET /api/\"x\"", Traza.Tipo.SERVIDOR)) {
            s.atributo("http.status_code", 503).marcarError();
        }
        String json = ExportadorOtlp.json(exportados);

        assertTrue(json.contains("{\"key\":\"service.name\",\"value\":{\"stringValue\":\"central\"}}"));
        assertTrue(json.contains("\"traceId\":\"" + TRACE + "\""));
        assertTrue(json.contains("\"parentSpanId\":\"" + PADRE + "\""));
        assertTrue(json.contains("\"name\":\"GET /api/\\\"x\\\"\""), "el nombre va escapado");
        assertTrue(json.contains("\"kind\":2"));
        assertTrue(json.contains("\"status\":{\"code\":2}"));
        assertTrue(json.contains("{\"key\":\"http.status_code\",\"value\":{\"stringValue\":\"503\"}}"));
    }
}
