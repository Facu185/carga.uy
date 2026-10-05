package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.GuiaServicioLocal;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.dto.CrearGuiaCommand;

import java.util.Map;

@Path("/guias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GuiaRecurso {
    @EJB private GuiaServicioLocal servicio;

    @POST
    public Response crear(CrearGuiaCommand comando) {
        try { return Response.status(Response.Status.CREATED).entity(servicio.crear(comando)).build(); }
        catch (NegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build(); }
    }
}
