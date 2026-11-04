package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.ReporteActividadLocal;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** Reporte agregado ADR-0023; la autorizacion AUTORIDAD se conserva en el EJB. */
@Path("/reportes/viajes-finalizados")
@Produces(MediaType.APPLICATION_JSON)
public class ViajesFinalizadosRecurso {
    @EJB
    private ReporteActividadLocal servicio;

    @Context
    private HttpServletResponse respuesta;

    @GET
    public ReporteActividadDTO consultar(@QueryParam("desde") String desde, @QueryParam("hasta") String hasta,
                                        @QueryParam("tipoCargaId") String tipoCargaId) throws NegocioException {
        respuesta.setHeader("Cache-Control", "no-store");
        return servicio.viajesFinalizados(desde, hasta, tipoCargaId);
    }
}
