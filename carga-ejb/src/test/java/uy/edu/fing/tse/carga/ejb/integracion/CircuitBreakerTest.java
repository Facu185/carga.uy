package uy.edu.fing.tse.carga.ejb.integracion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitBreakerTest {

    /** Reloj que se puede adelantar a mano. */
    static class RelojManual extends Clock {
        Instant ahora = Instant.parse("2026-01-01T00:00:00Z");
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private RelojManual reloj;
    private CircuitBreaker circuito;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual();
        circuito = new CircuitBreaker("prueba", 3, Duration.ofSeconds(30), reloj);
    }

    @Test
    void seAbreTrasFallasSeguidas() {
        circuito.registrarFalla();
        circuito.registrarFalla();
        assertTrue(circuito.permiteLlamada());
        circuito.registrarFalla();
        assertEquals(CircuitBreaker.Estado.ABIERTO, circuito.estado());
        assertFalse(circuito.permiteLlamada());
    }

    @Test
    void unExitoReiniciaElConteo() {
        circuito.registrarFalla();
        circuito.registrarFalla();
        circuito.registrarExito();
        circuito.registrarFalla();
        assertEquals(CircuitBreaker.Estado.CERRADO, circuito.estado());
    }

    @Test
    void pasadoElTiempoDejaPasarUnaSolaPrueba() {
        abrir();
        reloj.avanzar(Duration.ofSeconds(31));

        assertTrue(circuito.permiteLlamada());
        assertEquals(CircuitBreaker.Estado.SEMI_ABIERTO, circuito.estado());
        assertFalse(circuito.permiteLlamada(), "solo una llamada de prueba a la vez");
    }

    @Test
    void pruebaExitosaCierraElCircuito() {
        abrir();
        reloj.avanzar(Duration.ofSeconds(31));
        circuito.permiteLlamada();
        circuito.registrarExito();
        assertEquals(CircuitBreaker.Estado.CERRADO, circuito.estado());
    }

    @Test
    void pruebaFallidaLoVuelveAAbrir() {
        abrir();
        reloj.avanzar(Duration.ofSeconds(31));
        circuito.permiteLlamada();
        circuito.registrarFalla();
        assertEquals(CircuitBreaker.Estado.ABIERTO, circuito.estado());
        assertFalse(circuito.permiteLlamada());
    }

    private void abrir() {
        for (int i = 0; i < 3; i++) {
            circuito.registrarFalla();
        }
    }
}
