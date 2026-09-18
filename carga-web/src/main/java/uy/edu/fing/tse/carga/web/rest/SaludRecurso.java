package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.Map;

/** Sirve para verificar que el despliegue quedo arriba (lo usa el healthcheck de Docker). */
@Path("/salud")
public class SaludRecurso {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> salud() {
        return Map.of("estado", "ok", "instante", OffsetDateTime.now().toString());
    }
}
