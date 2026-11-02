package uy.edu.fing.tse.carga.ejb.servicio;

import uy.edu.fing.tse.carga.api.dto.publico.CifraPublicaDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Control de divulgacion de las estadisticas publicas (ADR-0021, letra 4.2 punto 6).
 *
 * Regla de umbral: una cifra se publica si reune al menos 5 guias de al menos 3 empresas distintas
 * (o si es cero: un cero no identifica a nadie). Con 1 empresa la cifra es de esa empresa; con 2,
 * cada una despeja la de la otra restando la propia.
 *
 * Supresion secundaria: si se reserva una sola celda de una tabla, su valor sale restando del total
 * (que es la suma de cualquier otra tabla). Por eso el conjunto reservado se agranda con las celdas
 * publicadas mas chicas hasta que tenga al menos dos celdas y, en conjunto, cumpla el umbral.
 */
final class ProteccionEstadistica {

    static final int UMBRAL_GUIAS = 5;
    static final int UMBRAL_EMPRESAS = 3;

    /** Celda calculada en la base. Las empresas solo se usan para contar, nunca salen del servidor. */
    record Celda(String clave, String etiqueta, long guias, Set<Long> empresas, long pesoKg) {
    }

    private ProteccionEstadistica() {
    }

    static boolean publicable(long guias, Set<Long> empresas) {
        return guias == 0 || (guias >= UMBRAL_GUIAS && empresas.size() >= UMBRAL_EMPRESAS);
    }

    /**
     * Categorias (tipo de carga, rubro): las que no cumplen se juntan en una fila "Otros", que a su
     * vez solo se publica si cumple. Ordenadas de mayor a menor.
     */
    static List<CifraPublicaDTO> categorias(List<Celda> celdas, String etiquetaOtros) {
        List<Celda> reservadas = reservar(celdas);
        List<CifraPublicaDTO> resultado = new ArrayList<>();
        celdas.stream()
                .filter(c -> c.guias() > 0 && !reservadas.contains(c))
                .sorted(Comparator.comparingLong(Celda::guias).reversed().thenComparing(Celda::clave))
                .forEach(c -> resultado.add(publicada(c)));
        if (!reservadas.isEmpty()) {
            Celda otros = unir(reservadas);
            resultado.add(publicable(otros.guias(), otros.empresas())
                    ? new CifraPublicaDTO("OTROS", etiquetaOtros, false, otros.guias(), toneladas(otros.pesoKg()))
                    : new CifraPublicaDTO("OTROS", etiquetaOtros, true, null, null));
        }
        return resultado;
    }

    /** Serie temporal (meses): se conserva el orden y cada celda reservada sale marcada como tal. */
    static List<CifraPublicaDTO> serie(List<Celda> celdas) {
        List<Celda> reservadas = reservar(celdas);
        return celdas.stream()
                .map(c -> reservadas.contains(c)
                        ? new CifraPublicaDTO(c.clave(), c.etiqueta(), true, null, null)
                        : publicada(c))
                .toList();
    }

    /** Un total suelto: null si no cumple el umbral. */
    static Long total(long cantidad, Set<Long> empresas, int minimo) {
        return cantidad == 0 || (cantidad >= minimo && empresas.size() >= UMBRAL_EMPRESAS) ? cantidad : null;
    }

    /** Celdas que no se publican: las que no cumplen, mas las necesarias para que no se despejen. */
    private static List<Celda> reservar(List<Celda> celdas) {
        List<Celda> reservadas = new ArrayList<>(celdas.stream()
                .filter(c -> !publicable(c.guias(), c.empresas())).toList());
        if (reservadas.isEmpty()) {
            return reservadas;
        }
        List<Celda> candidatas = new ArrayList<>(celdas.stream()
                .filter(c -> c.guias() > 0 && publicable(c.guias(), c.empresas()))
                .sorted(Comparator.comparingLong(Celda::guias).thenComparing(Celda::clave))
                .toList());
        Celda grupo = unir(reservadas);
        while ((reservadas.size() < 2 || !publicable(grupo.guias(), grupo.empresas())) && !candidatas.isEmpty()) {
            reservadas.add(candidatas.remove(0));
            grupo = unir(reservadas);
        }
        return reservadas;
    }

    private static Celda unir(List<Celda> celdas) {
        Set<Long> empresas = new HashSet<>();
        long guias = 0;
        long peso = 0;
        for (Celda c : celdas) {
            empresas.addAll(c.empresas());
            guias += c.guias();
            peso += c.pesoKg();
        }
        return new Celda("", "", guias, empresas, peso);
    }

    private static CifraPublicaDTO publicada(Celda c) {
        return new CifraPublicaDTO(c.clave(), c.etiqueta(), false, c.guias(), toneladas(c.pesoKg()));
    }

    private static double toneladas(long pesoKg) {
        return Math.round(pesoKg / 100.0) / 10.0;
    }
}
