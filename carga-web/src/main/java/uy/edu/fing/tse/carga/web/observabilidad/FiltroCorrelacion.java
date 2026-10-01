package uy.edu.fing.tse.carga.web.observabilidad;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.MDC;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Para cada pedido: toma (o genera) el ID de correlacion, lo deja en el MDC mientras dura el pedido,
 * lo devuelve en la respuesta y escribe una linea de log con metodo, ruta, estado y duracion.
 * Esa linea tambien sirve de evidencia para la prueba de performance (latencia por pedido).
 */
@WebFilter(urlPatterns = "/*")
public class FiltroCorrelacion implements Filter {

    private static final Logger LOG = Logger.getLogger(FiltroCorrelacion.class.getName());

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
            MDC.remove(Correlacion.CLAVE_MDC);
            MDC.remove(Correlacion.CLAVE_USUARIO_MDC);
        }
    }
}
