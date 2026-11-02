package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.dto.reporte.ConteoActividadDTO;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

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

    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    public ReporteActividadDTO consultar(Instant desde, Instant hasta) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SQL)
                .setParameter(1, desde.atOffset(ZoneOffset.UTC))
                .setParameter(2, hasta.atOffset(ZoneOffset.UTC))
                .getResultList();
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
}
