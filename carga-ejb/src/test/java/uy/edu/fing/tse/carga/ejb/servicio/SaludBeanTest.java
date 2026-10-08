package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.EstadoSaludDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaludBeanTest {

    private static SaludBean salud(boolean baseOk, String pdiEstado) {
        return salud(baseOk, pdiEstado, SaludBean.UP);
    }

    private static SaludBean salud(boolean baseOk, String pdiEstado, String balanzasEstado) {
        return new SaludBean() {
            @Override
            boolean baseDeDatosResponde() {
                return baseOk;
            }

            @Override
            String estadoPdi() {
                return pdiEstado;
            }

            @Override
            String estadoBalanzas() {
                return balanzasEstado;
            }
        };
    }

    @Test
    void conTodoAndandoEstaUp() {
        EstadoSaludDTO estado = salud(true, SaludBean.UP).verificar();
        assertEquals("UP", estado.getEstado());
        assertEquals("UP", estado.getComponentes().get("baseDeDatos"));
    }

    @Test
    void conLaPdiCaidaSigueUpEnModoDegradado() {
        EstadoSaludDTO estado = salud(true, SaludBean.DEGRADADO).verificar();
        assertEquals("UP", estado.getEstado());
        assertEquals("DEGRADADO", estado.getComponentes().get("pdi"));
    }

    @Test
    void conElNodoBalanzasCaidoSigueUpEnModoDegradado() {
        EstadoSaludDTO estado = salud(true, SaludBean.UP, SaludBean.DEGRADADO).verificar();
        assertEquals("UP", estado.getEstado());
        assertEquals("DEGRADADO", estado.getComponentes().get("balanzas"));
    }

    @Test
    void sinBaseDeDatosEstaDown() {
        assertEquals("DOWN", salud(false, SaludBean.UP).verificar().getEstado());
    }
}
