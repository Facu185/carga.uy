package uy.edu.fing.tse.carga.ejb.modelo;

import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reglas del caso de uso "Registrar evento de viaje". Sin acceso a la base, para probarlas sueltas.
 *
 * Ciclo de vida de la guia segun los eventos:
 *   ASIGNADA --INICIO--> EN_CURSO --CARGA_PARCIAL | DESCARGA_PARCIAL | INCIDENTE--> EN_CURSO
 *   EN_CURSO --FIN--> FINALIZADA
 * Cualquier otro evento se rechaza: el central es la fuente de verdad (ADR-0019).
 */
public final class ReglasEventoViaje {

    public static final String ASIGNADA = "ASIGNADA";
    public static final String EN_CURSO = "EN_CURSO";
    public static final String FINALIZADA = "FINALIZADA";

    public static final int DETALLE_MAX = 1000;
    /** El reloj del telefono puede ir algo adelantado; mas que esto ya no es un desfase razonable. */
    static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    private static final Pattern ID_EVENTO = Pattern.compile("[A-Za-z0-9._-]{8,80}");
    private static final byte[] INICIO_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private ReglasEventoViaje() {
    }

    /** Problema de forma del evento (vacio si esta bien formado). No valida la foto: ver decodificarFoto. */
    public static Optional<String> problemaDeFormato(EventoViajeDTO e, Instant ahora) {
        if (e == null) return Optional.of("Faltan los datos del evento.");
        if (e.getIdEvento() == null || !ID_EVENTO.matcher(e.getIdEvento()).matches())
            return Optional.of("El idEvento es obligatorio: de 8 a 80 caracteres entre letras, n\u00fameros, '.', '_' y '-'.");
        if (e.getGuiaId() == null) return Optional.of("La gu\u00eda es obligatoria.");
        if (tipo(e.getTipo()).isEmpty())
            return Optional.of("El tipo de evento tiene que ser INICIO, FIN, CARGA_PARCIAL, DESCARGA_PARCIAL o INCIDENTE.");
        Optional<Instant> instante = instante(e.getInstante());
        if (instante.isEmpty())
            return Optional.of("El instante es obligatorio, en ISO-8601 con zona (por ejemplo 2026-10-20T08:00:00-03:00).");
        if (instante.get().isAfter(ahora.plus(TOLERANCIA_FUTURO)))
            return Optional.of("El instante del evento no puede estar en el futuro.");
        if (e.getDetalle() != null && e.getDetalle().length() > DETALLE_MAX)
            return Optional.of("El detalle no puede superar los " + DETALLE_MAX + " caracteres.");
        if (tipo(e.getTipo()).get() == TipoEventoViaje.INCIDENTE && (e.getDetalle() == null || e.getDetalle().isBlank()))
            return Optional.of("Un incidente necesita un detalle que lo describa.");
        if ((e.getLatitud() == null) != (e.getLongitud() == null))
            return Optional.of("La ubicaci\u00f3n necesita latitud y longitud juntas.");
        if (e.getLatitud() != null && (e.getLatitud() < -90 || e.getLatitud() > 90))
            return Optional.of("La latitud tiene que estar entre -90 y 90.");
        if (e.getLongitud() != null && (e.getLongitud() < -180 || e.getLongitud() > 180))
            return Optional.of("La longitud tiene que estar entre -180 y 180.");
        return Optional.empty();
    }

    public static Optional<TipoEventoViaje> tipo(String valor) {
        if (valor == null) return Optional.empty();
        try {
            return Optional.of(TipoEventoViaje.valueOf(valor.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<Instant> instante(String valor) {
        if (valor == null || valor.isBlank()) return Optional.empty();
        try {
            return Optional.of(OffsetDateTime.parse(valor.trim()).toInstant());
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /** Por que el viaje no admite el evento (vacio si lo admite). */
    public static Optional<String> motivoDeRechazo(String estadoGuia, TipoEventoViaje tipo) {
        if (FINALIZADA.equals(estadoGuia)) return Optional.of("El viaje ya finaliz\u00f3: no admite m\u00e1s eventos.");
        return switch (tipo) {
            case INICIO -> ASIGNADA.equals(estadoGuia) ? Optional.empty()
                    : Optional.of("El viaje ya fue iniciado.");
            case FIN, CARGA_PARCIAL, DESCARGA_PARCIAL, INCIDENTE -> EN_CURSO.equals(estadoGuia) ? Optional.empty()
                    : Optional.of("Primero hay que registrar el inicio del viaje.");
        };
    }

    /** Estado de la guia despues del evento. Solo para eventos admitidos (ver motivoDeRechazo). */
    public static String estadoTrasEvento(String estadoGuia, TipoEventoViaje tipo) {
        Optional<String> rechazo = motivoDeRechazo(estadoGuia, tipo);
        if (rechazo.isPresent()) throw new IllegalStateException(rechazo.get());
        return switch (tipo) {
            case INICIO, CARGA_PARCIAL, DESCARGA_PARCIAL, INCIDENTE -> EN_CURSO;
            case FIN -> FINALIZADA;
        };
    }

    /**
     * La foto del evento: null si no vino. Exige JPEG y un tope de tamano (se controla antes de
     * decodificar, para no reservar memoria por un cuerpo enorme).
     *
     * @throws IllegalArgumentException con un mensaje para mostrar
     */
    public static byte[] decodificarFoto(String base64, int maxBytes) {
        if (base64 == null || base64.isBlank()) return null;
        long maxBase64 = ((long) maxBytes + 2) / 3 * 4;
        if (base64.length() > maxBase64)
            throw new IllegalArgumentException("La foto supera el tama\u00f1o m\u00e1ximo de " + (maxBytes / 1000) + " KB.");
        byte[] foto;
        try {
            foto = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La foto no est\u00e1 en base64 v\u00e1lido.");
        }
        if (foto.length > maxBytes)
            throw new IllegalArgumentException("La foto supera el tama\u00f1o m\u00e1ximo de " + (maxBytes / 1000) + " KB.");
        if (foto.length < INICIO_JPEG.length || foto[0] != INICIO_JPEG[0] || foto[1] != INICIO_JPEG[1] || foto[2] != INICIO_JPEG[2])
            throw new IllegalArgumentException("La foto tiene que ser JPEG.");
        return foto;
    }
}
