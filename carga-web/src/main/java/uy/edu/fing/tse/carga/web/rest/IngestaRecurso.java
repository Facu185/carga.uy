package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.IngestaTrackingLocal;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;

import java.util.Map;

/**
 * Punto de entrada del nodo Tracking.
 * Responde 202 (aceptado, todavia no procesado) apenas encola: el nodo no espera
 * a que la posicion se persista.
 */
@Path("/tracking")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IngestaRecurso {

    @EJB
    private IngestaTrackingLocal ingesta;

    @POST
    @Path("/posiciones")
    public Response recibir(PosicionDTO posicion) {
        if (posicion == null || posicion.getIdEvento() == null || posicion.getMatricula() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "idEvento y matricula son obligatorios")).build();
        }
        ingesta.encolarPosicion(posicion);
        return Response.accepted(Map.of("idEvento", posicion.getIdEvento())).build();
    }
}
