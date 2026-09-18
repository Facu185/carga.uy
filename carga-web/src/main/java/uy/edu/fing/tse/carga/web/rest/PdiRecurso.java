package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.PdiLocal;
import uy.edu.fing.tse.carga.api.dto.PersonaDTO;

import java.util.Map;

/**
 * Expone la consulta a la PDI para poder probarla sin levantar todo el flujo de
 * autenticacion. El frente P1 la va a consumir desde el ingreso, no por REST.
 */
@Path("/pdi")
@Produces(MediaType.APPLICATION_JSON)
public class PdiRecurso {

    @EJB
    private PdiLocal pdi;

    @GET
    @Path("/personas/{cedula}")
    public Response persona(@PathParam("cedula") String cedula) {
        PersonaDTO persona = pdi.obtenerPersona(cedula);
        if (persona == null) {
            // Puede ser que la cedula no exista o que la PDI no haya respondido:
            // el frente P1 tiene que distinguir los dos casos (flujo alternativo 7a).
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sin datos para la cedula " + cedula)).build();
        }
        return Response.ok(persona).build();
    }
}
