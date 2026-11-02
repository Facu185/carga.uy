package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.CasoFiscalizacion;
import uy.edu.fing.tse.carga.ejb.modelo.Descargo;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoCaso;
import uy.edu.fing.tse.carga.ejb.modelo.ParametroRegla;
import uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Acceso a casos, descargos y parametros de fiscalizacion (frente P5, ADR-0022). */
@Stateless
public class FiscalizacionDAO {

    /**
     * Recorrido de un viaje frente a la linea origen-destino de su guia: distancia en linea recta,
     * posiciones del periodo [inicio, fin), cuantas quedan fuera del corredor y el alejamiento maximo.
     * Todo en metros sobre la Tierra (geography). Sin coordenadas de origen o destino no devuelve filas.
     */
    static final String SQL_DESVIO = """
            WITH g AS (
                SELECT v.matricula, g.inicio, g.fin,
                       geography(ST_MakeLine(g.origen_geom, g.destino_geom)) AS linea,
                       ST_Distance(geography(g.origen_geom), geography(g.destino_geom)) AS recta
                FROM guia g JOIN vehiculo v ON v.id = g.vehiculo_id
                WHERE g.id = ?1 AND g.origen_geom IS NOT NULL AND g.destino_geom IS NOT NULL
            ), d AS (
                SELECT g.recta, ST_Distance(geography(p.geom), g.linea) AS distancia
                FROM g JOIN posicion_tracking p
                  ON p.matricula = g.matricula AND p.instante >= g.inicio AND p.instante < g.fin
            )
            SELECT (SELECT recta FROM g) AS recta,
                   COUNT(d.distancia) AS posiciones,
                   COUNT(*) FILTER (WHERE d.distancia > greatest(?2, d.recta * ?3 / 100.0)) AS fuera,
                   COALESCE(MAX(d.distancia), 0) AS maxima
            FROM d
            """;

    /**
     * Vehiculos registrados con posiciones recibidas desde un instante, y si ese tiempo lo cubre un viaje
     * real: una guia EN_CURSO o FINALIZADA (el chofer registro el inicio). Una guia solo ASIGNADA no
     * cuenta: no hay viaje en curso. FINALIZADA cuenta porque la agenda puede revisar posiciones de un
     * viaje que termino despues. Agrega en la base: la agenda no recorre posiciones una por una.
     */
    static final String SQL_ACTIVIDAD = """
            SELECT v.id, v.matricula, MIN(p.instante) AS desde, MAX(p.instante) AS hasta, COUNT(*) AS posiciones,
                   EXISTS (SELECT 1 FROM guia g WHERE g.vehiculo_id = v.id AND g.estado IN ('EN_CURSO', 'FINALIZADA')
                           AND g.inicio <= MAX(p.instante) AND g.fin > MIN(p.instante)) AS en_curso
            FROM posicion_tracking p JOIN vehiculo v ON v.matricula = p.matricula
            WHERE p.recibido_en >= ?1
            GROUP BY v.id, v.matricula
            """;

    /** Actividad de tracking agregada de un vehiculo. */
    public record Actividad(Long vehiculoId, String matricula, OffsetDateTime desde, OffsetDateTime hasta,
                            long posiciones, boolean conViaje) {
    }

    /** Resultado de la consulta de desvio de un viaje. */
    public record Recorrido(double distanciaRectaM, int posiciones, int fueraDelCorredor, double maximaDistanciaM) {
    }

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    /**
     * Crea el caso solo si el hecho no tenia uno. La deduplicacion la hace la base (hecho UNIQUE con
     * ON CONFLICT): dos consumidores que procesan el mismo hecho a la vez no generan dos casos.
     *
     * @return el id del caso nuevo, o vacio si el hecho ya tenia caso
     */
    public Optional<Long> crearSiEsNuevo(TipoInfraccion tipo, String hecho, Long empresaId, Long vehiculoId,
                                         String matricula, Long guiaId, String evidencia) {
        int insertadas = em.createNativeQuery("INSERT INTO caso_fiscalizacion (tipo, estado, hecho, empresa_id,"
                        + " vehiculo_id, matricula, guia_id, evidencia, detectada_en)"
                        + " VALUES (?1, 'DETECTADA', ?2, ?3, ?4, ?5, ?6, ?7, now()) ON CONFLICT (hecho) DO NOTHING")
                .setParameter(1, tipo.name())
                .setParameter(2, hecho)
                .setParameter(3, empresaId)
                .setParameter(4, vehiculoId)
                .setParameter(5, matricula)
                .setParameter(6, guiaId)
                .setParameter(7, evidencia)
                .executeUpdate();
        if (insertadas == 0) {
            return Optional.empty();
        }
        return em.createQuery("select c.id from CasoFiscalizacion c where c.hecho = :h", Long.class)
                .setParameter("h", hecho).getResultStream().findFirst();
    }

    public void notificar(Long empresaId, Long casoId, String canal, String mensaje) {
        em.createNativeQuery("INSERT INTO notificacion_empresa (empresa_id, caso_id, canal, mensaje) VALUES (?1, ?2, ?3, ?4)")
                .setParameter(1, empresaId).setParameter(2, casoId).setParameter(3, canal).setParameter(4, mensaje)
                .executeUpdate();
    }

    public Optional<CasoFiscalizacion> caso(Long id) {
        return em.createQuery("select c from CasoFiscalizacion c left join fetch c.empresa left join fetch c.vehiculo"
                        + " where c.id = :id", CasoFiscalizacion.class)
                .setParameter("id", id).getResultStream().findFirst();
    }

