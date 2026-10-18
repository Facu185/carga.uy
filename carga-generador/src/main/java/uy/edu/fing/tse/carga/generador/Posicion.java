package uy.edu.fing.tse.carga.generador;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public record Posicion(String idEvento, String matricula, double latitud, double longitud, String instante,
                       boolean duplicada) {
    public String json() {
        return String.format(Locale.ROOT,
            "{\"idEvento\":\"%s\",\"matricula\":\"%s\",\"latitud\":%.6f,\"longitud\":%.6f,\"instante\":\"%s\"}",
            idEvento, matricula, latitud, longitud, instante);
    }

    public Posicion comoDuplicada() {
        return new Posicion(idEvento, matricula, latitud, longitud, instante, true);
    }

    public static String lote(List<Posicion> posiciones) {
        return posiciones.stream().map(Posicion::json).collect(Collectors.joining(",", "[", "]"));
    }
}
