package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.PesajeLocal;
import uy.edu.fing.tse.carga.api.dto.PesadaDTO;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Cliente del nodo periferico Balanzas: integracion sincronica request-response
 * (Secciones 3.7.2 y 4.1 de la letra).
 *
 * TODO (frente P4): agregar politica de reintentos y mecanismo de corte (circuit breaker),
 * exigidos por la Seccion 4.3 de la letra. Hoy solo hay timeout y degradacion.
 */
@Stateless
public class PesajeBean implements PesajeLocal {

    private static final Logger LOG = Logger.getLogger(PesajeBean.class.getName());

    private static final String URL_BASE = System.getenv()
            .getOrDefault("NODO_BALANZAS_URL", "http://balanzas:8080/balanzas/api/pesadas");
    private static final int TIMEOUT_SEGUNDOS = 3;

    @Override
    public List<PesadaDTO> obtenerPesadas(String matricula, String fecha) {
        Client cliente = ClientBuilder.newBuilder()
                .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .build();
        try {
            return cliente.target(URL_BASE)
                    .queryParam("matricula", matricula)
                    .queryParam("fecha", fecha)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<List<PesadaDTO>>() { });
        } catch (RuntimeException e) {
            // Comportamiento degradado: el componente central sigue operativo (RNF 4.3).
            LOG.warning("El nodo Balanzas no respondio (" + e.getClass().getSimpleName() + "): "
                    + e.getMessage() + ". Se devuelve lista vacia.");
            return List.of();
        } finally {
            cliente.close();
        }
    }
}
