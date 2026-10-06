package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiaTest {

    @Test
    void aceptaDatosObligatoriosConOrigenYDestinoDistintos() {
        assertDoesNotThrow(() -> Guia.validarDatos(new BigDecimal("100"), new BigDecimal("10.50"), Instant.parse("2026-10-04T10:00:00Z"), Instant.parse("2026-10-04T11:00:00Z"),
                "Montevideo", "Salto"));
    }

    @Test
    void rechazaOrigenYDestinoIguales() {
        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarDatos(BigDecimal.ONE, BigDecimal.ONE, Instant.parse("2026-10-04T10:00:00Z"), Instant.parse("2026-10-04T11:00:00Z"), "Montevideo", " Montevideo "));
    }

    @Test
    void rechazaPesoOVolumenNoPositivosODatosObligatoriosAusentes() {
        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarDatos(BigDecimal.ZERO, BigDecimal.ONE, Instant.parse("2026-10-04T10:00:00Z"), Instant.parse("2026-10-04T11:00:00Z"), "Montevideo", "Salto"));
        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarDatos(BigDecimal.ONE, BigDecimal.ZERO, Instant.parse("2026-10-04T10:00:00Z"), Instant.parse("2026-10-04T11:00:00Z"), "Montevideo", "Salto"));
        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarDatos(BigDecimal.ONE, BigDecimal.ONE, null, Instant.parse("2026-10-04T11:00:00Z"), "Montevideo", "Salto"));
    }
}
