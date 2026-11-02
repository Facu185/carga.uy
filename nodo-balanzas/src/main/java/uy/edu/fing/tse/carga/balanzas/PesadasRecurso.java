package uy.edu.fing.tse.carga.balanzas;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;
import uy.edu.fing.tse.carga.api.observabilidad.Traza;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Nodo periferico Balanzas.
 * Genera pesadas ficticias de forma determinista (misma matricula + misma fecha
 * devuelven siempre lo mismo), asi las pruebas son repetibles.
 *
 * Variables de entorno:
 *   BALANZAS_DEMORA_MS -> demora artificial, para probar timeouts en el central.
 *   BALANZAS_FALLA     -> "true" hace que el nodo responda con error 503.
 *
 * Continua la traza del central (header traceparent) y exporta su tramo a Jaeger si esta definido
 * OTEL_EXPORTER_OTLP_ENDPOINT: asi una misma traza va del celular al nodo periferico (letra 4.4, punto 9).
 */
@Path("/pesadas")
@Produces(MediaType.APPLICATION_JSON)
public class PesadasRecurso {

    private static final String[] PEAJES = {"Peaje Solis", "Peaje Pando", "Peaje Mercedes", "Peaje Paso de los Toros"};

    static {
        Traza.usarServicio("nodo-balanzas");
    }

    @GET
    public List<PesadaDTO> pesadas(@QueryParam("matricula") String matricula,
                                   @QueryParam("fecha") String fecha,
                                   @HeaderParam(Traza.HEADER) String traceparent) {
        try (Traza.Tramo tramo = Traza.iniciarDesde(traceparent, "Balanzas pesadas", Traza.Tipo.SERVIDOR)
                .atributo("matricula", matricula == null ? "" : matricula)) {
            return generar(matricula, fecha);
        }
    }

    private List<PesadaDTO> generar(String matricula, String fecha) {
        demorarSiCorresponde();
        if (Boolean.parseBoolean(System.getenv().getOrDefault("BALANZAS_FALLA", "false"))) {
            throw new jakarta.ws.rs.ServiceUnavailableException("Nodo Balanzas fuera de servicio (simulado).");
        }
        if (matricula == null || matricula.isBlank()) {
            return List.of();
        }
        LocalDate dia = (fecha == null || fecha.isBlank()) ? LocalDate.now() : LocalDate.parse(fecha);
        Random azar = new Random((matricula + dia).hashCode());

        List<PesadaDTO> resultado = new ArrayList<>();
        int cantidad = 1 + azar.nextInt(3);
        for (int i = 0; i < cantidad; i++) {
            PesadaDTO pesada = new PesadaDTO();
            pesada.setIdPesada(matricula + "-" + dia + "-" + i);
            pesada.setMatricula(matricula);
            pesada.setPeaje(PEAJES[azar.nextInt(PEAJES.length)]);
            pesada.setPesoKg(24000 + azar.nextInt(9000));   // algunas superan las 30 t
            pesada.setInstante(OffsetDateTime.of(dia, java.time.LocalTime.of(6 + azar.nextInt(12),
                    azar.nextInt(60)), ZoneOffset.ofHours(-3)).toString());
            resultado.add(pesada);
        }
        return resultado;
    }

    private void demorarSiCorresponde() {
        long ms = Long.parseLong(System.getenv().getOrDefault("BALANZAS_DEMORA_MS", "0"));
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
