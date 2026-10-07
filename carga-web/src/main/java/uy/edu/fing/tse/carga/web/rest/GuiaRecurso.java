package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.GuiaServicioLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.guia.CrearGuiaCommand;

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

    /** Solo el principal CHOFER puede recuperar sus propias guias; la cedula nunca viene del cliente. */
    @GET
    @Path("/mis-guias")
    public Response misGuias() {
        try { return Response.ok(servicio.guiasDelChoferAutenticado()).build(); }
        catch (NegocioException e) { return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", e.getMessage())).build(); }
    }

    @GET
    @Path("/catalogos/vehiculos")
    public Response vehiculos() { try { return Response.ok(servicio.vehiculosDeLaEmpresaAutenticada()).build(); } catch (NegocioException e) { return prohibido(e); } }

    @GET
    @Path("/catalogos/choferes")
    public Response choferes() { try { return Response.ok(servicio.choferesDeLaEmpresaAutenticada()).build(); } catch (NegocioException e) { return prohibido(e); } }

    @GET
    @Path("/catalogos/rubros")
    public Response rubros() { return Response.ok(servicio.rubros()).build(); }

    @GET
    @Path("/catalogos/tipos-carga")
    public Response tiposCarga() { return Response.ok(servicio.tiposCarga()).build(); }

    @GET
    @Path("/mis-guias/descarga")
    @Produces("text/csv; charset=utf-8")
    public Response descargarMisGuias() {
        try {
            StringBuilder csv = new StringBuilder("id,origen,destino,inicio,fin,estado,vehiculoId\n");
            servicio.guiasDelChoferAutenticado().forEach(g -> csv.append(g.getId()).append(',').append(csv(g.getOrigen())).append(',').append(csv(g.getDestino())).append(',').append(g.getInicio()).append(',').append(g.getFin()).append(',').append(csv(g.getEstado())).append(',').append(g.getVehiculoId()).append('\n'));
            return Response.ok(csv.toString()).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mis-guias.csv").build();
        } catch (NegocioException e) { return prohibido(e); }
    }

    private static Response prohibido(NegocioException e) { return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", e.getMessage())).build(); }
    private static String csv(String valor) { return '"' + (valor == null ? "" : valor.replace("\"", "\"\"")) + '"'; }
}
