package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.MonitorLocal;
import uy.edu.fing.tse.carga.api.PesajeLocal;
import uy.edu.fing.tse.carga.api.dto.EstadoNodoDTO;
import uy.edu.fing.tse.carga.api.dto.MetricasDTO;
import uy.edu.fing.tse.carga.api.dto.RecorridoDTO;
import uy.edu.fing.tse.carga.api.dto.ResultadoPesajeDTO;
import uy.edu.fing.tse.carga.ejb.modelo.PosicionTracking;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Observabilidad de los nodos perifericos: lo que muestra el panel de operacion.
 *
 * El estado de cada nodo se verifica contra el nodo real en el momento de la
 * consulta, no contra un valor guardado: un estado cacheado puede decir "en linea"
 * de un nodo que se cayo hace cinco minutos.
 */
@Stateless
public class MonitorBean implements MonitorLocal {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
    /** Sin posiciones en esta ventana, damos el nodo Tracking por caido. */
    private static final int SILENCIO_TOLERADO_SEGUNDOS = 30;

    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @EJB
    private PesajeLocal pesaje;

    @Override
    public MetricasDTO metricas() {
        MetricasDTO m = new MetricasDTO();
        m.setPosicionesTotales(contar("select count(p) from PosicionTracking p"));
        m.setVehiculosReportando(contar("select count(distinct p.matricula) from PosicionTracking p"));
        m.setPosicionesUltimosCincoMinutos(em.createQuery(
                        "select count(p) from PosicionTracking p where p.recibidoEn > :desde", Long.class)
                .setParameter("desde", OffsetDateTime.now().minusMinutes(5))
                .getSingleResult());
        OffsetDateTime ultima = em.createQuery(
                        "select max(p.recibidoEn) from PosicionTracking p", OffsetDateTime.class)
                .getSingleResult();
        m.setUltimaRecepcion(ultima == null ? "sin datos" : RELOJ.format(ultima));
        return m;
    }

    @Override
    public List<EstadoNodoDTO> estadoNodos() {
        return List.of(estadoBalanzas(), estadoTracking());
    }

    @Override
    public List<RecorridoDTO> mapaFlota(int puntosPorVehiculo) {
        List<String> matriculas = em.createQuery(
                "select distinct p.matricula from PosicionTracking p order by p.matricula", String.class)
                .getResultList();

        List<RecorridoDTO> flota = new ArrayList<>();
        for (String matricula : matriculas) {
            List<PosicionTracking> ultimas = em.createQuery(
                            "select p from PosicionTracking p where p.matricula = :m order by p.instante desc",
                            PosicionTracking.class)
                    .setParameter("m", matricula)
                    .setMaxResults(Math.max(1, puntosPorVehiculo))
                    .getResultList();
            if (ultimas.isEmpty()) {
                continue;
            }
            PosicionTracking actual = ultimas.get(0);
            RecorridoDTO r = new RecorridoDTO();
            r.setMatricula(matricula);
            r.setLatitud(actual.getLatitud());
            r.setLongitud(actual.getLongitud());
            r.setInstante(RELOJ.format(actual.getInstante()));

            // la consulta viene del mas reciente al mas antiguo; el mapa dibuja al reves
            List<PosicionTracking> cronologico = new ArrayList<>(ultimas);
            Collections.reverse(cronologico);
            for (PosicionTracking p : cronologico) {
                r.getPuntos().add(new double[]{p.getLatitud(), p.getLongitud()});
            }
            flota.add(r);
        }
        return flota;
    }

    /** Se verifica con una consulta real al nodo, usando el mismo camino que el caso de uso. */
    private EstadoNodoDTO estadoBalanzas() {
        EstadoNodoDTO nodo = new EstadoNodoDTO("Balanzas peajes", "BALANZAS",
                System.getenv().getOrDefault("NODO_BALANZAS_URL", "http://balanzas:8080/balanzas/api/pesadas"));
        ResultadoPesajeDTO r = pesaje.consultarPesadas("", null);
        nodo.setEnLinea(r.isNodoDisponible());
        nodo.setLatenciaMs(r.getLatenciaMs());
        nodo.setDetalle(r.isNodoDisponible()
                ? "Responde a la consulta de pesadas."
                : r.getMensaje());
        return nodo;
    }

    /** No se puede consultar: el nodo Tracking empuja. Se deduce de cuando llego la ultima posicion. */
    private EstadoNodoDTO estadoTracking() {
        EstadoNodoDTO nodo = new EstadoNodoDTO("Tracking flota", "TRACKING", "ingesta en /carga/api/tracking/posiciones");
        OffsetDateTime ultima = em.createQuery(
                "select max(p.recibidoEn) from PosicionTracking p", OffsetDateTime.class).getSingleResult();
        if (ultima == null) {
            nodo.setEnLinea(false);
            nodo.setDetalle("Todavia no llego ninguna posicion.");
            return nodo;
        }
        long silencio = Duration.between(ultima, OffsetDateTime.now()).getSeconds();
        nodo.setEnLinea(silencio <= SILENCIO_TOLERADO_SEGUNDOS);
        nodo.setDetalle(nodo.isEnLinea()
                ? "Ultima posicion hace " + silencio + " s."
                : "Sin posiciones hace " + silencio + " s (tolerancia " + SILENCIO_TOLERADO_SEGUNDOS + " s).");
        return nodo;
    }

    private long contar(String jpql) {
        Long n = em.createQuery(jpql, Long.class).getSingleResult();
        return n == null ? 0 : n;
    }
}
