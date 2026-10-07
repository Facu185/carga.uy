package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.EstadoSaludDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaludBeanTest {

    private static SaludBean salud(boolean baseOk, String pdiEstado) {
        return new SaludBean() {
            @Override
            boolean baseDeDatosResponde() {
                return baseOk;
            }

            @Override
            String estadoPdi() {
                return pdiEstado;
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
    void sinBaseDeDatosEstaDown() {
        assertEquals("DOWN", salud(false, SaludBean.UP).verificar().getEstado());
    }
}
