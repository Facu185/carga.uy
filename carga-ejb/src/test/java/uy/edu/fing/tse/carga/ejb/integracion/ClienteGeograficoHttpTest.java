package uy.edu.fing.tse.carga.ejb.integracion;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** HTTP local sintetico; no usa red externa ni credenciales reales. No ejecutada. */
class ClienteGeograficoHttpTest {
    @Test void noSigueRedireccionesNiReenviaClaveAOtroDestino() throws Exception {
        var s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var visitas = new AtomicInteger();
        s.createContext("/origen", e -> {
            assertEquals("sintetica", e.getRequestHeaders().getFirst("Authorization"));
            e.getResponseHeaders().add("Location", "/destino"); e.sendResponseHeaders(302, -1); e.close();
        });
        s.createContext("/destino", e -> { visitas.incrementAndGet(); e.sendResponseHeaders(200, -1); e.close(); });
        s.start();
        try {
            var respuesta = new ClienteGeograficoHttp().solicitar(url(s, "/origen"), "POST", "{}", "sintetica");
            assertEquals(302, respuesta.estado()); assertEquals(0, visitas.get());
        } finally { s.stop(0); }
    }

    @Test void rechazaRespuestaSobredimensionada() throws Exception {
        var s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/grande", e -> {
            byte[] bytes = new byte[2 * 1024 * 1024 + 1]; e.sendResponseHeaders(200, bytes.length);
            try (var salida = e.getResponseBody()) { salida.write(bytes); }
        });
        s.start();
        try {
            assertThrows(ServicioNoDisponibleException.class,
                    () -> new ClienteGeograficoHttp().solicitar(url(s,"/grande"), "GET", null, null));
        } finally { s.stop(0); }
    }

    @Test void timeoutNoIncluyeClaveNiContenidoExternoEnLaExcepcion() throws Exception {
        var liberar = new CountDownLatch(1);
        var s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/lento", e -> {
            try { liberar.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            e.close();
        }); s.start();
        try {
            var cliente = new ClienteGeograficoHttp(200, 100);
            var error = assertThrows(ServicioNoDisponibleException.class,
                    () -> cliente.solicitar(url(s,"/lento"), "POST", "{}", "secreto-sintetico"));
            assertFalse(error.getMessage().contains("secreto")); assertNull(error.getCause());
        } finally { liberar.countDown(); s.stop(0); }
    }

    private static URI url(HttpServer s, String ruta) {
        return URI.create("http://127.0.0.1:" + s.getAddress().getPort() + ruta);
    }
}
