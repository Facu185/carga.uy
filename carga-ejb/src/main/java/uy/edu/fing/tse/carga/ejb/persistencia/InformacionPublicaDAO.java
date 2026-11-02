package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lecturas del frontoffice publico (ADR-0021). Solo lee tablas compartidas de P2; las agregaciones
 * devuelven, por celda, los ids de las empresas que la componen para poder aplicar el umbral en el
 * servidor. Esos ids nunca salen del central.
 */
@Stateless
public class InformacionPublicaDAO {

    /** Guias por mes, por tipo de carga y por rubro, en una sola lectura (misma instantanea). */
    static final String SQL_GUIAS = """
            WITH g AS (
                SELECT g.empresa_id, g.peso_kg,
                       to_char(g.inicio AT TIME ZONE 'America/Montevideo', 'YYYY-MM') AS mes,
                       g.tipo_carga_id, t.nombre AS tipo, g.rubro_id, r.nombre AS rubro
                FROM guia g
                LEFT JOIN tipo_carga t ON t.id = g.tipo_carga_id
                LEFT JOIN rubro r ON r.id = g.rubro_id
                WHERE g.inicio >= ?1 AND g.inicio < ?2
            )
            SELECT 'MES' AS grupo, mes AS clave, mes AS etiqueta, COUNT(*) AS guias,
                   string_agg(DISTINCT CAST(empresa_id AS text), ',') AS empresas,
                   COALESCE(SUM(peso_kg), 0) AS peso_kg
                FROM g GROUP BY mes
            UNION ALL
            SELECT 'TIPO', COALESCE(CAST(tipo_carga_id AS text), 'SIN_DATO'), COALESCE(tipo, 'Sin tipo declarado'),
                   COUNT(*), string_agg(DISTINCT CAST(empresa_id AS text), ','), COALESCE(SUM(peso_kg), 0)
                FROM g GROUP BY tipo_carga_id, tipo
            UNION ALL
            SELECT 'RUBRO', COALESCE(CAST(rubro_id AS text), 'SIN_DATO'), COALESCE(rubro, 'Sin rubro declarado'),
                   COUNT(*), string_agg(DISTINCT CAST(empresa_id AS text), ','), COALESCE(SUM(peso_kg), 0)
                FROM g GROUP BY rubro_id, rubro
            """;

    /**
     * Empresas y vehiculos habilitados. Un vehiculo esta "al dia" con el mismo criterio que la
     * consulta del funcionario: permiso con numero y periodo que contiene el dia, e ITV no vencida.
     */
    static final String SQL_FLOTA = """
            WITH v AS (
                SELECT v.empresa_id,
                       (v.numero_permiso IS NOT NULL AND btrim(v.numero_permiso) <> ''
                        AND v.permiso_desde <= ?1 AND v.permiso_hasta >= ?1
                        AND v.itv_vencimiento >= ?1) AS al_dia
                FROM vehiculo v JOIN empresa e ON e.id = v.empresa_id
                WHERE v.habilitado AND e.activa
            )
            SELECT 'EMPRESAS' AS grupo, COUNT(*) AS cantidad,
                   string_agg(CAST(id AS text), ',') AS empresas
                FROM empresa WHERE activa
            UNION ALL
            SELECT 'HABILITADOS', COUNT(*), string_agg(DISTINCT CAST(empresa_id AS text), ',') FROM v
            UNION ALL
            SELECT 'AL_DIA', COUNT(*), string_agg(DISTINCT CAST(empresa_id AS text), ',') FROM v WHERE al_dia
            UNION ALL
            SELECT 'NO_AL_DIA', COUNT(*), string_agg(DISTINCT CAST(empresa_id AS text), ',') FROM v WHERE NOT al_dia
            """;

    /** Fila agregada: grupo, clave, etiqueta, cantidad, empresas que la componen y peso declarado. */
    public record Fila(String grupo, String clave, String etiqueta, long cantidad, Set<Long> empresas, long pesoKg) {
    }

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    /** Matricula comparada sin espacios ni guiones y sin importar mayusculas. */
    public Optional<Vehiculo> vehiculo(String matriculaNormalizada) {
        return em.createQuery("select v from Vehiculo v join fetch v.empresa"
                        + " where upper(replace(replace(v.matricula, ' ', ''), '-', '')) = :m", Vehiculo.class)
                .setParameter("m", matriculaNormalizada)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    public Optional<Empresa> empresa(long numeroEmpresa) {
        return em.createQuery("select e from Empresa e where e.numeroEmpresa = :n", Empresa.class)
                .setParameter("n", numeroEmpresa)
                .getResultStream()
                .findFirst();
    }

    public List<Fila> guias(Instant desde, Instant hasta) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SQL_GUIAS)
                .setParameter(1, desde.atOffset(ZoneOffset.UTC))
                .setParameter(2, hasta.atOffset(ZoneOffset.UTC))
                .getResultList();
        return filas.stream()
                .map(f -> new Fila((String) f[0], (String) f[1], (String) f[2], ((Number) f[3]).longValue(),
                        ids((String) f[4]), ((Number) f[5]).longValue()))
                .toList();
    }

    public List<Fila> flota(LocalDate dia) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SQL_FLOTA).setParameter(1, dia).getResultList();
        return filas.stream()
                .map(f -> new Fila((String) f[0], "", "", ((Number) f[1]).longValue(), ids((String) f[2]), 0))
                .toList();
    }

    static Set<Long> ids(String lista) {
        return lista == null || lista.isBlank() ? Set.of()
                : Arrays.stream(lista.split(",")).map(Long::valueOf).collect(Collectors.toUnmodifiableSet());
    }
}
