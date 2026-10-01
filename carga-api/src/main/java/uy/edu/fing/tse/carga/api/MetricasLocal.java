package uy.edu.fing.tse.carga.api;

import jakarta.ejb.Local;

import java.util.function.Supplier;

/**
 * Metricas de funcionamiento del central (Seccion 4.4, punto 8 de la letra). Se exponen en
 * formato Prometheus en GET /carga/api/metricas.
 *
 * Uso desde cualquier servicio:
 * <pre>
 *   &#64;EJB MetricasLocal metricas;
 *   metricas.contar("carga_casos_creados_total", "regla", "exceso_peso");
 *   metricas.observarDuracion("carga_pesaje_duracion_ms", milisegundos, "nodo", "balanzas");
 *   metricas.registrarMedidor("carga_cola_posiciones_tamano", () -> tamanoDeLaCola());
 * </pre>
 * Las etiquetas van de a pares (nombre, valor). Ojo con los valores: cada combinacion distinta es
 * una serie nueva, asi que nunca usar ids, cedulas ni matriculas como valor de etiqueta.
 */
@Local
public interface MetricasLocal {

    /** Suma 1 a un contador. */
    void contar(String metrica, String... etiquetas);

    /** Registra una duracion en un histograma (milisegundos). */
    void observarDuracion(String metrica, long milisegundos, String... etiquetas);

    /** Un valor que se lee en el momento de exportar (tamano de una cola, estado de un circuito). */
    void registrarMedidor(String metrica, Supplier<Number> fuente, String... etiquetas);

    /** Todas las metricas en el formato de texto de Prometheus. */
    String exportarPrometheus();
}
