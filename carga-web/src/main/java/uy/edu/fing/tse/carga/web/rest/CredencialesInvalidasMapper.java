package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uy.edu.fing.tse.carga.api.exception.CredencialesInvalidasException;

/** Usuario o clave incorrectos (mensaje generico a proposito). */
@Provider
public class CredencialesInvalidasMapper implements ExceptionMapper<CredencialesInvalidasException> {

    @Override
    public Response toResponse(CredencialesInvalidasException e) {
        return Errores.respuesta(401, e.getMessage());
    }
}
