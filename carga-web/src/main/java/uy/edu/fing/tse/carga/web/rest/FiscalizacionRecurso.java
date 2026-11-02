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
import uy.edu.fing.tse.carga.api.contract.service.FiscalizacionLocal;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CasoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.PaginaCasosDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ReporteFiscalizacionDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ResolucionCasoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/**
 * Casos de fiscalizacion (P5, ADR-0022). Los roles los controla FiscalizacionBean: la autorizacion vive
 * en la capa de servicios (letra 4.2, punto 3), aca solo se traduce.
 * GET  /fiscalizacion/casos?estado=&tipo=&pagina=&tamano=   bandeja del funcionario
 * GET  /fiscalizacion/casos/{id}                            detalle con evidencia y descargo
 * POST /fiscalizacion/casos/{id}/resolucion                 {"confirmar": true, "fundamento": "..."}
 * GET  /fiscalizacion/reporte?desde=aaaa-mm-dd&hasta=       indicadores para la autoridad
 */
@Path("/fiscalizacion")
@Produces(MediaType.APPLICATION_JSON)
public class FiscalizacionRecurso {

    @EJB
    private FiscalizacionLocal fiscalizacion;

    @GET
    @Path("/casos")
    public PaginaCasosDTO bandeja(@QueryParam("estado") String estado, @QueryParam("tipo") String tipo,
                                  @QueryParam("pagina") String pagina, @QueryParam("tamano") String tamano)
            throws NegocioException {
        return fiscalizacion.bandeja(estado, tipo, entero(pagina), entero(tamano));
    }

    @GET
    @Path("/casos/{id}")
    public CasoDTO caso(@PathParam("id") String id) throws NegocioException {
        return fiscalizacion.caso(largo(id));
    }

    @POST
    @Path("/casos/{id}/resolucion")
    @Consumes(MediaType.APPLICATION_JSON)
    public CasoDTO resolver(@PathParam("id") String id, ResolucionCasoDTO resolucion) throws NegocioException {
        if (resolucion == null) throw new NegocioException("Falta la resolución.");
        return fiscalizacion.resolver(largo(id), resolucion.isConfirmar(), resolucion.getFundamento());
    }

    @GET
    @Path("/reporte")
    public ReporteFiscalizacionDTO reporte(@QueryParam("desde") String desde, @QueryParam("hasta") String hasta)
            throws NegocioException {
        return fiscalizacion.reporte(desde, hasta);
    }

    /** Sin conversion implicita: un numero mal escrito es un 400 con mensaje, no un 404. */
    static Integer entero(String valor) throws NegocioException {
        try {
            return valor == null || valor.isBlank() ? null : Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException("La página y el tamaño tienen que ser números enteros.");
        }
    }

    static Long largo(String valor) throws NegocioException {
        try {
            return valor == null || valor.isBlank() ? null : Long.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException("El identificador tiene que ser un número entero.");
        }
    }
}
