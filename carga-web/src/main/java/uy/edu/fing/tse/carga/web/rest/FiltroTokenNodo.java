package uy.edu.fing.tse.carga.web.rest;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.logging.Logger;

/**
 * Autenticacion de los nodos perifericos ante el central (frente P4, ADR-0007).
 *
 * El nodo manda un token compartido en X-Nodo-Token. No va en Authorization porque ese header lo
 * procesa MecanismoJwt en todos los pedidos: un token de nodo no es un JWT y lo rechazaria con 401
 * antes de llegar aca.
 *
 * Falla cerrado: si el central no tiene NODO_TRACKING_TOKEN configurado, no acepta a ningun nodo.
 */
@Provider
@NodoAutenticado
@Priority(Priorities.AUTHENTICATION)
public class FiltroTokenNodo implements ContainerRequestFilter {

    public static final String HEADER = "X-Nodo-Token";

    private static final Logger LOG = Logger.getLogger(FiltroTokenNodo.class.getName());

    private final byte[] esperado;

    public FiltroTokenNodo() {
        this(System.getenv("NODO_TRACKING_TOKEN"));
    }

    FiltroTokenNodo(String tokenEsperado) {
        if (tokenEsperado == null || tokenEsperado.isBlank()) {
            LOG.severe("NODO_TRACKING_TOKEN no esta configurado: se rechaza a todos los nodos perifericos");
            esperado = null;
        } else {
            esperado = tokenEsperado.trim().getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public void filter(ContainerRequestContext pedido) {
        if (!autorizado(pedido.getHeaderString(HEADER))) {
            LOG.warning("Pedido de nodo rechazado: token ausente o invalido en /" + pedido.getUriInfo().getPath());
            pedido.abortWith(Errores.respuesta(401, "Nodo no autenticado"));
        }
    }

    /** Comparacion en tiempo constante: no deja adivinar el token midiendo cuanto tarda la respuesta. */
    boolean autorizado(String recibido) {
        if (esperado == null || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(esperado, recibido.trim().getBytes(StandardCharsets.UTF_8));
    }
}
