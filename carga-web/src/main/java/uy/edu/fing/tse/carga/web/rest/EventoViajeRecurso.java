package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.EventoViajeLocal;
import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;
import uy.edu.fing.tse.carga.api.dto.viaje.ResultadoEventoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/**
 * Eventos de viaje del componente movil (P3). Las reglas y la autorizacion estan en EventoViajeLocal.
 *
 * POST /eventos-viaje          201 si el evento es nuevo, 200 si ese idEvento ya estaba registrado
 * GET  /eventos-viaje?guiaId=  historial de una guia del chofer, sin las fotos
 */
@Path("/eventos-viaje")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventoViajeRecurso {

    @EJB
    private EventoViajeLocal servicio;

    @POST
    public Response registrar(EventoViajeDTO evento) throws NegocioException {
        ResultadoEventoDTO resultado = servicio.registrar(evento);
        return Response.status(resultado.isNuevo() ? Response.Status.CREATED : Response.Status.OK)
                .entity(resultado).build();
    }

    @GET
    public List<EventoViajeDTO> deGuia(@QueryParam("guiaId") Long guiaId) throws NegocioException {
        return servicio.eventosDeGuia(guiaId);
    }
}
