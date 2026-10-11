package uy.edu.fing.tse.carga.web.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.SincronizacionPesajesLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoSincronizacionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.util.List;

/**
 * Pesajes obtenidos del nodo Balanzas (frente P4). Los errores de datos los traduce
 * NegocioExceptionMapper.
 *
 * EJB y no un recurso comun para que @RolesAllowed lo controle el contenedor, como en el resto
 * del central (rechazo -> AccesoDenegadoMapper -> 401/403). Los servicios que usa no llevan rol
 * porque tambien los llaman el panel de operacion y la agenda automatica (ADR-0007).
 */
@Stateless
@RolesAllowed({Roles.ADMINISTRADOR, Roles.FUNCIONARIO})
@Path("/pesajes")
@Produces(MediaType.APPLICATION_JSON)
public class PesajeRecurso {

    @EJB
    private SincronizacionPesajesLocal sincronizacion;

    /** Pesadas ya guardadas. */
    @GET
    public List<PesadaRegistradaDTO> registradas(@QueryParam("matricula") String matricula,
                                                 @QueryParam("fecha") String fecha) throws NegocioException {
        return sincronizacion.registradas(matricula, fecha);
    }

    /** Dispara el caso de uso a mano, sin esperar a la agenda. */
    @POST
    @Path("/sincronizar")
    public ResultadoSincronizacionDTO sincronizar(@QueryParam("matricula") String matricula,
                                                  @QueryParam("fecha") String fecha) throws NegocioException {
        return sincronizacion.sincronizar(matricula, fecha);
    }
}
