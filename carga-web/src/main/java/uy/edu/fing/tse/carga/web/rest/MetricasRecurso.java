package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import uy.edu.fing.tse.carga.api.MetricasLocal;

/**
 * Metricas del central en formato Prometheus (Seccion 4.4, punto 8). Sin autenticacion para que un
 * Prometheus las pueda leer; no contienen datos personales (solo conteos y duraciones).
 */
@Path("/metricas")
public class MetricasRecurso {

    @EJB
    private MetricasLocal metricas;

    @GET
    @Produces("text/plain; version=0.0.4; charset=utf-8")
    public String exportar() {
        return metricas.exportarPrometheus();
    }
}
