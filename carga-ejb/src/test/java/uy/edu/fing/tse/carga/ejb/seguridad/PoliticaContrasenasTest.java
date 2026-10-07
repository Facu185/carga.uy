package uy.edu.fing.tse.carga.ejb.seguridad;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliticaContrasenasTest {

    @Test
    void exigeUnMinimoDeCaracteres() {
        assertThrows(NegocioException.class, () -> PoliticaContrasenas.validar(null));
        assertThrows(NegocioException.class, () -> PoliticaContrasenas.validar("corta"));
        assertThrows(NegocioException.class, () -> PoliticaContrasenas.validar("             "));
        assertDoesNotThrow(() -> PoliticaContrasenas.validar("una-clave-segura"));
    }

    @Test
    void laTemporalCumpleLaPoliticaYEsFacilDeDictar() {
        String temporal = PoliticaContrasenas.generarTemporal();

        assertTrue(temporal.matches("[a-zA-Z2-9]{4}-[a-zA-Z2-9]{4}-[a-zA-Z2-9]{4}"));
        assertTrue(temporal.chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0), "sin caracteres ambiguos");
        assertDoesNotThrow(() -> PoliticaContrasenas.validar(temporal));
        assertNotEquals(temporal, PoliticaContrasenas.generarTemporal());
    }
}
