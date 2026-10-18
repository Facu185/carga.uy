package uy.edu.fing.tse.carga.generador;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Vehiculo {
    static final long ESPERA_ERROR_MS = 5_000;

    private static final double DURACION_VIAJE_SEG = 4 * 3600;

    private static final double RUIDO = 0.003;

    private static final double[] MONTEVIDEO = {-34.9011, -56.1645};

    private static final double[][] DESTINOS = {
        {-32.8161, -56.5158}, {-30.9053, -55.5508}, {-31.3833, -57.9667},
        {-34.4626, -57.8400}, {-34.4833, -54.3333}, {-33.1325, -58.2956}};

    private final String matricula;
    private final double[] origen;
    private final double[] destino;
    private final long intervaloMs;
    private final Deque<Posicion> buffer = new ArrayDeque<>();

    private double avance;
    private boolean vuelta;
    private long proximoReporteMs;
    private long sinCoberturaHastaMs = Long.MIN_VALUE;
    private long esperarHastaMs = Long.MIN_VALUE;
    private boolean enVuelo;

    public Vehiculo(int numero, int intervaloSeg, long inicioMs, Random azar) {
        this.matricula = String.format("GEN%05d", numero);
        this.origen = MONTEVIDEO;
        this.destino = DESTINOS[azar.nextInt(DESTINOS.length)];
        this.intervaloMs = intervaloSeg * 1000L;
        this.avance = azar.nextDouble();
        this.proximoReporteMs = inicioMs + (long) (azar.nextDouble() * intervaloMs);
    }

    public String matricula() {
        return matricula;
    }

    public synchronized boolean quizasReportar(long ahoraMs, Configuracion c, Random azar) {
        if (ahoraMs < proximoReporteMs) {
            return false;
        }
        proximoReporteMs += intervaloMs;
        buffer.addLast(nuevaPosicion(ahoraMs, azar));
        if (enCobertura(ahoraMs) && azar.nextDouble() < c.probSinCobertura()) {
            long duracion = 1000L + (long) (azar.nextDouble() * c.sinCoberturaMaxSeg() * 1000L);
            sinCoberturaHastaMs = ahoraMs + duracion;
        }
        return true;
    }

    public synchronized void cortarCobertura(long hastaMs) {
        sinCoberturaHastaMs = Math.max(sinCoberturaHastaMs, hastaMs);
    }

    public synchronized void restaurarCobertura() {
        sinCoberturaHastaMs = Long.MIN_VALUE;
    }

    public synchronized boolean enCobertura(long ahoraMs) {
        return ahoraMs >= sinCoberturaHastaMs;
    }

    public synchronized List<Posicion> tomarParaEnviar(long ahoraMs, int max) {
        if (enVuelo || !enCobertura(ahoraMs) || ahoraMs < esperarHastaMs || buffer.isEmpty()) {
            return List.of();
        }
        List<Posicion> envio = new ArrayList<>(Math.min(max, buffer.size()));
        while (!buffer.isEmpty() && envio.size() < max) {
            envio.add(buffer.pollFirst());
        }
        enVuelo = true;
        return envio;
    }

    public synchronized void resultado(List<Posicion> enviadas, Respuesta respuesta, long ahoraMs) {
        enVuelo = false;
        switch (respuesta.tipo()) {
            case ACEPTADA, RECHAZADA -> { }
            case SATURADA -> {
                devolver(enviadas);
                esperarHastaMs = ahoraMs + Math.max(1, respuesta.reintentarSeg()) * 1000L;
            }
            case ERROR -> {
                devolver(enviadas);
                esperarHastaMs = ahoraMs + ESPERA_ERROR_MS;
            }
        }
    }

    public synchronized void reinyectar(Posicion p) {
        buffer.addLast(p.comoDuplicada());
    }

    public synchronized int pendientes() {
        return buffer.size();
    }

    public synchronized boolean enVuelo() {
        return enVuelo;
    }

    private void devolver(List<Posicion> enviadas) {
        for (int i = enviadas.size() - 1; i >= 0; i--) {
            buffer.addFirst(enviadas.get(i));
        }
    }

    private Posicion nuevaPosicion(long ahoraMs, Random azar) {
        avance += (intervaloMs / 1000.0) / DURACION_VIAJE_SEG;
        if (avance >= 1) {
            avance -= 1;
            vuelta = !vuelta;
        }
        double[] desde = vuelta ? destino : origen;
        double[] hasta = vuelta ? origen : destino;
        double lat = desde[0] + (hasta[0] - desde[0]) * avance + (azar.nextDouble() - 0.5) * RUIDO;
        double lon = desde[1] + (hasta[1] - desde[1]) * avance + (azar.nextDouble() - 0.5) * RUIDO;
        String instante = Instant.ofEpochMilli(ahoraMs).atOffset(ZoneOffset.UTC).toString();
        return new Posicion(UUID.randomUUID().toString(), matricula, lat, lon, instante, false);
    }
}
