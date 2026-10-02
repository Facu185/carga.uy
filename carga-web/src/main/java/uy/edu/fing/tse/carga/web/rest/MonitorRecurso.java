package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.MonitorLocal;
import uy.edu.fing.tse.carga.api.dto.EstadoNodoDTO;
import uy.edu.fing.tse.carga.api.dto.MetricasDTO;
import uy.edu.fing.tse.carga.api.dto.RecorridoDTO;

import java.util.List;

/** Datos del panel de operacion. El mapa los consume desde el navegador. */
@Path("/monitor")
@Produces(MediaType.APPLICATION_JSON)
public class MonitorRecurso {

    @EJB
    private MonitorLocal monitor;

    @GET
    @Path("/metricas")
    public MetricasDTO metricas() {
        return monitor.metricas();
    }

    @GET
    @Path("/nodos")
    public List<EstadoNodoDTO> nodos() {
        return monitor.estadoNodos();
    }

    @GET
    @Path("/mapa")
    public List<RecorridoDTO> mapa(@QueryParam("puntos") Integer puntos) {
        return monitor.mapaFlota(puntos == null ? 40 : puntos);
    }
}
