package uy.edu.fing.tse.carga.balanzas;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.logging.Logger;

/**
 * El nodo solo le contesta al componente central: tiene que mandar el token compartido en
 * X-Nodo-Token (variable NODO_TOKEN). Las pesadas de los vehiculos no son publicas.
 * Falla cerrado: sin NODO_TOKEN configurado no contesta a nadie.
 */
@Provider
@PreMatching
public class FiltroToken implements ContainerRequestFilter {

    static final String HEADER = "X-Nodo-Token";

    private static final Logger LOG = Logger.getLogger(FiltroToken.class.getName());

    private final byte[] esperado = leerToken();

    @Override
    public void filter(ContainerRequestContext pedido) {
        String recibido = pedido.getHeaderString(HEADER);
        boolean valido = esperado != null && recibido != null
                && MessageDigest.isEqual(esperado, recibido.trim().getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            LOG.warning("Consulta rechazada: token del central ausente o invalido");
            pedido.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"error\":\"Central no autenticado\"}")
                    .build());
        }
    }

    private static byte[] leerToken() {
        String token = System.getenv("NODO_TOKEN");
        if (token == null || token.isBlank()) {
            LOG.severe("NODO_TOKEN no esta configurado: el nodo rechaza todas las consultas");
            return null;
        }
        return token.trim().getBytes(StandardCharsets.UTF_8);
    }
}
