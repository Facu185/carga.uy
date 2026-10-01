package uy.edu.fing.tse.carga.ejb.observabilidad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricasTest {

    private Metricas metricas;

    @BeforeEach
    void preparar() {
        metricas = new Metricas();
    }

    @Test
    void cuentaPorCombinacionDeEtiquetas() {
        metricas.contar("carga_ingresos_total", "resultado", "exitoso");
        metricas.contar("carga_ingresos_total", "resultado", "exitoso");
        metricas.contar("carga_ingresos_total", "resultado", "fallido");

        String salida = metricas.exportarPrometheus();

        assertTrue(salida.contains("# TYPE carga_ingresos_total counter\n"));
        assertTrue(salida.contains("carga_ingresos_total{resultado=\"exitoso\"} 2\n"));
        assertTrue(salida.contains("carga_ingresos_total{resultado=\"fallido\"} 1\n"));
    }

    @Test
    void elHistogramaAcumulaLosBucketsYSumaLasDuraciones() {
        metricas.observarDuracion("carga_pdi_duracion_ms", 3);
        metricas.observarDuracion("carga_pdi_duracion_ms", 40);
        metricas.observarDuracion("carga_pdi_duracion_ms", 20000);

        String salida = metricas.exportarPrometheus();

        assertTrue(salida.contains("carga_pdi_duracion_ms_bucket{le=\"5\"} 1\n"));
        assertTrue(salida.contains("carga_pdi_duracion_ms_bucket{le=\"50\"} 2\n"));
        assertTrue(salida.contains("carga_pdi_duracion_ms_bucket{le=\"10000\"} 2\n"));
        assertTrue(salida.contains("carga_pdi_duracion_ms_bucket{le=\"+Inf\"} 3\n"));
        assertTrue(salida.contains("carga_pdi_duracion_ms_sum 20043\n"));
        assertTrue(salida.contains("carga_pdi_duracion_ms_count 3\n"));
    }

    @Test
    void losMedidoresSeLeenAlExportarYUnoQueFallaNoRompeLosDemas() {
        int[] tamano = {4};
        metricas.registrarMedidor("carga_cola_tamano", () -> tamano[0]);
        metricas.registrarMedidor("carga_otro", () -> {
            throw new IllegalStateException("roto");
        });
        tamano[0] = 7;

        String salida = metricas.exportarPrometheus();

        assertTrue(salida.contains("carga_cola_tamano 7\n"));
        assertTrue(salida.contains("carga_otro NaN\n"));
    }

    @Test
    void escapaLosValoresYValidaNombres() {
        assertEquals("{ruta=\"a\\\"b\\\\c\"}", Metricas.formatearEtiquetas("ruta", "a\"b\\c"));
        assertThrows(IllegalArgumentException.class, () -> metricas.contar("con espacios"));
        assertThrows(IllegalArgumentException.class, () -> metricas.contar("carga_x", "solo-nombre"));
        metricas.contar("carga_x");
        assertThrows(IllegalArgumentException.class, () -> metricas.observarDuracion("carga_x", 1));
    }
}
