package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** Errores de validacion o de reglas de negocio. */
@Provider
public class NegocioExceptionMapper implements ExceptionMapper<NegocioException> {

    @Override
    public Response toResponse(NegocioException e) {
        return Errores.respuesta(400, e.getMessage());
    }
}
