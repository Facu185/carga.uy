package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import uy.edu.fing.tse.carga.api.contract.service.RecorridoLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.PuntoRecorridoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.PosicionTracking;
import uy.edu.fing.tse.carga.ejb.persistencia.RecorridoDAO;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Recorrido de un vehiculo (frente P4), para la regla de desvio de ruta (frente P5).
 *
 * Topes, para que una consulta no pueda traer millones de filas (al volumen base del ADR-0008 un
 * vehiculo reporta unas 2.900 posiciones por dia): 7 dias por consulta por fechas y 10.000 puntos
 * por respuesta. La distancia se calcula siempre sobre el periodo completo, aunque se trunquen los
 * puntos.
 */
@Stateless
public class RecorridoBean implements RecorridoLocal {

    static final Duration RANGO_MAXIMO = Duration.ofDays(7);
    static final int PUNTOS_MAXIMOS = 10_000;

    @EJB
    RecorridoDAO dao;

    @Override
    public RecorridoVehiculoDTO recorrido(String matricula, String desde, String hasta) throws NegocioException {
        String m = matriculaValida(matricula);
        OffsetDateTime d = instante(desde, "desde");
        OffsetDateTime h = instante(hasta, "hasta");
        if (!d.isBefore(h)) {
            throw new NegocioException("El inicio del periodo tiene que ser anterior al fin.");
        }
        if (Duration.between(d, h).compareTo(RANGO_MAXIMO) > 0) {
            throw new NegocioException("El periodo puede ser de hasta " + RANGO_MAXIMO.toDays()
                    + " dias. Para un viaje completo, pedi el recorrido de la guia.");
        }
        return armar(m, d, h, null);
    }

    @Override
    public RecorridoVehiculoDTO recorridoDeGuia(Long guiaId) throws NegocioException {
        RecorridoDAO.PeriodoDeGuia guia = dao.periodoDeGuia(guiaId)
                .orElseThrow(() -> new NegocioException("No existe la guia indicada."));
        return armar(guia.matricula(), guia.inicio().atOffset(ZoneOffset.UTC), guia.fin().atOffset(ZoneOffset.UTC), guiaId);
    }

    private RecorridoVehiculoDTO armar(String matricula, OffsetDateTime desde, OffsetDateTime hasta, Long guiaId) {
        // uno mas que el maximo: si vuelve, es que habia mas y el recorrido esta truncado
        List<PosicionTracking> posiciones = dao.posiciones(matricula, desde, hasta, PUNTOS_MAXIMOS + 1);
        boolean truncado = posiciones.size() > PUNTOS_MAXIMOS;

        RecorridoVehiculoDTO r = new RecorridoVehiculoDTO();
        r.setMatricula(matricula);
        r.setGuiaId(guiaId);
        r.setDesde(desde.toString());
        r.setHasta(hasta.toString());
        r.setTruncado(truncado);
        r.setPuntos((truncado ? posiciones.subList(0, PUNTOS_MAXIMOS) : posiciones).stream()
                .map(p -> new PuntoRecorridoDTO(p.getLatitud(), p.getLongitud(), p.getInstante().toString()))
                .toList());
        r.setDistanciaMetros(posiciones.isEmpty() ? 0 : dao.distanciaMetros(matricula, desde, hasta));
        return r;
    }

    private static String matriculaValida(String matricula) throws NegocioException {
        if (matricula == null || matricula.isBlank()) {
            throw new NegocioException("La matricula es obligatoria.");
        }
        return matricula.trim().toUpperCase();
    }

    static OffsetDateTime instante(String valor, String campo) throws NegocioException {
        if (valor == null || valor.isBlank()) {
            throw new NegocioException("Falta '" + campo + "' (ISO-8601 con zona, por ejemplo 2026-10-18T08:00:00-03:00).");
        }
        try {
            return OffsetDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new NegocioException("'" + campo + "' tiene que ser ISO-8601 con zona, por ejemplo 2026-10-18T08:00:00-03:00.");
        }
    }
}
