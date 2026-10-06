package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class GuiaPeriodoTest {
    @Test void aceptaIntervalosConsecutivosSemiabiertos() {
        assertFalse(Guia.seSolapan(Instant.parse("2026-10-06T10:00:00Z"), Instant.parse("2026-10-06T11:00:00Z"),
                Instant.parse("2026-10-06T11:00:00Z"), Instant.parse("2026-10-06T12:00:00Z")));
    }
    @Test void detectaInterseccionReal() {
        assertTrue(Guia.seSolapan(Instant.parse("2026-10-06T10:00:00Z"), Instant.parse("2026-10-06T12:00:00Z"),
                Instant.parse("2026-10-06T11:00:00Z"), Instant.parse("2026-10-06T13:00:00Z")));
    }
    @Test void rechazaPeriodoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Guia.validarPeriodo(Instant.parse("2026-10-06T12:00:00Z"), Instant.parse("2026-10-06T12:00:00Z")));
    }
}
