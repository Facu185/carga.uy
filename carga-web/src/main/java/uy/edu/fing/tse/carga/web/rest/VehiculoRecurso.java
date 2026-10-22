package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import uy.edu.fing.tse.carga.api.contract.service.VehiculoServicioLocal;
import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import java.util.Map;

@Path("/vehiculos") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class VehiculoRecurso {
    @EJB private VehiculoServicioLocal servicio;
    @GET public Response listar(@QueryParam("empresaId") Long empresaId) { return Response.ok(servicio.listar(empresaId)).build(); }
    @POST public Response agregar(VehiculoDTO dto) { try { return Response.status(Response.Status.CREATED).entity(servicio.agregar(dto)).build(); } catch (NegocioException e) { return error(e); } }
    @PUT @Path("/{id}") public Response modificar(@PathParam("id") Long id, VehiculoDTO dto) { try { return Response.ok(servicio.modificar(id, dto)).build(); } catch (NegocioException e) { return error(e); } }
    @DELETE @Path("/{id}") public Response baja(@PathParam("id") Long id) { try { servicio.darDeBaja(id); return Response.noContent().build(); } catch (NegocioException e) { return error(e); } }
    @PUT @Path("/{id}/reactivar") public Response reactivar(@PathParam("id") Long id) { try { servicio.reactivar(id); return Response.noContent().build(); } catch (NegocioException e) { return error(e); } }
    private Response error(NegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build(); }
}
