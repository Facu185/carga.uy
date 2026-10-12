package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.SolicitudesEmpresaLocal;
import uy.edu.fing.tse.carga.api.dto.empresa.NuevaSolicitudEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.SolicitudAltaEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.VinculoEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.RechazoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import java.util.List;
import java.util.Map;

/** API para el ciudadano solicitante y para el funcionario que verifica empresas y responsables. */
@Path("/solicitudes-empresa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SolicitudEmpresaRecurso {
    @EJB private SolicitudesEmpresaLocal servicio;
    @POST public Response solicitarAlta(NuevaSolicitudEmpresaDTO datos) { try { return Response.status(Response.Status.CREATED).entity(servicio.solicitarAlta(datos)).build(); } catch (NegocioException e) { return error(e); } }
    @GET @Path("/mia") public Response miSolicitudAlta() { SolicitudAltaEmpresaDTO solicitud=servicio.miSolicitudAlta(); return solicitud==null?Response.noContent().build():Response.ok(solicitud).build(); }
    @POST @Path("/vinculos/{empresaId}") public Response solicitarVinculo(@PathParam("empresaId") Long empresaId) { try { return Response.status(Response.Status.CREATED).entity(servicio.solicitarVinculo(empresaId)).build(); } catch (NegocioException e) { return error(e); } }
    @GET @Path("/vinculos/mios") public List<VinculoEmpresaDTO> misVinculos() { return servicio.misVinculos(); }
    @GET @Path("/vinculos") public List<VinculoEmpresaDTO> listarVinculos(@QueryParam("estado") String estado) throws NegocioException { return servicio.listarVinculos(estado); }
    @GET public List<SolicitudAltaEmpresaDTO> listar(@QueryParam("estado") String estado) throws NegocioException { return servicio.listarSolicitudes(estado); }
    @POST @Path("/{id}/aprobar") public SolicitudAltaEmpresaDTO aprobar(@PathParam("id") Long id) throws NegocioException { return servicio.aprobarAlta(id); }
    @POST @Path("/{id}/rechazar") public SolicitudAltaEmpresaDTO rechazar(@PathParam("id") Long id, RechazoDTO datos) throws NegocioException { return servicio.rechazarAlta(id, datos); }
    @POST @Path("/vinculos/{id}/aprobar") public VinculoEmpresaDTO aprobarVinculo(@PathParam("id") Long id) throws NegocioException { return servicio.aprobarVinculo(id); }
    @POST @Path("/vinculos/{id}/transferir") public VinculoEmpresaDTO transferir(@PathParam("id") Long id, RechazoDTO datos) throws NegocioException { return servicio.transferirResponsabilidad(id, datos); }
    @POST @Path("/vinculos/{id}/rechazar") public VinculoEmpresaDTO rechazarVinculo(@PathParam("id") Long id, RechazoDTO datos) throws NegocioException { return servicio.rechazarVinculo(id, datos); }
    @DELETE @Path("/empresas/{empresaId}") public Response bajaEmpresa(@PathParam("empresaId") Long empresaId, RechazoDTO datos) { try { servicio.darDeBajaEmpresa(empresaId, datos); return Response.noContent().build(); } catch (NegocioException e) { return error(e); } }
    private static Response error(NegocioException e) { return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build(); }
}
