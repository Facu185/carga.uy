package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Queue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.contract.service.PesajeLocal;
import uy.edu.fing.tse.carga.api.contract.service.SincronizacionPesajesLocal;
import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoPesajeDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoSincronizacionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.Pesada;
import uy.edu.fing.tse.carga.ejb.persistencia.PesadaDAO;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Caso de uso critico "Obtener pesajes del nodo Balanzas" (frente P4, Seccion 3.6 del SAD).
 *
 * Orden de las cosas: primero se consulta al nodo (sin tocar la base), despues se guarda cada pesada
 * y recien con la pesada guardada se publica el aviso. Todo en la misma transaccion: si algo falla
 * no queda ni la pesada sin aviso ni el aviso de una pesada que no existe.
 *
 * El aviso va a una cola y no a una llamada directa a P5 para que este frente no dependa de que la
 * deteccion de incumplimientos este terminada, ni de que ande en este momento.
 */
@JMSDestinationDefinition(
        name = SincronizacionPesajesBean.COLA,
        interfaceName = "jakarta.jms.Queue",
        destinationName = "cola_pesadas")
@Stateless
public class SincronizacionPesajesBean implements SincronizacionPesajesLocal {

    public static final String COLA = "java:/jms/queue/pesadas";

    private static final Logger LOG = Logger.getLogger(SincronizacionPesajesBean.class.getName());

    static final String METRICA_PESADAS = "carga_pesadas_total";

    @EJB
    PesajeLocal pesaje;

    @EJB
    PesadaDAO pesadas;

    @EJB
    MetricasLocal metricas;

    @Inject
    JMSContext jms;

    @Resource(lookup = COLA)
    Queue colaPesadas;

    @Override
    public ResultadoSincronizacionDTO sincronizar(String matricula, String fecha) throws NegocioException {
        String m = matriculaValida(matricula);
        LocalDate dia = diaValido(fecha);

        ResultadoPesajeDTO consulta = pesaje.consultarPesadas(m, dia.toString());
        if (!consulta.isNodoDisponible()) {
            return ResultadoSincronizacionDTO.nodoNoDisponible(consulta.getMensaje());
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        int nuevas = 0;
        int duplicadas = 0;
        int vinculadas = 0;
        int descartadas = 0;
        for (PesadaDTO dto : consulta.getPesadas()) {
            Pesada pesada;
            try {
                Long guiaId = pesadas.guiaVigente(dto.getMatricula(), Pesada.instanteDe(dto).toInstant())
                        .orElse(null);
                pesada = Pesada.desde(dto, guiaId, ahora);
            } catch (IllegalArgumentException e) {
                descartadas++;
                metricas.contar(METRICA_PESADAS, "resultado", "descartada");
                LOG.warning("Pesada descartada del nodo Balanzas: " + e.getMessage());
                continue;
            }
            if (pesadas.guardarSiEsNueva(pesada)) {
                nuevas++;
                if (pesada.getGuiaId() != null) {
                    vinculadas++;
                }
                metricas.contar(METRICA_PESADAS, "resultado", "nueva");
                publicar(pesadas.buscarPorIdPesada(pesada.getIdPesada()).orElse(pesada).aDTO());
            } else {
                duplicadas++;
                metricas.contar(METRICA_PESADAS, "resultado", "duplicada");
            }
        }
        return ResultadoSincronizacionDTO.completada(consulta.getPesadas().size(), nuevas, duplicadas,
                vinculadas, descartadas);
    }

    @Override
    public List<PesadaRegistradaDTO> registradas(String matricula, String fecha) throws NegocioException {
        return pesadas.buscar(matriculaValida(matricula), diaValido(fecha)).stream().map(Pesada::aDTO).toList();
    }

    /** Aviso a la deteccion de incumplimientos (P5). Package-private para verificarlo en las pruebas. */
    void publicar(PesadaRegistradaDTO pesada) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            jms.createProducer().send(colaPesadas, jsonb.toJson(pesada));
        } catch (Exception e) {
            // Sin aviso no hay deteccion: mejor que falle la transaccion y la pesada se vuelva a
            // traer en la proxima sincronizacion, que guardarla y que nadie se entere.
            throw new IllegalStateException("No se pudo avisar la pesada " + pesada.getIdPesada(), e);
        }
    }

    private static String matriculaValida(String matricula) throws NegocioException {
        if (matricula == null || matricula.isBlank()) {
            throw new NegocioException("La matricula es obligatoria.");
        }
        return matricula.trim().toUpperCase();
    }

    private static LocalDate diaValido(String fecha) throws NegocioException {
        if (fecha == null || fecha.isBlank()) {
            return LocalDate.now(PesadaDAO.ZONA);
        }
        try {
            return LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException e) {
            throw new NegocioException("La fecha tiene que tener el formato aaaa-mm-dd.");
        }
    }
}
