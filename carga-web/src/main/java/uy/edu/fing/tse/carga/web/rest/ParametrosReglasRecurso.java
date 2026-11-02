package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.ParametrosReglasLocal;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CambioParametroDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ParametroReglaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/**
 * Parametros de las reglas de fiscalizacion (P5, ADR-0022). Solo administradores: lo controla
 * ParametrosReglasBean.
 * GET /parametros-reglas
 * PUT /parametros-reglas/{clave}    {"valor": "600", "motivo": "..."}
 */
@Path("/parametros-reglas")
@Produces(MediaType.APPLICATION_JSON)
public class ParametrosReglasRecurso {

    @EJB
    private ParametrosReglasLocal parametros;

    @GET
    public List<ParametroReglaDTO> listar() {
        return parametros.listar();
    }

    @PUT
    @Path("/{clave}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ParametroReglaDTO modificar(@PathParam("clave") String clave, CambioParametroDTO cambio) throws NegocioException {
        if (cambio == null) throw new NegocioException("Falta el nuevo valor.");
        return parametros.modificar(clave, cambio.getValor(), cambio.getMotivo());
    }
}
