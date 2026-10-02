package uy.edu.fing.tse.carga.api.observabilidad;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Manda los tramos terminados a un colector OpenTelemetry (Jaeger) por OTLP/HTTP con JSON.
 * Junta los tramos y los envia cada segundo en un hilo aparte: medir nunca demora un pedido,
 * y si Jaeger no esta, los tramos se descartan sin afectar al sistema.
 *
 * Se activa con OTEL_EXPORTER_OTLP_ENDPOINT (por ejemplo http://jaeger:4318); sin esa variable no exporta.
 */
final class ExportadorOtlp implements Consumer<Traza.Tramo> {

    private static final Logger LOG = Logger.getLogger(ExportadorOtlp.class.getName());
    private static final int MAXIMO_EN_COLA = 5_000;

    private final URI destino;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ConcurrentLinkedQueue<Traza.Tramo> cola = new ConcurrentLinkedQueue<>();
    private volatile boolean avisoDeFalla;

    private ExportadorOtlp(String endpoint) {
        this.destino = URI.create(endpoint.replaceAll("/+$", "") + "/v1/traces");
        ScheduledExecutorService envios = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread hilo = new Thread(r, "exportador-trazas");
            hilo.setDaemon(true);
            return hilo;
        });
        envios.scheduleWithFixedDelay(this::enviar, 1, 1, TimeUnit.SECONDS);
    }

    static Consumer<Traza.Tramo> desdeEntorno() {
        String endpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        return endpoint == null || endpoint.isBlank() ? tramo -> { } : new ExportadorOtlp(endpoint.trim());
    }

    @Override
    public void accept(Traza.Tramo tramo) {
        if (cola.size() < MAXIMO_EN_COLA) {
            cola.add(tramo);
        }
    }

    private void enviar() {
        List<Traza.Tramo> lote = new ArrayList<>();
        for (Traza.Tramo t = cola.poll(); t != null && lote.size() < 500; t = cola.poll()) {
            lote.add(t);
        }
        if (lote.isEmpty()) {
            return;
        }
        try {
            HttpRequest pedido = HttpRequest.newBuilder(destino)
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json(lote)))
                    .build();
            HttpResponse<Void> respuesta = http.send(pedido, HttpResponse.BodyHandlers.discarding());
            if (respuesta.statusCode() >= 300) {
                avisar("el colector respondio " + respuesta.statusCode());
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            avisar(e.toString());
        }
    }

    /** Solo el primer aviso: si Jaeger no esta, no se llena el log con lo mismo cada segundo. */
    private void avisar(String motivo) {
        if (!avisoDeFalla) {
            avisoDeFalla = true;
            LOG.warning("No se pudieron exportar trazas a " + destino + ": " + motivo);
        }
    }

    /** Cuerpo OTLP/JSON: un resourceSpans por servicio. */
    static String json(List<Traza.Tramo> tramos) {
        Map<String, List<Traza.Tramo>> porServicio = new LinkedHashMap<>();
        tramos.forEach(t -> porServicio.computeIfAbsent(t.servicioDelTramo, s -> new ArrayList<>()).add(t));
        StringBuilder j = new StringBuilder("{\"resourceSpans\":[");
        boolean primerServicio = true;
        for (Map.Entry<String, List<Traza.Tramo>> e : porServicio.entrySet()) {
            if (!primerServicio) {
                j.append(',');
            }
            primerServicio = false;
            j.append("{\"resource\":{\"attributes\":[");
            atributo(j, "service.name", e.getKey());
            j.append("]},\"scopeSpans\":[{\"scope\":{\"name\":\"carga.uy\"},\"spans\":[");
            boolean primerTramo = true;
            for (Traza.Tramo t : e.getValue()) {
                if (!primerTramo) {
                    j.append(',');
                }
                primerTramo = false;
                j.append("{\"traceId\":\"").append(t.traceId)
                        .append("\",\"spanId\":\"").append(t.spanId).append('"');
                if (t.padreId != null) {
                    j.append(",\"parentSpanId\":\"").append(t.padreId).append('"');
                }
                j.append(",\"name\":").append(texto(t.nombre))
                        .append(",\"kind\":").append(t.tipo.codigo)
                        .append(",\"startTimeUnixNano\":\"").append(t.inicioEpochNanos)
                        .append("\",\"endTimeUnixNano\":\"").append(t.finEpochNanos)
                        .append("\",\"attributes\":[");
                boolean primerAtributo = true;
                for (Map.Entry<String, String> a : t.atributos.entrySet()) {
                    if (!primerAtributo) {
                        j.append(',');
                    }
                    primerAtributo = false;
                    atributo(j, a.getKey(), a.getValue());
                }
                j.append("],\"status\":{\"code\":").append(t.conError ? 2 : 1).append("}}");
            }
            j.append("]}]}");
        }
        return j.append("]}").toString();
    }

    private static void atributo(StringBuilder j, String clave, String valor) {
        j.append("{\"key\":").append(texto(clave)).append(",\"value\":{\"stringValue\":")
                .append(texto(valor)).append("}}");
    }

    static String texto(String s) {
        StringBuilder r = new StringBuilder("\"");
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case '"' -> r.append("\\\"");
                case '\\' -> r.append("\\\\");
                case '\n' -> r.append("\\n");
                case '\r' -> r.append("\\r");
                case '\t' -> r.append("\\t");
                default -> {
                    if (c < 0x20) {
                        r.append(String.format("\\u%04x", (int) c));
                    } else {
                        r.append(c);
                    }
                }
            }
        }
        return r.append('"').toString();
    }
}
