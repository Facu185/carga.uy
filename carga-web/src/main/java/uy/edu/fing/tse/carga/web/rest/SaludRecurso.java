package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.SaludLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EstadoSaludDTO;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Health checks (Seccion 4.4, punto 8).
 * GET /salud        el despliegue esta arriba ("liveness"; no consulta nada, siempre responde rapido).
 * GET /salud/listo  puede atender pedidos ("readiness"): base de datos y sistemas externos.
 *                   200 si esta UP (aunque algo este DEGRADADO), 503 si esta DOWN.
 */
@Path("/salud")
@Produces(MediaType.APPLICATION_JSON)
public class SaludRecurso {

    @EJB
    private SaludLocal salud;

    @GET
    public Map<String, String> salud() {
        return Map.of("estado", "ok", "instante", OffsetDateTime.now().toString());
    }

    @GET
    @Path("/listo")
    public Response listo() {
        EstadoSaludDTO estado = salud.verificar();
        int codigo = "DOWN".equals(estado.getEstado()) ? 503 : 200;
        return Response.status(codigo).entity(estado).build();
    }
}
