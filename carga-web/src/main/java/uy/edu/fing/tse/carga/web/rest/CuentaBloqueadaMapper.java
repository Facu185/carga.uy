package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uy.edu.fing.tse.carga.api.CuentaBloqueadaException;

/** 429 Too Many Requests: la cuenta esta bloqueada temporalmente por intentos fallidos. */
@Provider
public class CuentaBloqueadaMapper implements ExceptionMapper<CuentaBloqueadaException> {

    @Override
    public Response toResponse(CuentaBloqueadaException e) {
        return Errores.respuesta(429, e.getMessage());
    }
}
