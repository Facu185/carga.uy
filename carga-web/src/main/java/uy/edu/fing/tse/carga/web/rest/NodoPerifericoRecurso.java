package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.fing.tse.carga.api.contract.service.NodosPerifericosLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.ActivacionNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.DatosNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.NodoPerifericoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/** Administracion de nodos perifericos (frente P4). Solo administradores: lo controla el EJB. */
@Path("/nodos")
@Produces(MediaType.APPLICATION_JSON)
public class NodoPerifericoRecurso {

    @EJB
    private NodosPerifericosLocal servicio;

    @GET
    public List<NodoPerifericoDTO> listar() {
        return servicio.listar();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(DatosNodoDTO datos) throws NegocioException {
        return Response.status(Response.Status.CREATED).entity(servicio.crear(datos)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public NodoPerifericoDTO modificar(@PathParam("id") Long id, DatosNodoDTO datos) throws NegocioException {
        return servicio.modificar(id, datos);
    }

    @PUT
    @Path("/{id}/activo")
    @Consumes(MediaType.APPLICATION_JSON)
    public NodoPerifericoDTO cambiarActivacion(@PathParam("id") Long id, ActivacionNodoDTO activacion)
            throws NegocioException {
        return servicio.cambiarActivacion(id, activacion);
    }
}
