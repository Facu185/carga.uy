package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.dto.comun.PaginaAuditoriaDTO;

/** Solo lectura: no hay POST, PUT ni DELETE sobre la auditoria. */
@Path("/auditoria")
@Produces(MediaType.APPLICATION_JSON)
public class AuditoriaRecurso {

    @EJB
    private AuditoriaLocal auditoria;

    /** pagina empieza en 0; tamano por defecto 50, maximo 100. */
    @GET
    public PaginaAuditoriaDTO buscar(@QueryParam("usuario") String usuario,
                                     @QueryParam("entidad") String entidad,
                                     @QueryParam("entidadId") String entidadId,
                                     @QueryParam("pagina") Integer pagina,
                                     @QueryParam("tamano") Integer tamano) {
        return auditoria.buscar(usuario, entidad, entidadId, pagina, tamano);
    }
}
