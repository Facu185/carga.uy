package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.EventoViajeLocal;
import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;
import uy.edu.fing.tse.carga.api.dto.viaje.ResultadoEventoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;
import java.util.Optional;

/**
 * Eventos de viaje del componente movil (P3). Las reglas y la autorizacion estan en EventoViajeLocal.
 *
 * POST /eventos-viaje          201 si el evento es nuevo, 200 si ese idEvento ya estaba registrado
 * GET  /eventos-viaje?guiaId=  historial de una guia del chofer, sin las fotos
 * GET  /eventos-viaje/{idEvento}/foto  JPEG de la foto del evento; 404 si no tiene, 403 si no es suyo
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

    /**
     * JSON tambien en @Produces: un cliente que pide JSON recibe el error del mapper en vez de un 406.
     * La foto de un evento no cambia (reenviarla con otros bytes es conflicto), asi que se puede
     * guardar en cache, solo en el dispositivo: es de un chofer autenticado.
     */
    @GET
    @Path("/{idEvento}/foto")
    @Produces({"image/jpeg", MediaType.APPLICATION_JSON})
    public Response foto(@PathParam("idEvento") String idEvento) throws NegocioException {
        Optional<byte[]> foto = servicio.fotoDeEvento(idEvento);
        if (foto.isEmpty()) {
            return Errores.respuesta(404, "El evento no tiene foto.");
        }
        return Response.ok(foto.get(), "image/jpeg")
                .header("Cache-Control", "private, max-age=86400")
                .build();
    }
}
