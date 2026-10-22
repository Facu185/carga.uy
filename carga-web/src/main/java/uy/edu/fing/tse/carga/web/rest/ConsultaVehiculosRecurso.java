package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.ConsultaVehiculosLocal;
import uy.edu.fing.tse.carga.api.dto.vehiculo.PaginaConsultaVehiculosDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** Transporte C13 independiente del ABM; sintaxis invalida usa el mapper 400 existente. */
@Path("/vehiculos/consulta")
@Produces(MediaType.APPLICATION_JSON)
public class ConsultaVehiculosRecurso {
    @EJB
    private ConsultaVehiculosLocal servicio;

    @GET
    public PaginaConsultaVehiculosDTO consultar(@QueryParam("empresaId") String empresaId,
            @QueryParam("matricula") String matricula, @QueryParam("pagina") String pagina,
            @QueryParam("tamano") String tamano) throws NegocioException {
        Long empresa;
        Integer numero, cantidad;
        try {
            // Evita que la conversion implicita de QueryParam transforme sintaxis invalida en 404.
            empresa = empresaId == null ? null : Long.valueOf(empresaId);
            numero = pagina == null ? null : Integer.valueOf(pagina);
            cantidad = tamano == null ? null : Integer.valueOf(tamano);
        } catch (NumberFormatException e) {
            throw new NegocioException("La empresa, pagina y tamano deben ser numeros enteros representables.");
        }
        return servicio.consultar(empresa, matricula, numero, cantidad);
    }
}
