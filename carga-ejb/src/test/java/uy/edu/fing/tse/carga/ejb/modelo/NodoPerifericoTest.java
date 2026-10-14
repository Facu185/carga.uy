package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NodoPerifericoTest {

    @Test
    void unNodoBalanzasNuevoNaceDesactivadoYConLosDatosLimpios() throws NegocioException {
        NodoPeriferico n = NodoPeriferico.nuevoBalanzas("  Balanzas norte ", " https://balanzas-norte.mtop.uy/api/pesadas ");
        assertEquals("Balanzas norte", n.getNombre());
        assertEquals("https://balanzas-norte.mtop.uy/api/pesadas", n.getUrl());
        assertEquals(TipoNodo.BALANZAS, n.getTipo());
        assertFalse(n.isActivo());
    }

    @Test
    void laUrlTieneQueSerHttpOHttpsConServidor() {
        for (String mala : new String[]{null, " ", "balanzas:8080/api", "ftp://balanzas/api", "http://", "http:// con espacios"}) {
            assertThrows(NegocioException.class, () -> NodoPeriferico.nuevoBalanzas("Nodo", mala), "acepto: " + mala);
        }
    }

    @Test
    void elNombreEsObligatorioYTieneLargoMaximo() {
        assertThrows(NegocioException.class, () -> NodoPeriferico.nuevoBalanzas(" ", "http://b/api"));
        assertThrows(NegocioException.class, () -> NodoPeriferico.nuevoBalanzas("x".repeat(101), "http://b/api"));
    }
}
