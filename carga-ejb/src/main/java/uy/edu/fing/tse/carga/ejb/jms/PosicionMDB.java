package uy.edu.fing.tse.carga.ejb.jms;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.PosicionTracking;
import uy.edu.fing.tse.carga.ejb.modelo.ValidacionPosicion;
import uy.edu.fing.tse.carga.ejb.persistencia.PosicionDAO;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Procesa las posiciones encoladas por {@code IngestaTrackingBean} (Seccion 3.7.3 de la letra).
 *
 * Distingue dos clases de problema:
 * - Mensaje invalido (JSON roto, datos fuera de rango): reintentarlo no lo arregla. Se descarta,
 *   se loguea y se cuenta.
 * - Falla pasajera (la base no responde): se deshace la transaccion y el broker vuelve a entregar
 *   el mensaje. Tras varios intentos va a la DLQ (configurado en docker/central/configure.cli).
 *
 * Idempotente: el mismo idEvento procesado dos veces deja una sola fila (ON CONFLICT en PosicionDAO).
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

    public static final String PROPIEDAD_RECIBIDO_EN = "recibidoEnMs";
    public static final String METRICA_POSICIONES = "carga_tracking_posiciones_total";
    static final String METRICA_PROCESAMIENTO = "carga_tracking_procesamiento_ms";
    private static final Logger LOG = Logger.getLogger(PosicionMDB.class.getName());

    @EJB
    PosicionDAO posiciones;

    @EJB
    MetricasLocal metricas;

    @Resource
    private MessageDrivenContext contexto;

    @Override
    public void onMessage(Message message) {
        PosicionDTO dto;
        Long recibidoEnMs;
        try {
            recibidoEnMs = message.propertyExists(PROPIEDAD_RECIBIDO_EN)
                ? message.getLongProperty(PROPIEDAD_RECIBIDO_EN) : null;
            dto = Json.JSONB.fromJson(message.getBody(String.class), PosicionDTO.class);
        } catch (Exception e) {
            descartar("no se pudo leer el mensaje: " + e.getMessage(), null);
            return;
        }
        procesar(dto, recibidoEnMs);
    }

    void procesar(PosicionDTO dto, Long recibidoEnMs) {
        Optional<String> problema = ValidacionPosicion.problema(dto, Instant.now());
        if (problema.isPresent()) {
            descartar(problema.get(), dto);
            return;
        }
        boolean nueva;
        try {
            nueva = posiciones.guardarSiEsNueva(PosicionTracking.desde(dto, recibidoEn(recibidoEnMs)));
        } catch (RuntimeException e) {
            metricas.contar(METRICA_POSICIONES, "resultado", "error");
            LOG.log(Level.WARNING, "No se pudo guardar la posicion " + dto.getIdEvento()
                + "; se deshace para que el broker la vuelva a entregar", e);
            deshacer();
            return;
        }
        metricas.contar(METRICA_POSICIONES, "resultado", nueva ? "nueva" : "duplicada");
        if (recibidoEnMs != null) {
            metricas.observarDuracion(METRICA_PROCESAMIENTO, System.currentTimeMillis() - recibidoEnMs);
        }
        if (!nueva) {
            LOG.fine(() -> "Posicion duplicada descartada: " + dto.getIdEvento());
        }
    }

    void deshacer() {
        contexto.setRollbackOnly();
    }

    private void descartar(String motivo, PosicionDTO dto) {
        metricas.contar(METRICA_POSICIONES, "resultado", "descartada");
        LOG.warning("Posicion descartada (" + motivo + "): " + dto);
    }

    private static OffsetDateTime recibidoEn(Long recibidoEnMs) {
        return recibidoEnMs == null
            ? OffsetDateTime.now()
            : OffsetDateTime.ofInstant(Instant.ofEpochMilli(recibidoEnMs), ZoneOffset.UTC);
    }

    private static final class Json {
        static final Jsonb JSONB = JsonbBuilder.create();
    }
}
