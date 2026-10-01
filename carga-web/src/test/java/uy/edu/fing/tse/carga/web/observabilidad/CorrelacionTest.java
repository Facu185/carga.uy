package uy.edu.fing.tse.carga.web.observabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelacionTest {

    @AfterEach
    void limpiar() {
        MDC.remove(Correlacion.CLAVE_MDC);
        MDC.remove(Correlacion.CLAVE_USUARIO_MDC);
    }

    @Test
    void usaElIdRecibidoSiEsValido() {
        assertEquals("abc-123-def", Correlacion.validarOGenerar(" abc-123-def "));
    }

    @Test
    void generaUnoNuevoSiFaltaOEsSospechoso() {
        assertEquals(36, Correlacion.validarOGenerar(null).length());
        assertEquals(36, Correlacion.validarOGenerar("corto").length());
        // Un ID con saltos de linea podria falsificar lineas en el log: se descarta
        assertNotEquals("abcdefgh\nFALSO", Correlacion.validarOGenerar("abcdefgh\nFALSO"));
    }

    @Test
    void ejecutarConPoneElIdYDespuesLoSaca() throws Exception {
        assertEquals("id-de-prueba", Correlacion.ejecutarCon("id-de-prueba", () -> Correlacion.actual().orElse(null)));
        assertTrue(Correlacion.actual().isEmpty());
        assertEquals("sin-id", Correlacion.ejecutarCon(null, () -> "sin-id"));
    }

    /** Pedido y respuesta falsos con Proxy: solo implementan lo que usa el filtro. */
    @SuppressWarnings("unchecked")
    private static <T> T falso(Class<T> tipo, Map<String, Object> respuestas) {
        return (T) Proxy.newProxyInstance(CorrelacionTest.class.getClassLoader(), new Class<?>[]{tipo},
                (proxy, metodo, args) -> {
                    if (metodo.getName().equals("setHeader")) {
                        respuestas.put("header:" + args[0], args[1]);
                        return null;
                    }
                    return respuestas.get(metodo.getName());
                });
    }

    @Test
    void elFiltroPoneElIdDuranteElPedidoLoDevuelveYDespuesLoSaca() throws Exception {
        Map<String, Object> datosPedido = new HashMap<>(Map.of(
                "getHeader", "pedido-12345678",
                "getRequestURI", "/carga/api/funcionarios",
                "getMethod", "GET",
                "getUserPrincipal", (Principal) () -> "admin"));
        Map<String, Object> datosRespuesta = new HashMap<>(Map.of("getStatus", 200));
        HttpServletRequest pedido = falso(HttpServletRequest.class, datosPedido);
        HttpServletResponse respuesta = falso(HttpServletResponse.class, datosRespuesta);

        AtomicReference<Object> idDuranteElPedido = new AtomicReference<>();
        AtomicReference<Object> usuarioDuranteElPedido = new AtomicReference<>();
        FilterChain cadena = (req, res) -> {
            idDuranteElPedido.set(MDC.get(Correlacion.CLAVE_MDC));
            usuarioDuranteElPedido.set(MDC.get(Correlacion.CLAVE_USUARIO_MDC));
        };

        new FiltroCorrelacion().doFilter(pedido, respuesta, cadena);

        assertEquals("pedido-12345678", idDuranteElPedido.get());
        assertEquals("admin", usuarioDuranteElPedido.get());
        assertEquals("pedido-12345678", datosRespuesta.get("header:" + Correlacion.HEADER));
        assertNull(MDC.get(Correlacion.CLAVE_MDC), "no queda pegado al hilo para el proximo pedido");
        assertNull(MDC.get(Correlacion.CLAVE_USUARIO_MDC));
    }
}
