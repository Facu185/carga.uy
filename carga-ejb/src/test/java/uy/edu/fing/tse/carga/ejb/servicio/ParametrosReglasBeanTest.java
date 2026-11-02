package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParametrosReglasBeanTest {

    @Test
    void aceptaNumerosNoNegativosYNormalizaElFormato() throws NegocioException {
        assertEquals("600", ParametrosReglasBean.validar("exceso_carga.tolerancia_kg", " 600 "));
        assertEquals("12.5", ParametrosReglasBean.validar("desvio_ruta.tolerancia_relativa_pct", "12,5"));
    }

    @Test
    void rechazaTextoNegativosYPorcentajesMayoresA100() {
        assertThrows(NegocioException.class, () -> ParametrosReglasBean.validar("exceso_carga.tolerancia_kg", "mucho"));
        assertThrows(NegocioException.class, () -> ParametrosReglasBean.validar("exceso_carga.tolerancia_kg", "-1"));
        assertThrows(NegocioException.class, () -> ParametrosReglasBean.validar("desvio_ruta.porcentaje_puntos", "150"));
        assertThrows(NegocioException.class, () -> ParametrosReglasBean.validar("desvio_ruta.tolerancia_relativa_pct", "101"));
    }

    @Test
    void elPlazoDeDescargoEsUnEnteroDeAlMenosUnDia() {
        assertThrows(NegocioException.class, () -> ParametrosReglasBean.validar("descargo.plazo_dias", "0"));
        assertThrows(NegocioException.class, () -> ParametrosReglasBean.validar("descargo.plazo_dias", "2.5"));
    }
}
