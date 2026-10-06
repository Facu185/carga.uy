package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;

import java.util.List;
import java.util.Map;

/**
 * Recurso REST de ejemplo. Sirve de plantilla para el resto de los recursos:
 * el recurso no tiene logica, solo traduce HTTP a llamadas al EJB de negocio.
 */
@Path("/empresas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmpresaRecurso {

    @EJB
    private EmpresaServicioLocal servicio;

    @GET
    public List<EmpresaDTO> listar(@QueryParam("nombre") String nombre) {
        return (nombre == null || nombre.isBlank()) ? servicio.listar() : servicio.buscarPorNombre(nombre);
    }

    @POST
    public Response agregar(EmpresaDTO empresa) {
        try {
            return Response.status(Response.Status.CREATED).entity(servicio.agregar(empresa)).build();
        } catch (NegocioException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }
}
