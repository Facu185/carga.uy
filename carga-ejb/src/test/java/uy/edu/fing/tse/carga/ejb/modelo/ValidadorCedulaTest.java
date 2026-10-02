package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorCedulaTest {

    @Test
    void aceptaCedulasValidasConYSinFormato() {
        assertTrue(ValidadorCedula.esValida("12345672"));
        assertTrue(ValidadorCedula.esValida("1.234.567-2"));
        assertTrue(ValidadorCedula.esValida("1234567-2"));
    }

    @Test
    void aceptaCedulasDeSieteDigitos() {
        // 123456 + verificador: 0*2+1*9+2*8+3*7+4*6+5*3+6*4 = 109 -> (10-9)%10 = 1
        assertTrue(ValidadorCedula.esValida("1234561"));
    }

    @Test
    void rechazaDigitoVerificadorIncorrecto() {
        assertFalse(ValidadorCedula.esValida("12345670"));
    }

    @Test
    void rechazaFormatosInvalidos() {
        assertFalse(ValidadorCedula.esValida(null));
        assertFalse(ValidadorCedula.esValida("abc"));
        assertFalse(ValidadorCedula.esValida("123"));
        assertFalse(ValidadorCedula.esValida("123456789"));
    }

    @Test
    void rechazaPuntosFueraDeLugar() {
        assertNull(ValidadorCedula.normalizar("60000008."));
        assertNull(ValidadorCedula.normalizar(".60000008"));
        assertNull(ValidadorCedula.normalizar("6.0000008"));
        assertNull(ValidadorCedula.normalizar("1234.567-2"));
        assertNull(ValidadorCedula.normalizar("1234567--2"));
        assertFalse(ValidadorCedula.esValida("60000008."));
    }

    @Test
    void aceptaFormatosHabituales() {
        assertEquals("60000008", ValidadorCedula.normalizar("6.000.000-8"));
        assertEquals("1234561", ValidadorCedula.normalizar("123.456-1"));
        assertEquals("12345672", ValidadorCedula.normalizar(" 12345672 "));
    }

    @Test
    void normalizaQuitandoPuntosYGuiones() {
        assertEquals("12345672", ValidadorCedula.normalizar("1.234.567-2"));
        assertNull(ValidadorCedula.normalizar("12a45672"));
    }
}
