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
import uy.edu.fing.tse.carga.api.contract.service.InfraccionesEmpresaLocal;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CasoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.NuevoDescargoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.PaginaCasosDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import static uy.edu.fing.tse.carga.web.rest.FiscalizacionRecurso.entero;
import static uy.edu.fing.tse.carga.web.rest.FiscalizacionRecurso.largo;

/**
 * Infracciones de la empresa para su responsable (P5, ADR-0022). El empresaId solo elige la empresa: el
 * servicio comprueba en cada pedido que quien consulta sea su responsable activo.
 * GET  /infracciones?empresaId=&pagina=&tamano=
 * GET  /infracciones/{casoId}?empresaId=
 * POST /infracciones/{casoId}/descargo?empresaId=     {"fundamento": "..."}
 */
@Path("/infracciones")
@Produces(MediaType.APPLICATION_JSON)
public class InfraccionesRecurso {

    @EJB
    private InfraccionesEmpresaLocal infracciones;

    @GET
    public PaginaCasosDTO listar(@QueryParam("empresaId") String empresaId, @QueryParam("pagina") String pagina,
                                 @QueryParam("tamano") String tamano) throws NegocioException {
        return infracciones.infracciones(largo(empresaId), entero(pagina), entero(tamano));
    }

    @GET
    @Path("/{casoId}")
    public CasoDTO detalle(@PathParam("casoId") String casoId, @QueryParam("empresaId") String empresaId)
            throws NegocioException {
        return infracciones.infraccion(largo(empresaId), largo(casoId));
    }

    @POST
    @Path("/{casoId}/descargo")
    @Consumes(MediaType.APPLICATION_JSON)
    public CasoDTO descargo(@PathParam("casoId") String casoId, @QueryParam("empresaId") String empresaId,
                            NuevoDescargoDTO descargo) throws NegocioException {
        return infracciones.presentarDescargo(largo(empresaId), largo(casoId),
                descargo == null ? null : descargo.getFundamento());
    }
}
