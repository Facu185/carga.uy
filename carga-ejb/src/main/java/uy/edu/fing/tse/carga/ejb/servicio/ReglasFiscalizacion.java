package uy.edu.fing.tse.carga.ejb.servicio;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Las reglas de deteccion de la letra (Fiscalizacion Automatica, puntos 6 a 9), sin base ni contenedor.
 * Cada una devuelve la evidencia en texto si hay incumplimiento. Criterios justificados en el ADR-0022.
 */
final class ReglasFiscalizacion {

    /** Con menos posiciones que esto en el viaje, no hay recorrido suficiente para hablar de desvio. */
    static final int PUNTOS_MINIMOS_DESVIO = 5;

    private static final NumberFormat NUMERO = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-UY"));

    private ReglasFiscalizacion() {
    }

    /** Exceso de carga: el pesaje supera el peso del vehiculo mas su capacidad declarada, mas la tolerancia. */
    static Optional<String> exceso(int pesadoKg, int pesoVehiculoKg, int capacidadKg, int toleranciaKg) {
        int maximo = pesoVehiculoKg + capacidadKg;
        if (pesadoKg <= maximo + toleranciaKg) {
            return Optional.empty();
        }
        return Optional.of("Peso registrado " + kg(pesadoKg) + ". Máximo admitido: peso del vehículo " + kg(pesoVehiculoKg)
                + " + capacidad de carga declarada " + kg(capacidadKg) + " = " + kg(maximo)
                + " (tolerancia " + kg(toleranciaKg) + "). Exceso: " + kg(pesadoKg - maximo) + ".");
    }

    /** Datos del vehiculo que hacen falta para la regla de habilitacion. */
    record Habilitacion(boolean vehiculoHabilitado, boolean empresaHabilitada, String numeroPermiso,
                        LocalDate permisoDesde, LocalDate permisoHasta, LocalDate itvVencimiento) {
    }

    /**
     * Vehiculo sin habilitacion vigente el dia de la actividad: permiso nacional o ITV vencidos (o sin
     * datos), o el vehiculo o su empresa dados de baja.
     */
    static Optional<String> sinHabilitacion(Habilitacion h, LocalDate dia) {
        List<String> motivos = new ArrayList<>();
        if (!h.vehiculoHabilitado()) {
            motivos.add("el vehículo está dado de baja");
        }
        if (!h.empresaHabilitada()) {
            motivos.add("la empresa está dada de baja");
        }
        if (h.numeroPermiso() == null || h.numeroPermiso().isBlank() || h.permisoDesde() == null || h.permisoHasta() == null) {
            motivos.add("no tiene Permiso Nacional de Circulación registrado");
        } else if (dia.isBefore(h.permisoDesde()) || dia.isAfter(h.permisoHasta())) {
            motivos.add("el Permiso Nacional de Circulación " + h.numeroPermiso() + " no estaba vigente (válido del "
                    + h.permisoDesde() + " al " + h.permisoHasta() + ")");
        }
        if (h.itvVencimiento() == null) {
            motivos.add("no tiene Inspección Técnica Vehicular registrada");
        } else if (dia.isAfter(h.itvVencimiento())) {
            motivos.add("la Inspección Técnica Vehicular venció el " + h.itvVencimiento());
        }
        return motivos.isEmpty() ? Optional.empty()
                : Optional.of("Actividad registrada el " + dia + " cuando " + String.join("; ", motivos) + ".");
    }

    /** Ancho del corredor alrededor de la linea origen-destino: el mayor entre el absoluto y el relativo. */
    static double corredorMetros(double distanciaRectaM, double toleranciaM, double toleranciaRelativaPct) {
        return Math.max(toleranciaM, distanciaRectaM * toleranciaRelativaPct / 100.0);
    }

    /**
     * Desvio de ruta: mas del porcentaje tolerado de las posiciones del viaje quedan fuera del corredor.
     * Por porcentaje y no por un solo punto, para no confundir un error de GPS con un desvio.
     */
    static Optional<String> desvio(double distanciaRectaM, double corredorM, int posiciones, int fueraDelCorredor,
                                   double maximaDistanciaM, double porcentajeMinimo) {
        if (posiciones < PUNTOS_MINIMOS_DESVIO) {
            return Optional.empty();
        }
        double porcentaje = 100.0 * fueraDelCorredor / posiciones;
        if (porcentaje <= porcentajeMinimo) {
            return Optional.empty();
        }
        return Optional.of(String.format(Locale.forLanguageTag("es-UY"),
                "%d de %d posiciones del viaje (%.0f %%) quedaron a más de %s de la línea entre el origen y el destino"
                        + " declarados (distancia en línea recta %s). Alejamiento máximo: %s. Se admite hasta el %.0f %%.",
                fueraDelCorredor, posiciones, porcentaje, km(corredorM), km(distanciaRectaM), km(maximaDistanciaM),
                porcentajeMinimo));
    }

    private static String kg(int kilos) {
        return NUMERO.format(kilos) + " kg";
    }

    private static String km(double metros) {
        return String.format(Locale.forLanguageTag("es-UY"), "%.1f km", metros / 1000.0);
    }
}
