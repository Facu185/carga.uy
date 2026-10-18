package uy.edu.fing.tse.carga.web.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.RecorridoLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;

/**
 * Recorrido de un vehiculo (frente P4). Las posiciones de la flota son sensibles: exige rol, como el
 * resto de la API de P4 (ADR-0007). El servicio no lleva rol porque P5 lo usa desde procesos sin usuario.
 */
@Stateless
@RolesAllowed({Roles.ADMINISTRADOR, Roles.FUNCIONARIO})
@Path("/recorridos")
@Produces(MediaType.APPLICATION_JSON)
public class RecorridoRecurso {

    @EJB
    private RecorridoLocal recorridos;

    /** GET /carga/api/recorridos?matricula=STZ1234&desde=2026-10-18T08:00:00-03:00&hasta=... */
    @GET
    public RecorridoVehiculoDTO recorrido(@QueryParam("matricula") String matricula,
                                          @QueryParam("desde") String desde,
                                          @QueryParam("hasta") String hasta) throws NegocioException {
        return recorridos.recorrido(matricula, desde, hasta);
    }

    @GET
    @Path("/guia/{id}")
    public RecorridoVehiculoDTO recorridoDeGuia(@PathParam("id") Long guiaId) throws NegocioException {
        return recorridos.recorridoDeGuia(guiaId);
    }
}
