package uy.edu.fing.tse.carga.ejb.jms;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import uy.edu.fing.tse.carga.api.contract.service.DeteccionLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.ejb.servicio.SincronizacionPesajesBean;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumidor de la cola de pesadas nuevas (la publica la sincronizacion de pesajes, P4) para la
 * deteccion de incumplimientos (P5, ADR-0022). Es el desacople que pide la letra: la obtencion de
 * pesajes no espera a las reglas, y si las reglas fallan la pesada ya quedo guardada.
 *
 * Mismo criterio que los otros MDB: un mensaje ilegible se descarta (reintentarlo no lo arregla) y una
 * falla tecnica deshace la transaccion para que el broker lo reentregue. Reentregar es seguro: la
 * deteccion no crea dos casos para el mismo hecho.
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = SincronizacionPesajesBean.COLA),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class DeteccionPesadaMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(DeteccionPesadaMDB.class.getName());

    @EJB
    DeteccionLocal deteccion;

    @Resource
    private MessageDrivenContext contexto;

    @Override
    public void onMessage(Message message) {
        PesadaRegistradaDTO pesada;
        try {
            pesada = Json.JSONB.fromJson(message.getBody(String.class), PesadaRegistradaDTO.class);
        } catch (Exception e) {
            LOG.warning("Aviso de pesada descartado: no se pudo leer (" + e.getMessage() + ")");
            return;
        }
        procesar(pesada);
    }

    void procesar(PesadaRegistradaDTO pesada) {
        if (pesada == null || pesada.getMatricula() == null) {
            LOG.warning("Aviso de pesada descartado: sin matricula");
            return;
        }
        try {
            deteccion.evaluarPesada(pesada);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo evaluar la pesada " + pesada.getIdPesada()
                    + "; se deshace para que el broker la vuelva a entregar", e);
            deshacer();
        }
    }

    void deshacer() {
        contexto.setRollbackOnly();
    }

    private static final class Json {
        static final Jsonb JSONB = JsonbBuilder.create();
    }
}
