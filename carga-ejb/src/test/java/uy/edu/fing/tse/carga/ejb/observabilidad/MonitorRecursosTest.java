package uy.edu.fing.tse.carga.ejb.observabilidad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitorRecursosTest {
    @Test
    void publicaLasMetricasDeLaJvm() {
        Metricas metricas = new Metricas();
        MonitorRecursos monitor = new MonitorRecursos();
        monitor.metricas = metricas;

        monitor.registrar();

        String texto = metricas.exportarPrometheus();
        assertTrue(texto.contains("carga_jvm_memoria_heap_maxima_bytes "));
        assertTrue(texto.contains("carga_jvm_gc_colecciones{recolector=\""));
        assertTrue(texto.contains("carga_jvm_gc_tiempo_ms{recolector=\""));
        assertTrue(texto.contains("carga_jvm_cpu_proceso "));
    }

    @Test
    void sinElPoolDeWildflyInformaMenosUno() {
        Metricas metricas = new Metricas();
        MonitorRecursos monitor = new MonitorRecursos();
        monitor.metricas = metricas;

        monitor.registrar();

        String texto = metricas.exportarPrometheus();
        assertTrue(texto.contains("carga_db_conexiones{estado=\"en_uso\"} -1"));
        assertTrue(texto.contains("carga_db_esperas -1"));
    }

    @Test
    void leeAtributosDeLaJvmPorJmx() {
        assertTrue(MonitorRecursos.atributo("java.lang:type=Threading", "ThreadCount") > 0);
        assertTrue(MonitorRecursos.atributo("java.lang:type=Threading", "threadcount") > 0);
        assertEquals(-1.0, MonitorRecursos.atributo("java.lang:type=Threading", "NoExiste"));
        assertEquals(-1.0, MonitorRecursos.atributo("esto no es un nombre", "X"));
    }

    @Test
    void convierteLosValoresANumero() {
        assertEquals(3.0, MonitorRecursos.numero(3));
        assertEquals(2.5, MonitorRecursos.numero("2.5"));
        assertEquals(-1.0, MonitorRecursos.numero("abc"));
        assertEquals(-1.0, MonitorRecursos.numero(null));
    }
}
