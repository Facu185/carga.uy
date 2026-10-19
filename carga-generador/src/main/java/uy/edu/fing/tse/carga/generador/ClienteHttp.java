package uy.edu.fing.tse.carga.generador;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;

public class ClienteHttp {
    private static final String HEADER_TOKEN = "X-Nodo-Token";

    private final Configuracion config;
    private final HttpClient http = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public ClienteHttp(Configuracion config) {
        this.config = config;
    }

    public CompletableFuture<Respuesta> enviar(List<Posicion> posiciones) {
        boolean lote = posiciones.size() > 1;
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(lote ? config.urlLote() : config.urlIngesta()))
            .header("Content-Type", "application/json")
            .header(HEADER_TOKEN, config.token())
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(lote ? Posicion.lote(posiciones) : posiciones.get(0).json()))
            .build();
        long inicio = System.nanoTime();
        return http.sendAsync(pedido, HttpResponse.BodyHandlers.ofString())
            .handle((respuesta, error) -> {
                long ms = (System.nanoTime() - inicio) / 1_000_000;
                if (error != null) {
                    return Respuesta.sinRespuesta(ms);
                }
                return Respuesta.desde(respuesta.statusCode(), respuesta.body(),
                    respuesta.headers().firstValue("Retry-After").orElse(null), ms);
            });
    }

    public OptionalDouble leerMetrica(String serie) {
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(config.urlMetricas()))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
            return valorDe(r.body(), serie);
        } catch (Exception e) {
            return OptionalDouble.empty();
        }
    }

    static OptionalDouble valorDe(String prometheus, String serie) {
        if (prometheus == null) {
            return OptionalDouble.empty();
        }
        for (String linea : prometheus.split("\n")) {
            if (linea.startsWith(serie + " ")) {
                try {
                    return OptionalDouble.of(Double.parseDouble(linea.substring(serie.length() + 1).trim()));
                } catch (NumberFormatException e) {
                    return OptionalDouble.empty();
                }
            }
        }
        return OptionalDouble.of(0);
    }
}
