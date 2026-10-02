package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uy.edu.fing.tse.carga.api.ServicioNoDisponibleException;

/** 503: un sistema externo (LDAP, gub.uy) no respondio; no es que las credenciales esten mal. */
@Provider
public class ServicioNoDisponibleMapper implements ExceptionMapper<ServicioNoDisponibleException> {

    @Override
    public Response toResponse(ServicioNoDisponibleException e) {
        return Errores.respuesta(503, e.getMessage());
    }
}
