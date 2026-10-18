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
import uy.edu.fing.tse.carga.api.contract.service.SincronizacionPesajesLocal;
import uy.edu.fing.tse.carga.api.dto.comun.ViajeFinalizadoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoSincronizacionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.persistencia.PesadaDAO;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Al terminar un viaje, obtiene los pesajes de ese vehiculo para cada dia del viaje (caso de uso
 * critico 4, disparo "al finalizar un viaje"; el otro disparo es AgendaPesajesBean). ADR-0010.
 *
 * Mismo criterio que PosicionMDB: un mensaje mal formado se descarta (reintentarlo no lo arregla) y
 * una falla tecnica se deshace para que el broker lo vuelva a entregar. Volver a procesar es
 * inofensivo porque la sincronizacion no duplica pesadas. Si el nodo Balanzas no responde no es una
 * falla del mensaje: no se guardo nada y la agenda lo vuelve a intentar dentro de las 24 horas.
 */
@JMSDestinationDefinition(
        name = FinDeViajeMDB.COLA,
        interfaceName = "jakarta.jms.Queue",
        destinationName = "cola_viajes_finalizados")
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = FinDeViajeMDB.COLA),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class FinDeViajeMDB implements MessageListener {

    public static final String COLA = "java:/jms/queue/viajes-finalizados";
    static final String METRICA = "carga_viajes_finalizados_total";
    /** Un viaje mas largo que esto es casi seguro un dato mal cargado: no se recorren meses de dias. */
    static final int DIAS_MAXIMOS = 14;

    private static final Logger LOG = Logger.getLogger(FinDeViajeMDB.class.getName());

    @EJB
    SincronizacionPesajesLocal sincronizacion;

    @EJB
    MetricasLocal metricas;

    @Resource
    private MessageDrivenContext contexto;

    @Override
    public void onMessage(Message message) {
        ViajeFinalizadoDTO viaje;
        try {
            viaje = Json.JSONB.fromJson(message.getBody(String.class), ViajeFinalizadoDTO.class);
        } catch (Exception e) {
            metricas.contar(METRICA, "resultado", "descartado");
            LOG.warning("Aviso de fin de viaje descartado: no se pudo leer (" + e.getMessage() + ")");
            return;
        }
        procesar(viaje);
    }

    void procesar(ViajeFinalizadoDTO viaje) {
        List<LocalDate> dias;
        try {
            dias = diasDelViaje(viaje);
        } catch (IllegalArgumentException e) {
            metricas.contar(METRICA, "resultado", "descartado");
            LOG.warning("Aviso de fin de viaje descartado (" + e.getMessage() + ")");
            return;
        }
        try {
            boolean nodoDisponible = true;
            for (LocalDate dia : dias) {
                ResultadoSincronizacionDTO r = sincronizacion.sincronizar(viaje.getMatricula(), dia.toString());
                nodoDisponible &= r.isNodoDisponible();
            }
            metricas.contar(METRICA, "resultado", nodoDisponible ? "sincronizado" : "nodo_no_disponible");
        } catch (NegocioException e) {
            metricas.contar(METRICA, "resultado", "descartado");
            LOG.warning("Aviso de fin de viaje descartado (" + e.getMessage() + ")");
        } catch (RuntimeException e) {
            metricas.contar(METRICA, "resultado", "error");
            LOG.log(Level.WARNING, "No se pudo procesar el fin de viaje de " + viaje.getMatricula()
                    + "; se deshace para que el broker lo vuelva a entregar", e);
            deshacer();
        }
    }

    /** Los dias de Uruguay que toca el viaje, del inicio al fin. Sin inicio, solo el dia del fin. */
    static List<LocalDate> diasDelViaje(ViajeFinalizadoDTO viaje) {
        if (viaje == null || viaje.getMatricula() == null || viaje.getMatricula().isBlank()) {
            throw new IllegalArgumentException("sin matricula");
        }
        LocalDate fin = dia(viaje.getFin(), "fin");
        LocalDate inicio = viaje.getInicio() == null || viaje.getInicio().isBlank() ? fin : dia(viaje.getInicio(), "inicio");
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("el inicio es posterior al fin");
        }
        if (inicio.plusDays(DIAS_MAXIMOS).isBefore(fin)) {
            throw new IllegalArgumentException("viaje de mas de " + DIAS_MAXIMOS + " dias");
        }
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            dias.add(d);
        }
        return dias;
    }

    private static LocalDate dia(String iso, String campo) {
        if (iso == null || iso.isBlank()) {
            throw new IllegalArgumentException("sin " + campo);
        }
        try {
            return OffsetDateTime.parse(iso.trim()).atZoneSameInstant(PesadaDAO.ZONA).toLocalDate();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(campo + " no es ISO-8601 con zona");
        }
    }

    void deshacer() {
        contexto.setRollbackOnly();
    }

    /** Un solo Jsonb para todos los mensajes: crearlo es caro. */
    private static final class Json {
        static final Jsonb JSONB = JsonbBuilder.create();
    }
}
