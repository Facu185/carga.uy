package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import uy.edu.fing.tse.carga.api.contract.service.AvisoFinDeViajeLocal;
import uy.edu.fing.tse.carga.api.dto.comun.ViajeFinalizadoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.jms.FinDeViajeMDB;

import java.time.OffsetDateTime;

/**
 * Recibe el aviso de fin de viaje del frente P3 y lo encola para FinDeViajeMDB (ADR-0010).
 * Valida antes de encolar: un aviso mal formado lo rechaza aca, donde quien llama puede corregirlo,
 * en vez de descubrirse despues en la cola.
 */
@Stateless
public class AvisoFinDeViajeBean implements AvisoFinDeViajeLocal {

    @Inject
    JMSContext jms;

    @Resource(lookup = FinDeViajeMDB.COLA)
    Queue cola;

    @Override
    public void avisar(ViajeFinalizadoDTO viaje) throws NegocioException {
        if (viaje == null || viaje.getMatricula() == null || viaje.getMatricula().isBlank()) {
            throw new NegocioException("El aviso de fin de viaje necesita la matricula del vehiculo.");
        }
        OffsetDateTime fin = RecorridoBean.instante(viaje.getFin(), "fin");
        if (viaje.getInicio() != null && !viaje.getInicio().isBlank()
                && RecorridoBean.instante(viaje.getInicio(), "inicio").isAfter(fin)) {
            throw new NegocioException("El inicio del viaje no puede ser posterior al fin.");
        }
        publicar(viaje);
    }

    /** Package-private para verificarlo en las pruebas, sin broker ni JSON-B. */
    void publicar(ViajeFinalizadoDTO viaje) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            jms.createProducer().send(cola, jsonb.toJson(viaje));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo encolar el aviso de fin de viaje", e);
        }
    }
}
