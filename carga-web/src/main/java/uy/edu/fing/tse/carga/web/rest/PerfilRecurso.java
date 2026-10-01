package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.PerfilLocal;
import uy.edu.fing.tse.carga.api.dto.PerfilDTO;

@Path("/mi-perfil")
@Produces(MediaType.APPLICATION_JSON)
public class PerfilRecurso {

    @EJB
    private PerfilLocal servicio;

    @GET
    public PerfilDTO miPerfil() throws NegocioException {
        return servicio.miPerfil();
    }
}
