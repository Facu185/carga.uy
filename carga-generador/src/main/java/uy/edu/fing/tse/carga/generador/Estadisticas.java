package uy.edu.fing.tse.carga.generador;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

public class Estadisticas {
    private final LongAdder generadas = new LongAdder();
    private final LongAdder duplicadasInyectadas = new LongAdder();
    private final LongAdder pedidos = new LongAdder();
    private final LongAdder posicionesEnviadas = new LongAdder();
    private final LongAdder aceptadas = new LongAdder();
    private final LongAdder unicasAceptadas = new LongAdder();
    private final LongAdder rechazadas = new LongAdder();
    private final LongAdder saturadas = new LongAdder();
    private final LongAdder errores = new LongAdder();

    private final Object candado = new Object();
    private long[] latIntervalo = new long[1024];
    private int nIntervalo;
    private long[] latTotal = new long[1024];
    private int nTotal;

    private long antPedidos;
    private long antPosiciones;
    private long antAceptadas;
    private long antRechazadas;
    private long antSaturadas;
    private long antErrores;

    public void generada() {
        generadas.increment();
    }

    public void duplicadaInyectada() {
        duplicadasInyectadas.increment();
    }

    public void registrar(List<Posicion> enviadas, Respuesta r) {
        pedidos.increment();
        posicionesEnviadas.add(enviadas.size());
        switch (r.tipo()) {
            case ACEPTADA -> {
                for (int i = 0; i < enviadas.size(); i++) {
                    if (r.rechazadas().contains(i)) {
                        rechazadas.increment();
                    } else {
                        aceptadas.increment();
                        if (!enviadas.get(i).duplicada()) {
                            unicasAceptadas.increment();
                        }
                    }
                }
            }
            case RECHAZADA -> rechazadas.add(enviadas.size());
            case SATURADA -> saturadas.add(enviadas.size());
            case ERROR -> errores.increment();
        }
        synchronized (candado) {
            if (nIntervalo == latIntervalo.length) {
                latIntervalo = Arrays.copyOf(latIntervalo, nIntervalo * 2);
            }
            latIntervalo[nIntervalo++] = r.latenciaMs();
            if (nTotal == latTotal.length) {
                latTotal = Arrays.copyOf(latTotal, nTotal * 2);
            }
            latTotal[nTotal++] = r.latenciaMs();
        }
    }

    public Intervalo tomarIntervalo(double segundos) {
        long[] lat;
        synchronized (candado) {
            lat = Arrays.copyOf(latIntervalo, nIntervalo);
            nIntervalo = 0;
        }
        Arrays.sort(lat);
        long ped = pedidos.sum();
        long pos = posicionesEnviadas.sum();
        long ace = aceptadas.sum();
        long rec = rechazadas.sum();
        long sat = saturadas.sum();
        long err = errores.sum();
        Intervalo i = new Intervalo((ped - antPedidos) / segundos, (pos - antPosiciones) / segundos,
            ace - antAceptadas, rec - antRechazadas, sat - antSaturadas, err - antErrores,
            percentil(lat, 50), percentil(lat, 95), percentil(lat, 99));
        antPedidos = ped;
        antPosiciones = pos;
        antAceptadas = ace;
        antRechazadas = rec;
        antSaturadas = sat;
        antErrores = err;
        return i;
    }

    public Total total() {
        long[] lat;
        synchronized (candado) {
            lat = Arrays.copyOf(latTotal, nTotal);
        }
        Arrays.sort(lat);
        return new Total(generadas.sum(), duplicadasInyectadas.sum(), pedidos.sum(), posicionesEnviadas.sum(),
            aceptadas.sum(), unicasAceptadas.sum(), rechazadas.sum(), saturadas.sum(), errores.sum(),
            percentil(lat, 50), percentil(lat, 95), percentil(lat, 99), lat.length == 0 ? 0 : lat[lat.length - 1]);
    }

    static long percentil(long[] ordenados, double p) {
        if (ordenados.length == 0) {
            return 0;
        }
        int indice = (int) Math.ceil(p / 100.0 * ordenados.length) - 1;
        return ordenados[Math.max(0, Math.min(indice, ordenados.length - 1))];
    }

    public record Intervalo(double pedidosPorSeg, double posicionesPorSeg, long aceptadas, long rechazadas,
                            long saturadas, long errores, long p50, long p95, long p99) {
    }

    public record Total(long generadas, long duplicadasInyectadas, long pedidos, long posicionesEnviadas,
                        long aceptadas, long unicasAceptadas, long rechazadas, long saturadas, long errores,
                        long p50, long p95, long p99, long maximo) {
    }
}
