package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import uy.edu.fing.tse.carga.api.AutenticacionLocal;
import uy.edu.fing.tse.carga.api.CuentaLocal;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.Roles;
import uy.edu.fing.tse.carga.api.dto.CambioPasswordDTO;
import uy.edu.fing.tse.carga.api.dto.LoginDTO;
import uy.edu.fing.tse.carga.api.dto.SesionDTO;

import java.util.List;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AutenticacionRecurso {

    private static final List<String> ROLES = List.of(
            Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO);

    @EJB
    private AutenticacionLocal autenticacion;

    @EJB
    private CuentaLocal cuenta;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public SesionDTO login(LoginDTO credenciales) {
        return autenticacion.login(credenciales);
    }

    /** Devuelve una sesion nueva: cambiar la clave cierra las sesiones anteriores. */
    @POST
    @Path("/cambiar-password")
    @Consumes(MediaType.APPLICATION_JSON)
    public SesionDTO cambiarPassword(CambioPasswordDTO datos) throws NegocioException {
        return cuenta.cambiarMiPassword(datos);
    }

    /** "Seguir conectado": token nuevo para la misma persona, antes de que venza el actual. */
    @POST
    @Path("/renovar")
    public SesionDTO renovar() throws NegocioException {
        return cuenta.renovar();
    }

    /** Sirve para probar que el token funciona: devuelve quien sos segun el central. */
    @GET
    @Path("/yo")
    public Response yo(@Context SecurityContext seguridad) {
        if (seguridad.getUserPrincipal() == null) {
            return Errores.respuesta(401, "No autenticado");
        }
        List<String> roles = ROLES.stream().filter(seguridad::isUserInRole).toList();
        return Response.ok(Map.of("usuario", seguridad.getUserPrincipal().getName(), "roles", roles)).build();
    }
}
