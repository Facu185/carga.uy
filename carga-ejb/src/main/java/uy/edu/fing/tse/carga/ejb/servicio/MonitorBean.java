package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.MonitorLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.EstadoNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.MetricasDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoPesajeDTO;
import uy.edu.fing.tse.carga.ejb.integracion.CircuitBreaker;
import uy.edu.fing.tse.carga.ejb.modelo.NodoPeriferico;
import uy.edu.fing.tse.carga.ejb.modelo.TipoNodo;
import uy.edu.fing.tse.carga.ejb.persistencia.NodoPerifericoDAO;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    /** El mapa muestra la flota en ruta: vehiculos con posiciones en esta ventana. */
    static final int VENTANA_MAPA_HORAS = 6;
    /** Con mas filas que esto, el total de posiciones sale de la estadistica de Postgres y no de un count(*). */
    static final long TOTAL_EXACTO_HASTA = 100_000;

    /**
     * Las ultimas posiciones de cada vehiculo en una sola consulta (funcion de ventana sobre el indice
     * matricula + instante). Antes era una consulta por vehiculo: con 3.000 camiones, 3.001 consultas
     * cada vez que se abria el panel (prueba de performance, docs/informe-performance.md).
     */
    static final String SQL_MAPA = """
            SELECT matricula, latitud, longitud, instante FROM (
                SELECT matricula, latitud, longitud, instante,
                       row_number() OVER (PARTITION BY matricula ORDER BY instante DESC) AS orden
                FROM posicion_tracking
                WHERE instante >= ?1
            ) ultimas
            WHERE orden <= ?2
            ORDER BY matricula, instante
            """;
    private static final String URL_BALANZAS_POR_DEFECTO =
            System.getenv().getOrDefault("NODO_BALANZAS_URL", "http://balanzas:8080/balanzas/api/pesadas");

    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @EJB
    PesajeBean balanzas;

    @EJB
    NodoPerifericoDAO nodos;

    @Override
    public MetricasDTO metricas() {
        MetricasDTO m = new MetricasDTO();
        OffsetDateTime haceCincoMinutos = OffsetDateTime.now().minusMinutes(5);
        m.setPosicionesTotales(posicionesTotales());
        // "Reportando" = con alguna posicion en los ultimos 5 minutos, no en toda la historia
        m.setVehiculosReportando(em.createQuery(
                        "select count(distinct p.matricula) from PosicionTracking p where p.recibidoEn > :desde", Long.class)
                .setParameter("desde", haceCincoMinutos)
                .getSingleResult());
        m.setPosicionesUltimosCincoMinutos(em.createQuery(
                        "select count(p) from PosicionTracking p where p.recibidoEn > :desde", Long.class)
                .setParameter("desde", haceCincoMinutos)
                .getSingleResult());
        OffsetDateTime ultima = em.createQuery(
                        "select max(p.recibidoEn) from PosicionTracking p", OffsetDateTime.class)
                .getSingleResult();
        m.setUltimaRecepcion(ultima == null ? "sin datos" : RELOJ.format(ultima));
        return m;
    }

    @Override
    public List<EstadoNodoDTO> estadoNodos() {
        List<NodoPeriferico> registrados = nodos.listar();
        if (registrados.isEmpty()) {
            return List.of(estadoBalanzas(null, "Balanzas peajes", URL_BALANZAS_POR_DEFECTO),
                    estadoTracking(null, "Tracking flota"));
        }
        return registrados.stream().map(this::estado).toList();
    }

    private EstadoNodoDTO estado(NodoPeriferico n) {
        if (!n.isActivo()) {
            EstadoNodoDTO nodo = new EstadoNodoDTO(n.getNombre(), n.getTipo().name(), n.getUrl());
            nodo.setId(n.getId());
            nodo.setActivo(false);
            nodo.setDetalle("Desactivado por un administrador.");
            return nodo;
        }
        return n.getTipo() == TipoNodo.BALANZAS
                ? estadoBalanzas(n.getId(), n.getNombre(), n.getUrl())
                : estadoTracking(n.getId(), n.getNombre());
    }

    /** Exacto con pocas filas; con muchas, la estimacion de Postgres (se actualiza con el autovacuum). */
    private long posicionesTotales() {
        Number estimado = (Number) em.createNativeQuery(
                        "SELECT reltuples FROM pg_class WHERE oid = 'posicion_tracking'::regclass")
                .getSingleResult();
        long aproximado = estimado == null ? -1 : estimado.longValue();
        return aproximado > TOTAL_EXACTO_HASTA ? aproximado : contar("select count(p) from PosicionTracking p");
    }

    @Override
    public List<RecorridoDTO> mapaFlota(int puntosPorVehiculo) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SQL_MAPA)
                .setParameter(1, OffsetDateTime.now().minusHours(VENTANA_MAPA_HORAS))
                .setParameter(2, Math.max(1, puntosPorVehiculo))
                .getResultList();
        return armarMapa(filas);
    }

    /** Filas ordenadas por matricula y de la mas antigua a la mas reciente: la ultima es la actual. */
    static List<RecorridoDTO> armarMapa(List<Object[]> filas) {
        List<RecorridoDTO> flota = new ArrayList<>();
        RecorridoDTO r = null;
        for (Object[] f : filas) {
            String matricula = (String) f[0];
            if (r == null || !r.getMatricula().equals(matricula)) {
                r = new RecorridoDTO();
                r.setMatricula(matricula);
                flota.add(r);
            }
            double latitud = ((Number) f[1]).doubleValue();
            double longitud = ((Number) f[2]).doubleValue();
            r.getPuntos().add(new double[]{latitud, longitud});
            r.setLatitud(latitud);
            r.setLongitud(longitud);
            r.setInstante(RELOJ.format(instante(f[3])));
        }
        return flota;
    }

    private static OffsetDateTime instante(Object valor) {
        if (valor instanceof OffsetDateTime o) {
            return o;
        }
        if (valor instanceof Instant i) {
            return i.atOffset(ZoneOffset.UTC);
        }
        return ((Timestamp) valor).toInstant().atOffset(ZoneOffset.UTC);
    }

    /** Se verifica con una consulta real al nodo, usando el mismo camino que el caso de uso. */
    private EstadoNodoDTO estadoBalanzas(Long id, String nombre, String url) {
        EstadoNodoDTO nodo = new EstadoNodoDTO(nombre, "BALANZAS", url);
        nodo.setId(id);
        ResultadoPesajeDTO r = balanzas.consultarPesadas("", null);
        nodo.setEnLinea(r.isNodoDisponible());
        nodo.setLatenciaMs(r.getLatenciaMs());
        nodo.setDetalle((r.isNodoDisponible() ? "Responde a la consulta de pesadas." : r.getMensaje())
                + " Circuito " + nombre(balanzas.estadoCircuito()) + ".");
        return nodo;
    }

    private static String nombre(CircuitBreaker.Estado estado) {
        return switch (estado) {
            case CERRADO -> "cerrado";
            case ABIERTO -> "abierto";
            case SEMI_ABIERTO -> "semiabierto (probando si el nodo volvio)";
        };
    }

    /** No se puede consultar: el nodo Tracking empuja. Se deduce de cuando llego la ultima posicion. */
    private EstadoNodoDTO estadoTracking(Long id, String nombre) {
        EstadoNodoDTO nodo = new EstadoNodoDTO(nombre, "TRACKING", "ingesta en /carga/api/tracking/posiciones");
        nodo.setId(id);
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
