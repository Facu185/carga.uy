package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiaTest {

    @Test
    void aceptaDatosObligatoriosConOrigenYDestinoDistintos() {
        assertDoesNotThrow(() -> Guia.validarDatos(new BigDecimal("100"), new BigDecimal("10.50"), LocalDate.of(2026, 10, 4),
                "Montevideo", "Salto"));
    }

    @Test
    void rechazaOrigenYDestinoIguales() {
        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarDatos(BigDecimal.ONE, BigDecimal.ONE, LocalDate.of(2026, 10, 4), "Montevideo", " Montevideo "));
    }

    @Test
    void rechazaPesoOVolumenNoPositivosODatosObligatoriosAusentes() {
        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarDatos(BigDecimal.ZERO, BigDecimal.ONE, LocalDate.of(2026, 10, 4), "Montevideo", "Salto"));
        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarDatos(BigDecimal.ONE, BigDecimal.ZERO, LocalDate.of(2026, 10, 4), "Montevideo", "Salto"));
        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarDatos(BigDecimal.ONE, BigDecimal.ONE, null, "Montevideo", "Salto"));
    }
}
