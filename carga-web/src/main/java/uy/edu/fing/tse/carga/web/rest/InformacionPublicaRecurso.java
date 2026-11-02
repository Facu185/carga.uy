package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.InformacionPublicaLocal;
import uy.edu.fing.tse.carga.api.dto.publico.ValidacionEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.publico.ValidacionVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/**
 * Frontoffice publico (ADR-0021): sin sesion. Los errores de datos los traduce NegocioExceptionMapper.
 * GET /publico/permisos/vehiculo?matricula=STZ1234
 * GET /publico/permisos/empresa?numero=1001
 * GET /publico/estadisticas
 */
@Path("/publico")
@Produces(MediaType.APPLICATION_JSON)
public class InformacionPublicaRecurso {

    /** Las cifras cambian poco: un navegador o un proxy pueden reusarlas un minuto. */
    private static final int CACHE_ESTADISTICAS_SEG = 60;

    @EJB
    private InformacionPublicaLocal informacion;

    @GET
    @Path("/permisos/vehiculo")
    public ValidacionVehiculoDTO vehiculo(@QueryParam("matricula") String matricula) throws NegocioException {
        return informacion.validarVehiculo(matricula);
    }

    @GET
    @Path("/permisos/empresa")
    public ValidacionEmpresaDTO empresa(@QueryParam("numero") String numero) throws NegocioException {
        return informacion.validarEmpresa(numero);
    }

    @GET
    @Path("/estadisticas")
    public Response estadisticas() {
        CacheControl cache = new CacheControl();
        cache.setMaxAge(CACHE_ESTADISTICAS_SEG);
        return Response.ok(informacion.estadisticas()).cacheControl(cache).build();
    }
}
