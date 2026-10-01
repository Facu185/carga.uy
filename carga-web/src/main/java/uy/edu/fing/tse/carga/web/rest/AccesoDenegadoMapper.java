package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Traduce el rechazo de @RolesAllowed: 401 si no hay sesion, 403 si hay sesion pero sin el rol. */
@Provider
public class AccesoDenegadoMapper implements ExceptionMapper<EJBAccessException> {

    @Context
    SecurityContext seguridad;

    @Override
    public Response toResponse(EJBAccessException e) {
        boolean autenticado = seguridad != null && seguridad.getUserPrincipal() != null;
        return autenticado
                ? Errores.respuesta(403, "No ten\u00e9s permisos para esta operaci\u00f3n")
                : Errores.respuesta(401, "Deb\u00e9s iniciar sesi\u00f3n");
    }
}
