package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uy.edu.fing.tse.carga.api.exception.IngestaSaturadaException;

import java.util.Map;


@Provider
public class IngestaSaturadaMapper implements ExceptionMapper<IngestaSaturadaException> {

    @Override
    public Response toResponse(IngestaSaturadaException e) {
        return Response.status(503)
            .header("Retry-After", e.getReintentarEnSegundos())
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of("error", e.getMessage()))
            .build();
    }
}
