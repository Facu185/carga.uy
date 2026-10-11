package uy.edu.fing.tse.carga.web.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroTokenNodoTest {

    private final FiltroTokenNodo filtro = new FiltroTokenNodo("token-del-nodo");

    @Test
    void aceptaElTokenCorrecto() {
        assertTrue(filtro.autorizado("token-del-nodo"));
        assertTrue(filtro.autorizado("  token-del-nodo  "));
    }

    @Test
    void rechazaSinTokenOConUnoDistinto() {
        assertFalse(filtro.autorizado(null));
        assertFalse(filtro.autorizado(""));
        assertFalse(filtro.autorizado("otro-token"));
        assertFalse(filtro.autorizado("token-del-nodo-y-algo-mas"));
        assertFalse(filtro.autorizado("TOKEN-DEL-NODO"));
    }

    @Test
    void sinTokenConfiguradoNoAceptaANadie() {
        FiltroTokenNodo sinConfigurar = new FiltroTokenNodo("   ");
        assertFalse(sinConfigurar.autorizado("   "));
        assertFalse(sinConfigurar.autorizado(""));
        assertFalse(sinConfigurar.autorizado("cualquier-cosa"));
    }
}
