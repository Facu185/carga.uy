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
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoLoteDTO;
import uy.edu.fing.tse.carga.api.exception.IngestaSaturadaException;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.jms.MonitorColaPosiciones;
import uy.edu.fing.tse.carga.ejb.jms.PosicionMDB;
import uy.edu.fing.tse.carga.ejb.modelo.ValidacionPosicion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;


@Stateless
public class IngestaTrackingBean implements IngestaTrackingLocal {

    private static final Logger LOG = Logger.getLogger(IngestaTrackingBean.class.getName());
    static final int LOTE_MAX = entero("TRACKING_LOTE_MAX", 500);

    @Inject
    JMSContext jms;

    @Resource(lookup = PosicionMDB.COLA)
    Queue colaPosiciones;

    @EJB
    MetricasLocal metricas;

    @EJB
    MonitorColaPosiciones cola;

    @Override
    public void encolarPosicion(PosicionDTO posicion) throws NegocioException {
        verificarCapacidad(1);
        Optional<String> problema = ValidacionPosicion.problema(posicion, Instant.now());
        if (problema.isPresent()) {
            metricas.contar(PosicionMDB.METRICA_POSICIONES, "resultado", "rechazada");
            throw new NegocioException(problema.get());
        }
        publicar(posicion, System.currentTimeMillis());
        metricas.contar(PosicionMDB.METRICA_POSICIONES, "resultado", "aceptada");
    }

    @Override
    public ResultadoLoteDTO encolarLote(List<PosicionDTO> posiciones) throws NegocioException {
        if (posiciones == null || posiciones.isEmpty()) {
            throw new NegocioException("El lote no puede estar vacio.");
        }
        if (posiciones.size() > LOTE_MAX) {
            throw new NegocioException("El lote no puede superar " + LOTE_MAX + " posiciones (llegaron "
                + posiciones.size() + ").");
        }
        verificarCapacidad(posiciones.size());

        Instant ahora = Instant.now();
        long recibidoEnMs = System.currentTimeMillis();
        ResultadoLoteDTO resultado = new ResultadoLoteDTO();
        resultado.setRecibidas(posiciones.size());
        int aceptadas = 0;
        for (int i = 0; i < posiciones.size(); i++) {
            PosicionDTO posicion = posiciones.get(i);
            Optional<String> problema = ValidacionPosicion.problema(posicion, ahora);
            if (problema.isPresent()) {
                resultado.agregarRechazo(i, posicion == null ? null : posicion.getIdEvento(), problema.get());
                metricas.contar(PosicionMDB.METRICA_POSICIONES, "resultado", "rechazada");
                continue;
            }
            publicar(posicion, recibidoEnMs);
            aceptadas++;
            metricas.contar(PosicionMDB.METRICA_POSICIONES, "resultado", "aceptada");
        }
        resultado.setAceptadas(aceptadas);
        return resultado;
    }

    private void verificarCapacidad(int cantidad) {
        if (cola.saturada()) {
            for (int i = 0; i < cantidad; i++) {
                metricas.contar(PosicionMDB.METRICA_POSICIONES, "resultado", "saturada");
            }
            throw new IngestaSaturadaException("La ingesta de posiciones esta saturada; reintentar en "
                + cola.reintentarEnSegundos() + " s.", cola.reintentarEnSegundos());
        }
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

    private static int entero(String variable, int porDefecto) {
        String v = System.getenv(variable);
        try {
            return v == null || v.isBlank() ? porDefecto : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    private static final class Json {
        static final Jsonb JSONB = JsonbBuilder.create();
    }
}