    /** Casos filtrados (cualquier filtro puede ser null), del mas reciente al mas antiguo. */
    public List<CasoFiscalizacion> buscar(Long empresaId, EstadoCaso estado, TipoInfraccion tipo, int desde, int cantidad) {
        return filtrar("select c from CasoFiscalizacion c left join fetch c.empresa", empresaId, estado, tipo,
                " order by c.detectadaEn desc, c.id desc", CasoFiscalizacion.class)
                .setFirstResult(desde).setMaxResults(cantidad).getResultList();
    }

    public long contar(Long empresaId, EstadoCaso estado, TipoInfraccion tipo) {
        return filtrar("select count(c) from CasoFiscalizacion c", empresaId, estado, tipo, "", Long.class).getSingleResult();
    }

    private <T> jakarta.persistence.TypedQuery<T> filtrar(String base, Long empresaId, EstadoCaso estado, TipoInfraccion tipo,
                                                          String orden, Class<T> clase) {
        String where = " where 1 = 1" + (empresaId == null ? "" : " and c.empresa.id = :empresa")
                + (estado == null ? "" : " and c.estado = :estado") + (tipo == null ? "" : " and c.tipo = :tipo");
        var q = em.createQuery(base + where + orden, clase);
        if (empresaId != null) q.setParameter("empresa", empresaId);
        if (estado != null) q.setParameter("estado", estado);
        if (tipo != null) q.setParameter("tipo", tipo);
        return q;
    }

    public Optional<Descargo> descargo(Long casoId) {
        return em.createQuery("select d from Descargo d left join fetch d.usuario where d.casoId = :c", Descargo.class)
                .setParameter("c", casoId).getResultStream().findFirst();
    }

    public Map<Long, Descargo> descargos(List<Long> casos) {
        if (casos.isEmpty()) {
            return Map.of();
        }
        return em.createQuery("select d from Descargo d left join fetch d.usuario where d.casoId in :c", Descargo.class)
                .setParameter("c", casos).getResultStream()
                .collect(Collectors.toMap(Descargo::getCasoId, Function.identity()));
    }

    public void guardar(Descargo descargo) {
        em.persist(descargo);
    }

    public Optional<Vehiculo> vehiculo(String matricula) {
        return em.createQuery("select v from Vehiculo v join fetch v.empresa where v.matricula = :m", Vehiculo.class)
                .setParameter("m", matricula).getResultStream().findFirst();
    }

    public Optional<String> estadoGuia(Long guiaId) {
        return em.createQuery("select g.estado from Guia g where g.id = :id", String.class)
                .setParameter("id", guiaId).getResultStream().findFirst();
    }

    /** Vehiculo y empresa de una guia, para atribuir un desvio. */
    public Optional<Vehiculo> vehiculoDeGuia(Long guiaId) {
        return em.createQuery("select v from Guia g join g.vehiculo v join fetch v.empresa where g.id = :id", Vehiculo.class)
                .setParameter("id", guiaId).getResultStream().findFirst();
    }

    public Optional<Recorrido> recorrido(Long guiaId, double toleranciaM, double toleranciaRelativaPct) {
        Object[] f = (Object[]) em.createNativeQuery(SQL_DESVIO)
                .setParameter(1, guiaId).setParameter(2, toleranciaM).setParameter(3, toleranciaRelativaPct)
                .getSingleResult();
        if (f[0] == null) {
            return Optional.empty();
        }
        return Optional.of(new Recorrido(((Number) f[0]).doubleValue(), ((Number) f[1]).intValue(),
                ((Number) f[2]).intValue(), ((Number) f[3]).doubleValue()));
    }

    public List<Actividad> actividadDesde(Instant desde) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SQL_ACTIVIDAD)
                .setParameter(1, desde.atOffset(ZoneOffset.UTC)).getResultList();
        return filas.stream().map(f -> new Actividad(((Number) f[0]).longValue(), (String) f[1], instante(f[2]),
                instante(f[3]), ((Number) f[4]).longValue(), (Boolean) f[5])).toList();
    }

    /** Casos detectados en [desde, hasta), por tipo y estado: tipo, estado, cantidad. */
    public List<Object[]> conteoPorTipoYEstado(Instant desde, Instant hasta) {
        return em.createQuery("select c.tipo, c.estado, count(c) from CasoFiscalizacion c"
                        + " where c.detectadaEn >= :desde and c.detectadaEn < :hasta group by c.tipo, c.estado", Object[].class)
                .setParameter("desde", desde).setParameter("hasta", hasta).getResultList();
    }

    public List<ParametroRegla> parametros() {
        return em.createQuery("select p from ParametroRegla p order by p.clave", ParametroRegla.class).getResultList();
    }

    public Optional<ParametroRegla> parametro(String clave) {
        return em.createQuery("select p from ParametroRegla p where p.clave = :c", ParametroRegla.class)
                .setParameter("c", clave).getResultStream().findFirst();
    }

    private static OffsetDateTime instante(Object valor) {
        if (valor instanceof OffsetDateTime o) {
            return o;
        }
        if (valor instanceof Instant i) {
            return i.atOffset(ZoneOffset.UTC);
        }
        return ((java.sql.Timestamp) valor).toInstant().atOffset(ZoneOffset.UTC);
    }
}
