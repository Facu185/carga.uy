package uy.edu.fing.tse.carga.api.observabilidad;

import org.jboss.logging.MDC;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Identificador de correlacion (Seccion 4.4 de la letra, punto 7): permite seguir una misma operacion
 * a traves del frontend, el central y los sistemas que este llama.
 *
 * Viaja en el header X-Correlation-Id. Mientras se atiende un pedido queda en el MDC con la clave
 * "idCorrelacion", asi que todo log que se escriba en ese hilo lo incluye solo (los logs del central
 * salen en JSON con el MDC adentro).
 */
public final class Correlacion {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CLAVE_MDC = "idCorrelacion";
    public static final String CLAVE_USUARIO_MDC = "usuario";

    /** Solo caracteres seguros: un ID que viene de afuera no puede inyectar texto en los logs. */
    private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    private Correlacion() {
    }

    /** Usa el ID recibido si es valido; si no, genera uno nuevo. */
    public static String validarOGenerar(String recibido) {
        if (recibido != null && FORMATO.matcher(recibido.trim()).matches()) {
            return recibido.trim();
        }
        return UUID.randomUUID().toString();
    }

    /** ID de la operacion que se esta atendiendo en este hilo. */
    public static Optional<String> actual() {
        Object valor = MDC.get(CLAVE_MDC);
        return valor == null ? Optional.empty() : Optional.of(valor.toString());
    }

    /**
     * Ejecuta una tarea con el ID de correlacion dado (por ejemplo en otro hilo: el MDC es por hilo
     * y no se copia solo) y despues deja el MDC como estaba.
     */
    public static <T> T ejecutarCon(String id, Callable<T> tarea) throws Exception {
        if (id == null) {
            return tarea.call();
        }
        Object anterior = MDC.get(CLAVE_MDC);
        MDC.put(CLAVE_MDC, id);
        try {
            return tarea.call();
        } finally {
            if (anterior == null) {
                MDC.remove(CLAVE_MDC);
            } else {
                MDC.put(CLAVE_MDC, anterior);
            }
        }
    }
}
