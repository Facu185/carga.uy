package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import uy.edu.fing.tse.carga.api.contract.service.IngestaTrackingLocal;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.jms.PosicionMDB;
import uy.edu.fing.tse.carga.ejb.modelo.ValidacionPosicion;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Recibe posiciones del nodo Tracking y las encola.
 * La recepcion queda desacoplada del procesamiento: este metodo valida, publica el mensaje y
 * retorna, de modo que un pico de eventos no degrada la recepcion (Secciones 3.7.3 y 4.1 de la letra).
 *
 * Lo que no pasa la validacion se rechaza aca (400 para el nodo) y no llega a la cola.
 */
@Stateless
public class IngestaTrackingBean implements IngestaTrackingLocal {

    private static final Logger LOG = Logger.getLogger(IngestaTrackingBean.class.getName());

    @Inject
    JMSContext jms;

    @Resource(lookup = PosicionMDB.COLA)
    Queue colaPosiciones;

    @EJB
    MetricasLocal metricas;

    @Override
    public void encolarPosicion(PosicionDTO posicion) throws NegocioException {
        Optional<String> problema = ValidacionPosicion.problema(posicion, Instant.now());
        if (problema.isPresent()) {
            metricas.contar(PosicionMDB.METRICA_POSICIONES, "resultado", "rechazada");
            throw new NegocioException(problema.get());
        }
        publicar(posicion, System.currentTimeMillis());
        metricas.contar(PosicionMDB.METRICA_POSICIONES, "resultado", "aceptada");
    }

    void publicar(PosicionDTO posicion, long recibidoEnMs) {
        try {
            jms.createProducer()
                .setProperty(PosicionMDB.PROPIEDAD_RECIBIDO_EN, recibidoEnMs)
                .send(colaPosiciones, Json.JSONB.toJson(posicion));
        } catch (RuntimeException e) {
            LOG.severe("No se pudo encolar la posicion " + posicion.getIdEvento() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private static final class Json {
        static final Jsonb JSONB = JsonbBuilder.create();
    }
}
