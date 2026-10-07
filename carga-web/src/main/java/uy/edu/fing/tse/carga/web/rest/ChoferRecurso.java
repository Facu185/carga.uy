package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import uy.edu.fing.tse.carga.api.contract.service.ChoferServicioLocal;
import uy.edu.fing.tse.carga.api.dto.guia.ChoferDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import java.util.Map;

@Path("/choferes") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class ChoferRecurso {
    @EJB private ChoferServicioLocal servicio;
    @GET public Response listar(@QueryParam("empresaId") Long empresaId) { return Response.ok(servicio.listar(empresaId)).build(); }
    @POST public Response agregar(ChoferDTO dto) { try { return Response.status(Response.Status.CREATED).entity(servicio.agregar(dto)).build(); } catch (NegocioException e) { return error(e); } }
    @PUT @Path("/{id}") public Response modificar(@PathParam("id") Long id, ChoferDTO dto) { try { return Response.ok(servicio.modificar(id, dto)).build(); } catch (NegocioException e) { return error(e); } }
    @DELETE @Path("/{id}") public Response baja(@PathParam("id") Long id) { try { servicio.darDeBaja(id); return Response.noContent().build(); } catch (NegocioException e) { return error(e); } }
    @PUT @Path("/{id}/activo") public Response cambiarEstado(@PathParam("id") Long id, Boolean activo) { try { if (Boolean.TRUE.equals(activo)) servicio.darDeAlta(id); else servicio.darDeBaja(id); return Response.noContent().build(); } catch (NegocioException e) { return error(e); } }
    private Response error(NegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build(); }
}
