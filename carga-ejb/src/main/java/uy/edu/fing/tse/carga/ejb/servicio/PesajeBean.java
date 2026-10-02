package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.PesajeLocal;
import uy.edu.fing.tse.carga.api.dto.PesadaDTO;
import uy.edu.fing.tse.carga.api.dto.ResultadoPesajeDTO;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Cliente del nodo periferico Balanzas: integracion sincronica request-response
 * (Secciones 3.7.2 y 4.1 de la letra).
 *
 * TODO (frente P4): agregar politica de reintentos y mecanismo de corte (circuit breaker),
 * exigidos por la Seccion 4.3 de la letra. Hoy hay timeout y degradacion informada.
 */
@Stateless
public class PesajeBean implements PesajeLocal {

    private static final Logger LOG = Logger.getLogger(PesajeBean.class.getName());

    private static final String URL_BASE = System.getenv()
            .getOrDefault("NODO_BALANZAS_URL", "http://balanzas:8080/balanzas/api/pesadas");
    private static final int TIMEOUT_SEGUNDOS = 3;

    @Override
    public List<PesadaDTO> obtenerPesadas(String matricula, String fecha) {
        return consultarPesadas(matricula, fecha).getPesadas();
    }

    @Override
    public ResultadoPesajeDTO consultarPesadas(String matricula, String fecha) {
        Client cliente = ClientBuilder.newBuilder()
                .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .build();
        long inicio = System.currentTimeMillis();
        try {
            List<PesadaDTO> pesadas = cliente.target(URL_BASE)
                    .queryParam("matricula", matricula == null ? "" : matricula)
                    .queryParam("fecha", fecha == null ? "" : fecha)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<List<PesadaDTO>>() { });
            return ResultadoPesajeDTO.disponible(pesadas, System.currentTimeMillis() - inicio);
        } catch (RuntimeException e) {
            // Comportamiento degradado: el componente central sigue operativo (RNF 4.3).
            long demora = System.currentTimeMillis() - inicio;
            String motivo = "El nodo Balanzas no respondio (" + e.getClass().getSimpleName() + ").";
            LOG.warning(motivo + " Detalle: " + e.getMessage());
            return ResultadoPesajeDTO.noDisponible(motivo, demora);
        } finally {
            cliente.close();
        }
    }
}
