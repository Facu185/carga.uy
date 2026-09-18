package uy.edu.fing.tse.carga.tracking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Nodo periferico Tracking.
 *
 * Emite posiciones GPS simuladas de la flota contra el endpoint de ingesta del
 * componente central. Es un proceso autonomo: si el central esta caido, reintenta
 * y no pierde el ciclo (Seccion 4.3 de la letra).
 *
 * Variables de entorno:
 *   CENTRAL_INGESTA_URL   -> destino (por defecto http://central:8080/carga/api/tracking/posiciones)
 *   TRACKING_INTERVALO_MS -> cada cuanto emite (por defecto 5000)
 *   TRACKING_MATRICULAS   -> matriculas separadas por coma
 */
public final class NodoTracking {

    private static final String URL = System.getenv().getOrDefault(
            "CENTRAL_INGESTA_URL", "http://central:8080/carga/api/tracking/posiciones");
    private static final long INTERVALO_MS = Long.parseLong(
            System.getenv().getOrDefault("TRACKING_INTERVALO_MS", "5000"));
    private static final List<String> MATRICULAS = List.of(System.getenv()
            .getOrDefault("TRACKING_MATRICULAS", "STZ1234,STZ5678,CUY9012").split(","));

    // Recorrido de referencia: Montevideo -> Paso de los Toros.
    private static final double LAT_ORIGEN = -34.9011, LON_ORIGEN = -56.1645;
    private static final double LAT_DESTINO = -32.8161, LON_DESTINO = -56.5158;

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();
    private static final Random AZAR = new Random();

    private NodoTracking() {
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("[tracking] enviando a " + URL + " cada " + INTERVALO_MS + " ms");
        double avance = 0;
        while (true) {
            avance = (avance + 0.02) % 1.0;
            for (String matricula : MATRICULAS) {
                enviar(matricula, avance);
            }
            Thread.sleep(INTERVALO_MS);
        }
    }

    private static void enviar(String matricula, double avance) {
        // pequeno desvio aleatorio para que la ruta no sea una linea perfecta
        double lat = LAT_ORIGEN + (LAT_DESTINO - LAT_ORIGEN) * avance + (AZAR.nextDouble() - 0.5) * 0.01;
        double lon = LON_ORIGEN + (LON_DESTINO - LON_ORIGEN) * avance + (AZAR.nextDouble() - 0.5) * 0.01;

        String json = ("{"
                + "\"idEvento\":\"%s\","
                + "\"matricula\":\"%s\","
                + "\"latitud\":%f,"
                + "\"longitud\":%f,"
                + "\"instante\":\"%s\""
                + "}").formatted(UUID.randomUUID(), matricula.trim(), lat, lon, OffsetDateTime.now());

        HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> respuesta = CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
            System.out.printf("[tracking] %s -> HTTP %d%n", matricula.trim(), respuesta.statusCode());
        } catch (Exception e) {
            System.out.printf("[tracking] %s -> sin respuesta del central (%s)%n",
                    matricula.trim(), e.getMessage());
        }
    }
}
