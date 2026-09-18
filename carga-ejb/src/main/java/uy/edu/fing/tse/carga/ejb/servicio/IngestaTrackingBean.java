package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import uy.edu.fing.tse.carga.api.IngestaTrackingLocal;
import uy.edu.fing.tse.carga.api.dto.PosicionDTO;
import uy.edu.fing.tse.carga.ejb.jms.PosicionMDB;

import jakarta.annotation.Resource;
import java.util.logging.Logger;

/**
 * Recibe posiciones del nodo Tracking y las encola.
 * La recepcion queda desacoplada del procesamiento: este metodo solo publica el
 * mensaje y retorna, de modo que un pico de eventos no degrada la recepcion
 * (Secciones 3.7.3 y 4.1 de la letra).
 */
@Stateless
public class IngestaTrackingBean implements IngestaTrackingLocal {

    private static final Logger LOG = Logger.getLogger(IngestaTrackingBean.class.getName());

    @Inject
    private JMSContext jms;

    @Resource(lookup = PosicionMDB.COLA)
    private Queue colaPosiciones;

    @Override
    public void encolarPosicion(PosicionDTO posicion) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            jms.createProducer().send(colaPosiciones, jsonb.toJson(posicion));
        } catch (Exception e) {
            LOG.severe("No se pudo encolar la posicion " + posicion.getIdEvento() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
