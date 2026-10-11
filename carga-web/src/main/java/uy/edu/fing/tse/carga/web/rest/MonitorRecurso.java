package uy.edu.fing.tse.carga.web.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.MonitorLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.EstadoNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.MetricasDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoDTO;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.util.List;

/**
 * Datos del panel de operacion (frente P4).
 *
 * EJB y no un recurso comun para que @RolesAllowed lo controle el contenedor, como en el resto
 * del central (rechazo -> AccesoDenegadoMapper -> 401/403). Los servicios que usa no llevan rol
 * porque tambien los llaman el panel de operacion y la agenda automatica (ADR-0007).
 */
@Stateless
@RolesAllowed({Roles.ADMINISTRADOR, Roles.FUNCIONARIO})
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
