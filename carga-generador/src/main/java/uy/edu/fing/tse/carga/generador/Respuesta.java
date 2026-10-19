package uy.edu.fing.tse.carga.generador;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Respuesta(Tipo tipo, int estado, int reintentarSeg, Set<Integer> rechazadas, long latenciaMs) {
    public enum Tipo { ACEPTADA, RECHAZADA, SATURADA, ERROR }

    private static final Pattern INDICE = Pattern.compile("\"indice\"\\s*:\\s*(\\d+)");

    static final int REINTENTO_POR_DEFECTO_SEG = 10;

    public static Respuesta desde(int estado, String cuerpo, String retryAfter, long latenciaMs) {
        if (estado == 202 || estado == 200) {
            return new Respuesta(Tipo.ACEPTADA, estado, 0, indicesRechazados(cuerpo), latenciaMs);
        }
        if (estado == 503) {
            return new Respuesta(Tipo.SATURADA, estado, segundos(retryAfter), Set.of(), latenciaMs);
        }
        if (estado >= 400 && estado < 500) {
            return new Respuesta(Tipo.RECHAZADA, estado, 0, Set.of(), latenciaMs);
        }
        return new Respuesta(Tipo.ERROR, estado, 0, Set.of(), latenciaMs);
    }

    public static Respuesta sinRespuesta(long latenciaMs) {
        return new Respuesta(Tipo.ERROR, 0, 0, Set.of(), latenciaMs);
    }

    static Set<Integer> indicesRechazados(String cuerpo) {
        if (cuerpo == null || !cuerpo.contains("\"rechazadas\"")) {
            return Set.of();
        }
        Set<Integer> indices = new HashSet<>();
        Matcher m = INDICE.matcher(cuerpo);
        while (m.find()) {
            indices.add(Integer.parseInt(m.group(1)));
        }
        return indices;
    }

    static int segundos(String retryAfter) {
        if (retryAfter == null || retryAfter.isBlank()) {
            return REINTENTO_POR_DEFECTO_SEG;
        }
        try {
            return Math.max(1, Integer.parseInt(retryAfter.trim()));
        } catch (NumberFormatException e) {
            return REINTENTO_POR_DEFECTO_SEG;
        }
    }
}
