package uy.edu.fing.tse.carga.ejb.observabilidad;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Registro de metricas en memoria, exportado en formato de texto de Prometheus.
 *
 * Hecho a mano y no con la libreria de WildFly a proposito: no requiere configurar el servidor, se
 * prueba con tests unitarios y cualquier frente lo usa por la interfaz. Los valores son por instancia
 * del central (si se escala horizontalmente, Prometheus suma las instancias).
 *
 * Singleton con concurrencia BEAN: LongAdder y los mapas concurrentes ya son thread-safe, asi que
 * registrar una metrica nunca bloquea un pedido.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class Metricas implements MetricasLocal {

    /** Limites de los histogramas de duracion, en milisegundos. */
    static final long[] LIMITES_MS = {5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000};

    private static final Pattern NOMBRE = Pattern.compile("[a-zA-Z_:][a-zA-Z0-9_:]*");

    private enum Tipo { counter, histogram, gauge }

    private record Familia(Tipo tipo, Map<String, Object> series) {
    }

    /** Ordenado por nombre para que la salida sea estable. */
    private final Map<String, Familia> familias = new ConcurrentSkipListMap<>();

    @PostConstruct
    void registrarMetricasDeLaJvm() {
        MemoryMXBean memoria = ManagementFactory.getMemoryMXBean();
        registrarMedidor("carga_jvm_memoria_heap_usada_bytes", () -> memoria.getHeapMemoryUsage().getUsed());
        registrarMedidor("carga_jvm_hilos", () -> ManagementFactory.getThreadMXBean().getThreadCount());
    }

    @Override
    public void contar(String metrica, String... etiquetas) {
        ((LongAdder) serie(metrica, Tipo.counter, etiquetas, LongAdder::new)).increment();
    }

    @Override
    public void observarDuracion(String metrica, long milisegundos, String... etiquetas) {
        ((Histograma) serie(metrica, Tipo.histogram, etiquetas, Histograma::new)).observar(Math.max(0, milisegundos));
    }

    @Override
    public void registrarMedidor(String metrica, Supplier<Number> fuente, String... etiquetas) {
        familia(metrica, Tipo.gauge).series().put(formatearEtiquetas(etiquetas), fuente);
    }

    @Override
    public String exportarPrometheus() {
        StringBuilder salida = new StringBuilder();
        familias.forEach((nombre, familia) -> {
            salida.append("# TYPE ").append(nombre).append(' ').append(familia.tipo()).append('\n');
            new ConcurrentSkipListMap<>(familia.series()).forEach((etiquetas, valor) -> {
                switch (familia.tipo()) {
                    case counter -> linea(salida, nombre, etiquetas, ((LongAdder) valor).sum());
                    case gauge -> linea(salida, nombre, etiquetas, leer(valor));
                    case histogram -> ((Histograma) valor).exportar(salida, nombre, etiquetas);
                }
            });
        });
        return salida.toString();
    }

    // ---------------- internos ----------------

    private Object serie(String metrica, Tipo tipo, String[] etiquetas, Supplier<Object> crear) {
        return familia(metrica, tipo).series().computeIfAbsent(formatearEtiquetas(etiquetas), k -> crear.get());
    }

    private Familia familia(String metrica, Tipo tipo) {
        if (metrica == null || !NOMBRE.matcher(metrica).matches()) {
            throw new IllegalArgumentException("Nombre de m\u00e9trica inv\u00e1lido: " + metrica);
        }
        Familia familia = familias.computeIfAbsent(metrica, n -> new Familia(tipo, new ConcurrentHashMap<>()));
        if (familia.tipo() != tipo) {
            throw new IllegalArgumentException("La m\u00e9trica " + metrica + " ya existe como " + familia.tipo());
        }
        return familia;
    }

    /** Pares (nombre, valor) a {nombre="valor",...}, con los valores escapados como pide el formato. */
    static String formatearEtiquetas(String... etiquetas) {
        if (etiquetas == null || etiquetas.length == 0) {
            return "";
        }
        if (etiquetas.length % 2 != 0) {
            throw new IllegalArgumentException("Las etiquetas van de a pares (nombre, valor)");
        }
        StringBuilder texto = new StringBuilder("{");
        for (int i = 0; i < etiquetas.length; i += 2) {
            if (!NOMBRE.matcher(etiquetas[i]).matches()) {
                throw new IllegalArgumentException("Nombre de etiqueta inv\u00e1lido: " + etiquetas[i]);
            }
            if (i > 0) {
                texto.append(',');
            }
            String valor = etiquetas[i + 1] == null ? "" : etiquetas[i + 1];
            texto.append(etiquetas[i]).append("=\"")
                    .append(valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"))
                    .append('"');
        }
        return texto.append('}').toString();
    }

    private static double leer(Object fuente) {
        try {
            Number valor = ((Supplier<?>) fuente).get() instanceof Number n ? n : null;
            return valor == null ? Double.NaN : valor.doubleValue();
        } catch (RuntimeException e) {
            return Double.NaN; // una fuente que falla no rompe la exportacion de las demas
        }
    }

    private static void linea(StringBuilder salida, String nombre, String etiquetas, double valor) {
        salida.append(nombre).append(etiquetas).append(' ');
        if (valor == Math.rint(valor) && !Double.isInfinite(valor)) {
            salida.append((long) valor);
        } else {
            salida.append(valor);
        }
        salida.append('\n');
    }

    /** Conteos por intervalo; al exportar se acumulan (cada bucket incluye a los anteriores). */
    private static final class Histograma {
        private final LongAdder[] porIntervalo = new LongAdder[LIMITES_MS.length + 1];
        private final LongAdder suma = new LongAdder();

        Histograma() {
            for (int i = 0; i < porIntervalo.length; i++) {
                porIntervalo[i] = new LongAdder();
            }
        }

        void observar(long ms) {
            int i = 0;
            while (i < LIMITES_MS.length && ms > LIMITES_MS[i]) {
                i++;
            }
            porIntervalo[i].increment();
            suma.add(ms);
        }

        void exportar(StringBuilder salida, String nombre, String etiquetas) {
            String base = etiquetas.isEmpty() ? "" : etiquetas.substring(1, etiquetas.length() - 1) + ",";
            long acumulado = 0;
            for (int i = 0; i < porIntervalo.length; i++) {
                acumulado += porIntervalo[i].sum();
                String limite = i < LIMITES_MS.length ? String.valueOf(LIMITES_MS[i]) : "+Inf";
                linea(salida, nombre + "_bucket", "{" + base + "le=\"" + limite + "\"}", acumulado);
            }
            linea(salida, nombre + "_sum", etiquetas, suma.sum());
            linea(salida, nombre + "_count", etiquetas, acumulado);
        }
    }
}
