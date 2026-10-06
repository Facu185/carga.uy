package uy.edu.fing.tse.carga.ejb.jms;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.PosicionTracking;

import java.time.OffsetDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Procesa las posiciones encoladas por {@code IngestaTrackingBean}.
 * El procesamiento es idempotente: si el id de evento ya fue procesado, se descarta
 * (Seccion 3.7.3 de la letra).
 */
@JMSDestinationDefinition(
        name = PosicionMDB.COLA,
        interfaceName = "jakarta.jms.Queue",
        destinationName = "cola_posiciones")
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = PosicionMDB.COLA),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class PosicionMDB implements MessageListener {

    public static final String COLA = "java:/jms/queue/posiciones";

    private static final Logger LOG = Logger.getLogger(PosicionMDB.class.getName());

    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @Override
    public void onMessage(Message message) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            PosicionDTO dto = jsonb.fromJson(message.getBody(String.class), PosicionDTO.class);

            if (yaProcesada(dto.getIdEvento())) {
                LOG.fine(() -> "Posicion duplicada descartada: " + dto.getIdEvento());
                return;
            }

            PosicionTracking posicion = new PosicionTracking();
            posicion.setIdEvento(dto.getIdEvento());
            posicion.setMatricula(dto.getMatricula());
            posicion.setLatitud(dto.getLatitud());
            posicion.setLongitud(dto.getLongitud());
            posicion.setInstante(OffsetDateTime.parse(dto.getInstante()));
            posicion.setRecibidoEn(OffsetDateTime.now());
            em.persist(posicion);

            LOG.fine(() -> "Posicion registrada: " + dto.getIdEvento());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "No se pudo procesar la posicion", e);
        }
    }

    private boolean yaProcesada(String idEvento) {
        Long cantidad = em.createQuery(
                        "select count(p) from PosicionTracking p where p.idEvento = :id", Long.class)
                .setParameter("id", idEvento)
                .getSingleResult();
        return cantidad != null && cantidad > 0;
    }
}
