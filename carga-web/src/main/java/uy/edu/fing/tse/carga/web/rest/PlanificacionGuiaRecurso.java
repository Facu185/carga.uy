package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.PlanificacionGuiaLocal;
import uy.edu.fing.tse.carga.api.dto.planificacion.*;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/** Solo adapta transporte; identidad, pertenencia y peso se resuelven en EJB. */
@Path("/planificacion-guias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlanificacionGuiaRecurso {
    @EJB private PlanificacionGuiaLocal servicio;
    @Context private HttpServletResponse respuesta;

    @GET
    @Path("/calles")
    public List<SugerenciaCalleDTO> sugerir(@QueryParam("q") String texto) throws NegocioException {
        sinCache(); return servicio.sugerir(texto);
    }

    @POST
    @Path("/direccion")
    public DireccionMapaDTO resolver(SugerenciaCalleDTO seleccion) throws NegocioException {
        sinCache(); return servicio.resolver(seleccion);
    }

    @POST
    @Path("/direccion/alternativas")
    public List<DireccionMapaDTO> resolverAlternativas(SugerenciaCalleDTO seleccion) throws NegocioException {
        sinCache(); return servicio.resolverAlternativas(seleccion);
    }

    @POST
    @Path("/ruta")
    public RutaPreviaDTO calcular(SolicitudRutaDTO solicitud) throws NegocioException {
        sinCache(); return servicio.calcular(solicitud);
    }

    private void sinCache() { respuesta.setHeader("Cache-Control", "no-store"); }
}
