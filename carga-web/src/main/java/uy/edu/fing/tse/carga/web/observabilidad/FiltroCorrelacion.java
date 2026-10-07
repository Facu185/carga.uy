package uy.edu.fing.tse.carga.web.observabilidad;

import jakarta.ejb.EJB;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.MDC;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;
import uy.edu.fing.tse.carga.api.observabilidad.Traza;

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Para cada pedido: toma (o genera) el ID de correlacion, lo deja en el MDC mientras dura el pedido,
 * lo devuelve en la respuesta y escribe una linea de log con metodo, ruta, estado y duracion.
 * Esa linea tambien sirve de evidencia para la prueba de performance (latencia por pedido).
 * Ademas cuenta cada pedido y su duracion en las metricas, por ruta y por codigo de estado
 * (los 4xx y 5xx son los errores por tipo que pide la letra).
 * Y abre el tramo de servidor de la traza distribuida: continua el traceparent que mande el cliente
 * o empieza una traza nueva, y devuelve el traceId en X-Trace-Id para buscarla en Jaeger.
 */
@WebFilter(urlPatterns = "/*")
public class FiltroCorrelacion implements Filter {

    private static final Logger LOG = Logger.getLogger(FiltroCorrelacion.class.getName());
    /** Segmentos que son identificadores: numeros, cedulas, UUID. */
    private static final Pattern ID = Pattern.compile("\\d+|[0-9a-fA-F-]{16,}");

    @EJB
    MetricasLocal metricas;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest pedido = (HttpServletRequest) request;
        HttpServletResponse respuesta = (HttpServletResponse) response;

        String id = Correlacion.validarOGenerar(pedido.getHeader(Correlacion.HEADER));
        MDC.put(Correlacion.CLAVE_MDC, id);
        Principal usuario = pedido.getUserPrincipal();
        if (usuario != null) {
            MDC.put(Correlacion.CLAVE_USUARIO_MDC, usuario.getName());
        }
        respuesta.setHeader(Correlacion.HEADER, id);

        Traza.Tramo tramo = Traza.iniciarDesde(pedido.getHeader(Traza.HEADER),
                pedido.getMethod() + " " + rutaParaMetricas(pedido.getRequestURI(), pedido.getContextPath(), 200),
                Traza.Tipo.SERVIDOR)
                .atributo("http.method", pedido.getMethod())
                .atributo("idCorrelacion", id);
        respuesta.setHeader("X-Trace-Id", tramo.getTraceId());

        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
            // Los chequeos de salud se consultan muy seguido: van a FINE para no llenar el log
            String uri = pedido.getRequestURI();
            Level nivel = uri != null && uri.endsWith("/api/salud") ? Level.FINE : Level.INFO;
            LOG.log(nivel, () -> String.format("%s %s -> %d en %d ms",
                    pedido.getMethod(), uri, respuesta.getStatus(), milisegundos));
            String ruta = rutaParaMetricas(uri, pedido.getContextPath(), respuesta.getStatus());
            metricas.contar("carga_http_pedidos_total", "metodo", pedido.getMethod(), "ruta", ruta,
                    "estado", String.valueOf(respuesta.getStatus()));
            metricas.observarDuracion("carga_http_duracion_ms", milisegundos, "metodo", pedido.getMethod(),
                    "ruta", ruta);
            tramo.atributo("http.status_code", respuesta.getStatus());
            if (respuesta.getStatus() >= 500) {
                tramo.marcarError();
            }
            tramo.close();
            MDC.remove(Correlacion.CLAVE_MDC);
            MDC.remove(Correlacion.CLAVE_USUARIO_MDC);
        }
    }

    /**
     * Ruta sin el contexto y con los ids reemplazados por {id}: /carga/api/funcionarios/12/activo queda
     * /api/funcionarios/{id}/activo. Asi la cantidad de series no crece con los datos. Un 404 puede
     * ser cualquier cosa que alguien escribio, asi que va todo junto.
     */
    static String rutaParaMetricas(String uri, String contexto, int estado) {
        if (estado == 404 || uri == null) {
            return "desconocida";
        }
        String ruta = contexto != null && uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
        StringBuilder normalizada = new StringBuilder();
        for (String segmento : ruta.split("/")) {
            if (!segmento.isEmpty()) {
                normalizada.append('/').append(ID.matcher(segmento).matches() ? "{id}" : segmento);
            }
        }
        return normalizada.isEmpty() ? "/" : normalizada.toString();
    }
}
