package uy.edu.fing.tse.carga.generador;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObservadorMonitor {
    static final long RENOVAR_SESION_MS = 25 * 60_000L;

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEBE_CAMBIAR = Pattern.compile("\"debeCambiarPassword\"\\s*:\\s*true");

    private final Configuracion config;
    private final HttpClient http = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final Serie metricas = new Serie();
    private final Serie mapa = new Serie();
    private final LongAdder errores = new LongAdder();
    private long erroresInformados;

    private volatile String token;
    private volatile long sesionDesdeMs;
    private ScheduledExecutorService hilo;

    public ObservadorMonitor(Configuracion config) {
        this.config = config;
    }

    public void iniciar() throws Exception {
        entrar();
        hilo = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "funcionario-simulado");
            t.setDaemon(true);
            return t;
        });
        hilo.scheduleWithFixedDelay(this::consultar, 0, config.monitorCadaSeg(), TimeUnit.SECONDS);
    }

    public void detener() {
        if (hilo != null) {
            hilo.shutdownNow();
        }
    }

    void consultar() {
        try {
            if (token == null || System.currentTimeMillis() - sesionDesdeMs >= RENOVAR_SESION_MS) {
                entrar();
            }
            medir("/monitor/metricas", metricas);
            medir("/monitor/mapa", mapa);
        } catch (Exception e) {
            errores.increment();
        }
    }

    private void medir(String ruta, Serie serie) throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(config.urlApi() + ruta))
            .header("Authorization", "Bearer " + token)
            .timeout(Duration.ofSeconds(60))
            .GET()
            .build();
        long inicio = System.nanoTime();
        HttpResponse<Void> respuesta = http.send(pedido, HttpResponse.BodyHandlers.discarding());
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        if (respuesta.statusCode() == 200) {
            serie.registrar(ms);
        } else {
            if (respuesta.statusCode() == 401) {
                token = null;
            }
            errores.increment();
        }
    }

    private void entrar() throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(config.urlApi() + "/auth/login"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(cuerpoLogin(config.monitorUsuario(), config.monitorPassword())))
            .build();
        HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException("el login respondio HTTP " + respuesta.statusCode()
                + " (revisar usuario y contrasena)");
        }
        if (debeCambiarPassword(respuesta.body())) {
            throw new IllegalStateException("la cuenta " + config.monitorUsuario()
                + " tiene una clave temporal: cambiala primero desde el frontend");
        }
        String nuevo = tokenDe(respuesta.body());
        if (nuevo == null) {
            throw new IllegalStateException("el login no devolvio un token");
        }
        token = nuevo;
        sesionDesdeMs = System.currentTimeMillis();
    }

    public Intervalo tomarIntervalo() {
        long[] m = metricas.tomarIntervalo();
        long[] p = mapa.tomarIntervalo();
        long e = errores.sum();
        Intervalo i = new Intervalo(m.length, Estadisticas.percentil(m, 95), Estadisticas.percentil(p, 95),
            e - erroresInformados);
        erroresInformados = e;
        return i;
    }

    public Total total() {
        long[] m = metricas.total();
        long[] p = mapa.total();
        return new Total(m.length, Estadisticas.percentil(m, 50), Estadisticas.percentil(m, 95), maximo(m),
            Estadisticas.percentil(p, 50), Estadisticas.percentil(p, 95), maximo(p), errores.sum());
    }

    static String cuerpoLogin(String usuario, String password) {
        return "{\"usuario\":\"" + escapar(usuario) + "\",\"password\":\"" + escapar(password) + "\"}";
    }

    static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String tokenDe(String json) {
        if (json == null) {
            return null;
        }
        Matcher m = TOKEN.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static boolean debeCambiarPassword(String json) {
        return json != null && DEBE_CAMBIAR.matcher(json).find();
    }

    private static long maximo(long[] ordenados) {
        return ordenados.length == 0 ? 0 : ordenados[ordenados.length - 1];
    }

    static final class Serie {
        private long[] intervalo = new long[64];
        private int nIntervalo;
        private long[] total = new long[256];
        private int nTotal;

        synchronized void registrar(long ms) {
            if (nIntervalo == intervalo.length) {
                intervalo = Arrays.copyOf(intervalo, nIntervalo * 2);
            }
            intervalo[nIntervalo++] = ms;
            if (nTotal == total.length) {
                total = Arrays.copyOf(total, nTotal * 2);
            }
            total[nTotal++] = ms;
        }

        synchronized long[] tomarIntervalo() {
            long[] copia = Arrays.copyOf(intervalo, nIntervalo);
            nIntervalo = 0;
            Arrays.sort(copia);
            return copia;
        }

        synchronized long[] total() {
            long[] copia = Arrays.copyOf(total, nTotal);
            Arrays.sort(copia);
            return copia;
        }
    }

    public record Intervalo(int consultas, long metricasP95, long mapaP95, long errores) {
    }

    public record Total(int consultas, long metricasP50, long metricasP95, long metricasMax,
                        long mapaP50, long mapaP95, long mapaMax, long errores) {
    }
}
