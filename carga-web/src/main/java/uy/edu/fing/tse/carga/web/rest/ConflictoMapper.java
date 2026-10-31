package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uy.edu.fing.tse.carga.api.exception.ConflictoException;

/** Choque con el estado actual de un recurso. JAX-RS elige este mapper antes que el de NegocioException. */
@Provider
public class ConflictoMapper implements ExceptionMapper<ConflictoException> {

    @Override
    public Response toResponse(ConflictoException e) {
        return Errores.respuesta(409, e.getMessage());
    }
}
