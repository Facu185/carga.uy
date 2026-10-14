package uy.edu.fing.tse.carga.ejb.modelo;

import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;


public final class ValidacionPosicion {

    public static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    static final int LARGO_ID_EVENTO = 80;
    static final int LARGO_MATRICULA = 20;
    private ValidacionPosicion() {
    }

    public static Optional<String> problema(PosicionDTO p, Instant ahora) {
        if (p == null) {
            return Optional.of("La posicion es obligatoria.");
        }
        if (vacio(p.getIdEvento())) {
            return Optional.of("idEvento es obligatorio.");
        }
        if (p.getIdEvento().trim().length() > LARGO_ID_EVENTO) {
            return Optional.of("idEvento no puede superar " + LARGO_ID_EVENTO + " caracteres.");
        }
        if (vacio(p.getMatricula())) {
            return Optional.of("matricula es obligatoria.");
        }
        if (p.getMatricula().trim().length() > LARGO_MATRICULA) {
            return Optional.of("matricula no puede superar " + LARGO_MATRICULA + " caracteres.");
        }
        if (!(p.getLatitud() >= -90 && p.getLatitud() <= 90)) {
            return Optional.of("latitud fuera de rango [-90, 90].");
        }
        if (!(p.getLongitud() >= -180 && p.getLongitud() <= 180)) {
            return Optional.of("longitud fuera de rango [-180, 180].");
        }
        if (vacio(p.getInstante())) {
            return Optional.of("instante es obligatorio.");
        }
        OffsetDateTime instante;
        try {
            instante = OffsetDateTime.parse(p.getInstante().trim());
        } catch (DateTimeParseException e) {
            return Optional.of("instante tiene que ser ISO-8601 con zona (ej. 2026-10-14T10:15:00-03:00).");
        }
        if (instante.toInstant().isAfter(ahora.plus(TOLERANCIA_FUTURO))) {
            return Optional.of("instante esta en el futuro.");
        }
        return Optional.empty();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
