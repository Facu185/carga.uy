package uy.edu.fing.tse.carga.web.seguridad;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SesionJsfTest {
    private final HttpJsfDoble http = new HttpJsfDoble();

    @Test
    void noEligeEntreCookiesHomonimasNiAceptaUnaVacia() {
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "a"), new Cookie(SesionJsf.COOKIE, "b")};
        assertNull(SesionJsf.token(http.pedido));
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "")};
        assertNull(SesionJsf.token(http.pedido));
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, null), new Cookie(SesionJsf.COOKIE, "b")};
        assertNull(SesionJsf.token(http.pedido));
    }

    @Test
    void origenComparaEsquemaHostYPuertoNoPrefijos() {
        http.metodo = "POST";
        for (String origen : new String[]{"null", "https://localhost:8080", "http://localhost", "http://localhost:8081",
                "http://localhost.ejemplo.invalid:8080", "http://usuario@localhost:8080", "no es una uri"}) {
            http.headers.put("Origin", origen);
            assertFalse(SesionJsf.origenPermitido(http.pedido), origen);
        }
        http.headers.put("Origin", "http://localhost:8080");
        assertTrue(SesionJsf.origenPermitido(http.pedido));
        http.seguro = true;
        http.headers.put("Origin", "https://localhost");
        assertTrue(SesionJsf.origenPermitido(http.pedido));
    }
}
