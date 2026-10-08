package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
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

import java.util.List;

/** Pesajes obtenidos del nodo Balanzas (frente P4). Los errores de datos los traduce NegocioExceptionMapper. */
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
