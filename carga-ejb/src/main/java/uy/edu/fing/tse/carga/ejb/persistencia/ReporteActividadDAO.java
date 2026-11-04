package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.dto.reporte.ConteoActividadDTO;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
import uy.edu.fing.tse.carga.api.dto.guia.TipoCargaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.TipoCarga;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Agregacion PostgreSQL en una sentencia: todas las distribuciones comparten snapshot. */
@Stateless
public class ReporteActividadDAO {
    static final String SQL = """
            WITH filtradas AS (
                SELECT g.empresa_id, g.estado, g.rubro_id, r.nombre AS rubro_nombre,
                       CAST(g.creada_en AT TIME ZONE 'America/Montevideo' AS date) AS dia
                FROM guia g LEFT JOIN rubro r ON r.id = g.rubro_id
                WHERE g.creada_en >= ?1 AND g.creada_en < ?2
            )
            SELECT 'TOTAL' AS tipo, '' AS clave, '' AS etiqueta,
                   COUNT(*) AS cantidad, COUNT(DISTINCT empresa_id) AS empresas FROM filtradas
            UNION ALL
            SELECT 'DIA', to_char(dia, 'YYYY-MM-DD'), to_char(dia, 'YYYY-MM-DD'), COUNT(*), CAST(0 AS bigint)
                FROM filtradas GROUP BY dia
            UNION ALL
            SELECT 'ESTADO', estado, estado, COUNT(*), CAST(0 AS bigint)
                FROM filtradas GROUP BY estado
            UNION ALL
            SELECT 'RUBRO', COALESCE(CAST(rubro_id AS text), 'SIN_RUBRO'),
                   COALESCE(rubro_nombre, 'Sin rubro'), COUNT(*), CAST(0 AS bigint)
                FROM filtradas GROUP BY rubro_id, rubro_nombre
            """;
    // Solo cambia el predicado de la CTE; todas las distribuciones usan el mismo subconjunto.
    static final String SQL_POR_TIPO = SQL.replace("g.creada_en < ?2", "g.creada_en < ?2 AND g.tipo_carga_id = ?3");
    // FIN es terminal. Reducir antes del rango evita multiplicar una guia por filas repetidas.
    // Se conservan las mismas distribuciones y la sentencia original del reporte por creacion.
    static final String SQL_FINALIZADOS = SQL.replace("WITH filtradas AS (", """
            WITH finalizaciones AS (
                SELECT guia_id, MIN(instante) AS fin_real
                FROM evento_viaje WHERE tipo = 'FIN' GROUP BY guia_id
            ), filtradas AS (
            """)
            .replace("FROM guia g LEFT JOIN rubro r", "FROM guia g JOIN finalizaciones f ON f.guia_id = g.id LEFT JOIN rubro r")
            .replace("g.creada_en", "f.fin_real");
    static final String SQL_FINALIZADOS_POR_TIPO = SQL_FINALIZADOS.replace("f.fin_real < ?2", "f.fin_real < ?2 AND g.tipo_carga_id = ?3");

    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    public ReporteActividadDTO consultar(Instant desde, Instant hasta) {
        return consultar(desde, hasta, null);
    }

    public ReporteActividadDTO consultar(Instant desde, Instant hasta, Long tipoCargaId) {
        return consultarSql(tipoCargaId == null ? SQL : SQL_POR_TIPO, desde, hasta, tipoCargaId);
    }

    public ReporteActividadDTO consultarFinalizados(Instant desde, Instant hasta, Long tipoCargaId) {
        return consultarSql(tipoCargaId == null ? SQL_FINALIZADOS : SQL_FINALIZADOS_POR_TIPO, desde, hasta, tipoCargaId);
    }

    private ReporteActividadDTO consultarSql(String sql, Instant desde, Instant hasta, Long tipoCargaId) {
        var consulta = em.createNativeQuery(sql)
                .setParameter(1, desde.atOffset(ZoneOffset.UTC))
                .setParameter(2, hasta.atOffset(ZoneOffset.UTC));
        if (tipoCargaId != null) consulta.setParameter(3, tipoCargaId);
        @SuppressWarnings("unchecked")
        List<Object[]> filas = consulta.getResultList();
        var resultado = new ReporteActividadDTO();
        for (Object[] fila : filas) {
            long cantidad = ((Number) fila[3]).longValue();
            var grupo = new ConteoActividadDTO((String) fila[1], (String) fila[2], cantidad);
            switch ((String) fila[0]) {
                case "TOTAL" -> {
                    resultado.setTotalGuias(cantidad);
                    resultado.setEmpresasConGuias(((Number) fila[4]).longValue());
                }
                case "DIA" -> resultado.getPorDia().add(grupo);
                case "ESTADO" -> resultado.getPorEstado().add(grupo);
                case "RUBRO" -> resultado.getPorRubro().add(grupo);
                default -> throw new IllegalStateException("Grupo de reporte no reconocido.");
            }
        }
        resultado.getPorRubro().sort(Comparator.comparingLong(ConteoActividadDTO::getCantidad).reversed()
                .thenComparing(ConteoActividadDTO::getClave));
        return resultado;
    }

    public Optional<TipoCargaDTO> tipoCarga(Long id) {
        return Optional.ofNullable(em.find(TipoCarga.class, id)).map(ReporteActividadDAO::tipoDTO);
    }

    public List<TipoCargaDTO> tiposCarga() {
        return em.createQuery("select t from TipoCarga t order by t.nombre, t.id", TipoCarga.class)
                .getResultList().stream().map(ReporteActividadDAO::tipoDTO).toList();
    }

    private static TipoCargaDTO tipoDTO(TipoCarga tipo) {
        var dto = new TipoCargaDTO(); dto.setId(tipo.getId()); dto.setNombre(tipo.getNombre()); return dto;
    }
}
