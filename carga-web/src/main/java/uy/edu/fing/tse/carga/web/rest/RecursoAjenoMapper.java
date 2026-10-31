package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uy.edu.fing.tse.carga.api.exception.RecursoAjenoException;

/** Recurso inexistente o de otra persona: 403 en los dos casos, para no revelar cual es. */
@Provider
public class RecursoAjenoMapper implements ExceptionMapper<RecursoAjenoException> {

    @Override
    public Response toResponse(RecursoAjenoException e) {
        return Errores.respuesta(403, e.getMessage());
    }
}
