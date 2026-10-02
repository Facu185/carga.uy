package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.SaludLocal;
import uy.edu.fing.tse.carga.api.dto.EstadoSaludDTO;
import uy.edu.fing.tse.carga.ejb.integracion.CircuitBreaker;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Chequeo de estado ("readiness"): si el central puede atender pedidos.
 * - Base de datos: sin ella no hay nada que hacer, DOWN.
 * - PDI: con el circuito abierto el central sigue atendiendo en modo degradado (el ingreso con
 *   gub.uy queda pendiente de verificacion), asi que el componente queda DEGRADADO y el central UP.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SaludBean implements SaludLocal {

    private static final Logger LOG = Logger.getLogger(SaludBean.class.getName());

    static final String UP = "UP";
    static final String DOWN = "DOWN";
    static final String DEGRADADO = "DEGRADADO";

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    @EJB
    PdiBean pdi;

    @Override
    public EstadoSaludDTO verificar() {
        Map<String, String> componentes = new LinkedHashMap<>();
        componentes.put("baseDeDatos", baseDeDatosResponde() ? UP : DOWN);
        componentes.put("pdi", estadoPdi());
        String estado = DOWN.equals(componentes.get("baseDeDatos")) ? DOWN : UP;
        return new EstadoSaludDTO(estado, componentes, Instant.now().toString());
    }

    /** Package-private para poder simular una base caida en las pruebas. */
    boolean baseDeDatosResponde() {
        try {
            em.createNativeQuery("SELECT 1").getSingleResult();
            return true;
        } catch (RuntimeException e) {
            LOG.warning("La base de datos no responde: " + e.getMessage());
            return false;
        }
    }

    String estadoPdi() {
        return pdi.estadoCircuito() == CircuitBreaker.Estado.CERRADO ? UP : DEGRADADO;
    }
}
