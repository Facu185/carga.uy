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

/** Transporte del reporte de autoridad; permisos y periodo se resuelven en el EJB. */
@Path("/reportes/actividad")
@Produces(MediaType.APPLICATION_JSON)
public class ReporteActividadRecurso {
    @EJB
    private ReporteActividadLocal servicio;

    @Context
    private HttpServletResponse respuesta;

    @GET
    public ReporteActividadDTO consultar(@QueryParam("desde") String desde, @QueryParam("hasta") String hasta)
            throws NegocioException {
        respuesta.setHeader("Cache-Control", "no-store");
        return servicio.consultar(desde, hasta);
    }
}
