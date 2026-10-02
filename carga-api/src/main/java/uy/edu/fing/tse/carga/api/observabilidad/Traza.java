package uy.edu.fing.tse.carga.api.observabilidad;

import org.jboss.logging.MDC;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trazado distribuido (Seccion 4.4, punto 9 de la letra) con el estandar W3C Trace Context.
 *
 * Cada operacion es un "tramo" (span) con un traceId compartido por todo el flujo y un spanId propio.
 * Entre componentes viaja el header {@code traceparent: 00-<traceId>-<spanId>-01}; quien lo recibe
 * abre un tramo hijo con el mismo traceId. Los tramos terminados se exportan a Jaeger (ExportadorOtlp)
 * y el traceId queda en el MDC, asi los logs JSON de una operacion se encuentran desde la traza.
 *
 * Uso:
 * <pre>
 *   try (Traza.Tramo tramo = Traza.iniciar("pdi.obtenerPersona", Traza.Tipo.CLIENTE)) {
 *       ... llamada ...
 *   }
 * </pre>
 */
public final class Traza {

    public static final String HEADER = "traceparent";
    public static final String CLAVE_MDC_TRAZA = "traceId";
    public static final String CLAVE_MDC_TRAMO = "spanId";

    private static final Pattern TRACEPARENT = Pattern.compile("00-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}");
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final ThreadLocal<Tramo> ACTUAL = new ThreadLocal<>();
    /** Contexto recibido sin tramo abierto (por ejemplo, en el hilo del timeout de la PDI). */
    private static final ThreadLocal<String> PADRE_REMOTO = new ThreadLocal<>();

    private static volatile String servicio = valorOPorDefecto(System.getenv("OTEL_SERVICE_NAME"), "central");
    private static volatile Consumer<Tramo> destino = ExportadorOtlp.desdeEntorno();

    /** Tipos de tramo de OpenTelemetry (los numeros son los del protocolo OTLP). */
    public enum Tipo {
        INTERNO(1), SERVIDOR(2), CLIENTE(3);

        final int codigo;

        Tipo(int codigo) {
            this.codigo = codigo;
        }
    }

    private Traza() {
    }

    /** Nombre del componente en Jaeger. Cada despliegue tiene su copia de esta clase y su nombre. */
    public static void usarServicio(String nombre) {
        servicio = nombre;
    }

    /** Para las pruebas: a donde van los tramos terminados. */
    public static void exportarA(Consumer<Tramo> nuevoDestino) {
        destino = nuevoDestino;
    }

    /** Abre un tramo hijo del actual (o de un contexto recibido); si no hay ninguno, empieza una traza nueva. */
    public static Tramo iniciar(String nombre, Tipo tipo) {
        Tramo padre = ACTUAL.get();
        if (padre != null) {
            return new Tramo(nombre, tipo, padre.traceId, padre.spanId, padre);
        }
        return iniciarDesde(PADRE_REMOTO.get(), nombre, tipo);
    }

    /** Abre un tramo continuando el traceparent recibido de otro componente (o uno nuevo si no es valido). */
    public static Tramo iniciarDesde(String traceparent, String nombre, Tipo tipo) {
        Matcher m = traceparent == null ? null : TRACEPARENT.matcher(traceparent.trim());
        if (m != null && m.matches() && !m.group(1).matches("0+") && !m.group(2).matches("0+")) {
            return new Tramo(nombre, tipo, m.group(1), m.group(2), ACTUAL.get());
        }
        return new Tramo(nombre, tipo, hex(16), null, ACTUAL.get());
    }

    /** traceparent para mandarle a otro componente, si hay una traza en curso en este hilo. */
    public static Optional<String> traceparentActual() {
        Tramo actual = ACTUAL.get();
        if (actual != null) {
            return Optional.of(actual.traceparent());
        }
        return Optional.ofNullable(PADRE_REMOTO.get());
    }

    /** Ejecuta en otro hilo con el contexto de traza dado (el ThreadLocal no se copia solo). */
    public static <T> T ejecutarCon(String traceparent, Callable<T> tarea) throws Exception {
        if (traceparent == null) {
            return tarea.call();
        }
        String anterior = PADRE_REMOTO.get();
        PADRE_REMOTO.set(traceparent);
        try {
            return tarea.call();
        } finally {
            if (anterior == null) {
                PADRE_REMOTO.remove();
            } else {
                PADRE_REMOTO.set(anterior);
            }
        }
    }

    static String hex(int bytes) {
        byte[] b = new byte[bytes];
        ALEATORIO.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private static String valorOPorDefecto(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }

    /** Una operacion medida. Se cierra con try-with-resources y ahi se exporta. */
    public static final class Tramo implements AutoCloseable {
        final String servicioDelTramo;
        final String nombre;
        final Tipo tipo;
        final String traceId;
        final String spanId;
        final String padreId;
        final long inicioNanos;
        final long inicioEpochNanos;
        long finEpochNanos;
        boolean conError;
        final Map<String, String> atributos = new LinkedHashMap<>();
        private final Tramo anterior;
        private final Object mdcTrazaAnterior;
        private final Object mdcTramoAnterior;
        private boolean cerrado;

        private Tramo(String nombre, Tipo tipo, String traceId, String padreId, Tramo anterior) {
            this.servicioDelTramo = servicio;
            this.nombre = nombre;
            this.tipo = tipo;
            this.traceId = traceId;
            this.spanId = hex(8);
            this.padreId = padreId;
            this.anterior = anterior;
            this.inicioNanos = System.nanoTime();
            this.inicioEpochNanos = System.currentTimeMillis() * 1_000_000L;
            this.mdcTrazaAnterior = MDC.get(CLAVE_MDC_TRAZA);
            this.mdcTramoAnterior = MDC.get(CLAVE_MDC_TRAMO);
            ACTUAL.set(this);
            MDC.put(CLAVE_MDC_TRAZA, traceId);
            MDC.put(CLAVE_MDC_TRAMO, spanId);
        }

        public Tramo atributo(String clave, Object valor) {
            if (valor != null) {
                atributos.put(clave, String.valueOf(valor));
            }
            return this;
        }

        public Tramo error(Throwable causa) {
            conError = true;
            return atributo("error.tipo", causa.getClass().getSimpleName());
        }

        public Tramo marcarError() {
            conError = true;
            return this;
        }

        public String traceparent() {
            return "00-" + traceId + "-" + spanId + "-01";
        }

        public String getTraceId() {
            return traceId;
        }

        @Override
        public void close() {
            if (cerrado) {
                return;
            }
            cerrado = true;
            finEpochNanos = inicioEpochNanos + (System.nanoTime() - inicioNanos);
            if (anterior == null) {
                ACTUAL.remove();
            } else {
                ACTUAL.set(anterior);
            }
            restaurar(CLAVE_MDC_TRAZA, mdcTrazaAnterior);
            restaurar(CLAVE_MDC_TRAMO, mdcTramoAnterior);
            try {
                destino.accept(this);
            } catch (RuntimeException e) {
                // exportar nunca puede romper la operacion que se esta midiendo
            }
        }

        private static void restaurar(String clave, Object valor) {
            if (valor == null) {
                MDC.remove(clave);
            } else {
                MDC.put(clave, valor);
            }
        }
    }
}
