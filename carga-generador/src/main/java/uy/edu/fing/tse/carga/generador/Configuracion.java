package uy.edu.fing.tse.carga.generador;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public record Configuracion(
    String urlIngesta,
    String urlMetricas,
    String token,
    String escenario,
    int vehiculos,
    int intervaloSeg,
    int duracionSeg,
    double probDuplicado,
    double probSinCobertura,
    int sinCoberturaMaxSeg,
    double picoFraccion,
    int picoCorteSeg,
    int picoVentanaSeg,
    int rampaPaso,
    int rampaCadaSeg,
    int concurrencia,
    int loteMax,
    int reporteSeg,
    int drenajeSeg,
    String archivoCsv,
    long semilla) {
    public static Configuracion desdeEntorno(Map<String, String> env) {
        String escenario = texto(env, "GEN_ESCENARIO", "constante").toLowerCase(Locale.ROOT);
        if (!escenario.equals("constante") && !escenario.equals("pico") && !escenario.equals("rampa")) {
            throw new IllegalArgumentException("GEN_ESCENARIO tiene que ser constante, pico o rampa (llego: "
                + escenario + ")");
        }
        String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return new Configuracion(
            texto(env, "CENTRAL_INGESTA_URL", "http://central:8080/carga/api/tracking/posiciones"),
            texto(env, "CENTRAL_METRICAS_URL", "http://central:8080/carga/api/metricas"),
            texto(env, "NODO_TOKEN", ""),
            escenario,
            entero(env, "GEN_VEHICULOS", 3000),
            entero(env, "GEN_INTERVALO_SEG", 30),
            entero(env, "GEN_DURACION_SEG", 600),
            decimal(env, "GEN_PROB_DUPLICADO", 0.02),
            decimal(env, "GEN_PROB_SIN_COBERTURA", 0.01),
            entero(env, "GEN_SIN_COBERTURA_MAX_SEG", 600),
            decimal(env, "GEN_PICO_FRACCION", 0.5),
            entero(env, "GEN_PICO_CORTE_SEG", 300),
            entero(env, "GEN_PICO_VENTANA_SEG", 60),
            entero(env, "GEN_RAMPA_PASO", 500),
            entero(env, "GEN_RAMPA_CADA_SEG", 120),
            entero(env, "GEN_CONCURRENCIA", 200),
            entero(env, "GEN_LOTE_MAX", 500),
            entero(env, "GEN_REPORTE_SEG", 10),
            entero(env, "GEN_DRENAJE_SEG", 180),
            texto(env, "GEN_CSV", "/resultados/carga-" + escenario + "-" + marca + ".csv"),
            entero(env, "GEN_SEMILLA", 42));
    }

    public String urlLote() {
        return urlIngesta + "/lote";
    }

    public double eventosPorSegundoBase() {
        return (double) vehiculos / intervaloSeg;
    }

    private static String texto(Map<String, String> env, String clave, String porDefecto) {
        String v = env.get(clave);
        return v == null || v.isBlank() ? porDefecto : v.trim();
    }

    private static int entero(Map<String, String> env, String clave, int porDefecto) {
        String v = env.get(clave);
        try {
            return v == null || v.isBlank() ? porDefecto : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(clave + " tiene que ser un numero entero (llego: " + v + ")");
        }
    }

    private static double decimal(Map<String, String> env, String clave, double porDefecto) {
        String v = env.get(clave);
        try {
            return v == null || v.isBlank() ? porDefecto : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(clave + " tiene que ser un numero (llego: " + v + ")");
        }
    }
}
