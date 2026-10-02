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
import uy.edu.fing.tse.carga.api.CuentasInternasLocal;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.dto.CambioRolDTO;
import uy.edu.fing.tse.carga.api.dto.CuentaInternaDTO;
import uy.edu.fing.tse.carga.api.dto.NuevaCuentaInternaDTO;
import uy.edu.fing.tse.carga.api.dto.PasswordTemporalDTO;

import java.util.List;

/** Cuentas del backoffice. El control de acceso lo hace el EJB (solo ADMINISTRADOR). */
@Path("/usuarios-internos")
@Produces(MediaType.APPLICATION_JSON)
public class CuentasInternasRecurso {

    @EJB
    private CuentasInternasLocal servicio;

    @GET
    public List<CuentaInternaDTO> listar() {
        return servicio.listar();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(NuevaCuentaInternaDTO datos) throws NegocioException {
        return Response.status(Response.Status.CREATED).entity(servicio.crear(datos)).build();
    }

    @PUT
    @Path("/{id}/activo")
    @Consumes(MediaType.APPLICATION_JSON)
    public CuentaInternaDTO cambiarEstado(@PathParam("id") Long id, Boolean activo) throws NegocioException {
        return servicio.cambiarEstado(id, Boolean.TRUE.equals(activo));
    }

    @PUT
    @Path("/{id}/rol")
    @Consumes(MediaType.APPLICATION_JSON)
    public CuentaInternaDTO cambiarRol(@PathParam("id") Long id, CambioRolDTO datos) throws NegocioException {
        return servicio.cambiarRol(id, datos);
    }

    @POST
    @Path("/{id}/password")
    public PasswordTemporalDTO resetearPassword(@PathParam("id") Long id) throws NegocioException {
        return servicio.resetearPassword(id);
    }

    @POST
    @Path("/{id}/desbloqueo")
    public CuentaInternaDTO desbloquear(@PathParam("id") Long id) throws NegocioException {
        return servicio.desbloquear(id);
    }
}
