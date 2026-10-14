package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidacionPosicionTest {

    private static final Instant AHORA = Instant.parse("2026-10-14T15:00:00Z");

    @Test
    void unaPosicionCompletaEsValida() {
        assertTrue(ValidacionPosicion.problema(posicion(), AHORA).isEmpty());
    }

    @Test
    void unaPosicionViejaEsValida() {
        PosicionDTO p = posicion();
        p.setInstante("2026-10-13T08:00:00-03:00");
        assertTrue(ValidacionPosicion.problema(p, AHORA).isEmpty());
    }

    @Test
    void rechazaLaPosicionNula() {
        assertFalse(ValidacionPosicion.problema(null, AHORA).isEmpty());
    }

    @Test
    void rechazaIdEventoVacioOLargo() {
        PosicionDTO p = posicion();
        p.setIdEvento("  ");
        assertEquals("idEvento es obligatorio.", ValidacionPosicion.problema(p, AHORA).orElseThrow());
        p.setIdEvento("x".repeat(81));
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
    }

    @Test
    void rechazaMatriculaVaciaOLarga() {
        PosicionDTO p = posicion();
        p.setMatricula(null);
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
        p.setMatricula("M".repeat(21));
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
    }

    @Test
    void rechazaCoordenadasFueraDeRango() {
        PosicionDTO p = posicion();
        p.setLatitud(-91);
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
        p = posicion();
        p.setLongitud(180.5);
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
        p = posicion();
        p.setLatitud(Double.NaN);
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
    }

    @Test
    void rechazaInstanteFaltanteMalFormadoOFuturo() {
        PosicionDTO p = posicion();
        p.setInstante(null);
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
        p.setInstante("14/10/2026 12:00");
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
        p.setInstante("2026-10-14T12:00:00");          // sin zona horaria
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
        p.setInstante("2026-10-14T12:10:00-03:00");    // 10 min adelantado
        assertFalse(ValidacionPosicion.problema(p, AHORA).isEmpty());
    }

    @Test
    void toleraUnRelojLevementeAdelantado() {
        PosicionDTO p = posicion();
        p.setInstante("2026-10-14T12:03:00-03:00");    // 3 min adelantado
        assertTrue(ValidacionPosicion.problema(p, AHORA).isEmpty());
    }

    static PosicionDTO posicion() {
        PosicionDTO p = new PosicionDTO();
        p.setIdEvento("evt-1");
        p.setMatricula("STZ1234");
        p.setLatitud(-34.9);
        p.setLongitud(-56.16);
        p.setInstante("2026-10-14T11:55:00-03:00");
        return p;
    }
}
