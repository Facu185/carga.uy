package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.FuncionariosLocal;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.dto.CambioDependenciaDTO;
import uy.edu.fing.tse.carga.api.dto.FuncionarioDTO;
import uy.edu.fing.tse.carga.api.dto.NuevoFuncionarioDTO;

import java.util.List;

@Path("/funcionarios")
@Produces(MediaType.APPLICATION_JSON)
public class FuncionarioRecurso {

    @EJB
    private FuncionariosLocal servicio;

    @GET
    public List<FuncionarioDTO> listar() {
        return servicio.listar();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(NuevoFuncionarioDTO datos) throws NegocioException {
        return Response.status(Response.Status.CREATED).entity(servicio.crear(datos)).build();
    }

    @PUT
    @Path("/{id}/activo")
    @Consumes(MediaType.APPLICATION_JSON)
    public FuncionarioDTO cambiarEstado(@PathParam("id") Long id, Boolean activo) throws NegocioException {
        return servicio.cambiarEstado(id, Boolean.TRUE.equals(activo));
    }

    @PUT
    @Path("/{id}/dependencia")
    @Consumes(MediaType.APPLICATION_JSON)
    public FuncionarioDTO cambiarDependencia(@PathParam("id") Long id, CambioDependenciaDTO datos)
            throws NegocioException {
        return servicio.cambiarDependencia(id, datos);
    }
}
