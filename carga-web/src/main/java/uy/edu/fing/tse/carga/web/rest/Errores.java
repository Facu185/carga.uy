package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/** Mismo formato de error que el resto de la API: {"error": "mensaje"}. */
final class Errores {

    private Errores() {
    }

    static Response respuesta(int status, String mensaje) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(Map.of("error", mensaje)).build();
    }
}
