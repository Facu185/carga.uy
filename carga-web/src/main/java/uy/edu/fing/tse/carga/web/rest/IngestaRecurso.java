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
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.Map;

@Path("/tracking")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IngestaRecurso {

    @EJB
    private IngestaTrackingLocal ingesta;

    @POST
    @Path("/posiciones")
    @NodoAutenticado
    public Response recibir(PosicionDTO posicion) throws NegocioException {
        ingesta.encolarPosicion(posicion);
        return Response.accepted(Map.of("idEvento", posicion.getIdEvento())).build();
    }
}
