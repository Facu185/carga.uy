package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.SolicitudesFuncionarioLocal;
import uy.edu.fing.tse.carga.api.dto.NuevaSolicitudDTO;
import uy.edu.fing.tse.carga.api.dto.RechazoDTO;
import uy.edu.fing.tse.carga.api.dto.SolicitudFuncionarioDTO;

import java.util.List;

@Path("/solicitudes-funcionario")
@Produces(MediaType.APPLICATION_JSON)
public class SolicitudFuncionarioRecurso {

    @EJB
    private SolicitudesFuncionarioLocal servicio;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response solicitar(NuevaSolicitudDTO datos) throws NegocioException {
        return Response.status(Response.Status.CREATED).entity(servicio.solicitar(datos)).build();
    }

    /** 204 si nunca pidio acceso. */
    @GET
    @Path("/mia")
    public Response miSolicitud() {
        SolicitudFuncionarioDTO mia = servicio.miSolicitud();
        return mia == null ? Response.noContent().build() : Response.ok(mia).build();
    }

    @GET
    public List<SolicitudFuncionarioDTO> listar(@QueryParam("estado") String estado) throws NegocioException {
        return servicio.listar(estado);
    }

    @POST
    @Path("/{id}/aprobar")
    public SolicitudFuncionarioDTO aprobar(@PathParam("id") Long id) throws NegocioException {
        return servicio.aprobar(id);
    }

    @POST
    @Path("/{id}/rechazar")
    @Consumes(MediaType.APPLICATION_JSON)
    public SolicitudFuncionarioDTO rechazar(@PathParam("id") Long id, RechazoDTO datos) throws NegocioException {
        return servicio.rechazar(id, datos);
    }
}
