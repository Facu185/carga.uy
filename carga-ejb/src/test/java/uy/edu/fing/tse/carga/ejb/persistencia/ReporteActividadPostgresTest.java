package uy.edu.fing.tse.carga.ejb.persistencia;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** SQL real con fixture minimo temporal, NO mapeo Hibernate, migraciones ni seguridad EJB.
 * Escrita antes del DAO; pendiente de ejecucion por prohibicion de builds. */
@Tag("integracion")
class ReporteActividadPostgresTest {
    private static GenericContainer<?> postgres;

    @BeforeAll static void preparar() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "requiere Docker");
        postgres = new GenericContainer<>(DockerImageName.parse("postgis/postgis:17-3.5"))
                .withCreateContainerCmdModifier(c -> c.withPlatform("linux/amd64"))
                .withEnv("POSTGRES_DB", "reporte").withEnv("POSTGRES_USER", "reporte")
                .withEnv("POSTGRES_PASSWORD", "clave-sintetica").withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        postgres.start();
    }

    @AfterAll static void cerrar() { if (postgres != null) postgres.stop(); }

    @Test void medianochesLocalesConteoDistintoYGruposCompletosNoDependenDeZonaSql() throws Exception {
        try (var c = conexion(); var sql = c.createStatement()) {
            fixture(c);
            sql.execute("INSERT INTO rubro VALUES (1, 'Sin rubro'), (2, 'Alimentos')");
            sql.execute("""
                    INSERT INTO guia (empresa_id, rubro_id, estado, creada_en, inicio) VALUES
                    (1, NULL, 'CREADA', '2026-11-01T02:59:59Z', '2026-11-01T12:00Z'),
                    (1, NULL, 'ASIGNADA', '2026-11-01T03:00:00Z', '2027-01-01T12:00Z'),
                    (1, 1, 'CREADA', '2026-11-02T02:59:59Z', '2027-01-01T12:00Z'),
                    (2, 2, 'FINALIZADA', '2026-11-02T03:00:00Z', '2027-01-01T12:00Z'),
                    (2, NULL, 'EN_CURSO', '2026-11-03T02:59:59Z', '2027-01-01T12:00Z'),
                    (3, NULL, 'ASIGNADA', '2026-11-03T03:00:00Z', '2026-11-02T12:00Z')
                    """);
            var r = consultar(c, "2026-11-01T03:00Z", "2026-11-03T03:00Z");
            assertEquals(4L, r.get("TOTAL:")[0]); assertEquals(2L, r.get("TOTAL:")[1]);
            assertEquals(2L, r.get("DIA:2026-11-01")[0]); assertEquals(2L, r.get("DIA:2026-11-02")[0]);
            assertEquals(2L, r.get("RUBRO:SIN_RUBRO")[0]); assertEquals(1L, r.get("RUBRO:1")[0]);
            assertEquals(1L, r.get("RUBRO:2")[0]);
            for (String estado : new String[]{"CREADA", "ASIGNADA", "EN_CURSO", "FINALIZADA"})
                assertEquals(1L, r.get("ESTADO:" + estado)[0]);
            assertEquals(10, r.size());
        }
    }

    @Test void vacioNoPierdeFilaDeTotalesNiInventaGrupos() throws Exception {
        try (var c = conexion()) {
            fixture(c);
            var r = consultar(c, "2026-11-01T03:00Z", "2026-11-02T03:00Z");
            assertEquals(1, r.size()); assertArrayEquals(new long[]{0L, 0L}, r.get("TOTAL:"));
        }
    }

    @Test void cargaFiltraTodosLosAgregadosYTodosIncluyeSinTipo() throws Exception {
        try (var c = conexion(); var sql = c.createStatement()) {
            fixture(c);
            sql.execute("INSERT INTO rubro VALUES (1, 'Alimentos')");
            sql.execute("""
                    INSERT INTO guia (empresa_id, rubro_id, estado, creada_en, tipo_carga_id) VALUES
                    (1, NULL, 'ASIGNADA', '2026-11-01T03:00Z', 7),
                    (1, 1, 'FINALIZADA', '2026-11-02T02:59:59Z', 7),
                    (2, 1, 'EN_CURSO', '2026-11-01T12:00Z', 8),
                    (3, NULL, 'CREADA', '2026-11-01T12:00Z', NULL),
                    (4, 1, 'ASIGNADA', '2026-11-02T03:00Z', 7)
                    """);
            var r = consultar(c, "2026-11-01T03:00Z", "2026-11-02T03:00Z", 7L);
            assertArrayEquals(new long[]{2L, 1L}, r.get("TOTAL:"));
            assertEquals(2L, r.get("DIA:2026-11-01")[0]);
            assertEquals(1L, r.get("ESTADO:ASIGNADA")[0]);
            assertEquals(1L, r.get("ESTADO:FINALIZADA")[0]);
            assertEquals(1L, r.get("RUBRO:SIN_RUBRO")[0]); assertEquals(1L, r.get("RUBRO:1")[0]);
            assertEquals(6, r.size());
            var todos = consultar(c, "2026-11-01T03:00Z", "2026-11-02T03:00Z");
            assertArrayEquals(new long[]{4L, 3L}, todos.get("TOTAL:"));
            var vacio = consultar(c, "2026-11-01T03:00Z", "2026-11-02T03:00Z", 9L);
            assertEquals(1, vacio.size()); assertArrayEquals(new long[]{0L, 0L}, vacio.get("TOTAL:"));
        }
    }

    @Test void finalizadosUsaFinRealAunqueCreacionYRecepcionEstenFueraDelPeriodo() throws Exception {
        try (var c = conexion(); var sql = c.createStatement()) {
            fixture(c);
            sql.execute("INSERT INTO rubro VALUES (1, 'Alimentos')");
            sql.execute("""
                    INSERT INTO guia (empresa_id, rubro_id, estado, creada_en, inicio, tipo_carga_id) VALUES
                    (1, 1, 'FINALIZADA', '2026-10-01T12:00Z', '2026-12-01T12:00Z', 7),
                    (1, 1, 'FINALIZADA', '2026-11-04T12:00Z', '2026-11-04T12:00Z', 7),
                    (2, 1, 'FINALIZADA', '2026-11-04T12:00Z', '2026-11-04T12:00Z', 7),
                    (2, 1, 'FINALIZADA', '2026-11-04T12:00Z', '2026-11-04T12:00Z', 7),
                    (2, NULL, 'FINALIZADA', '2026-10-01T12:00Z', '2026-12-01T12:00Z', 8),
                    (2, 1, 'EN_CURSO', '2026-11-04T12:00Z', '2026-11-04T12:00Z', 7),
                    (1, NULL, 'FINALIZADA', '2026-10-01T12:00Z', '2026-12-01T12:00Z', NULL)
                    """);
            sql.execute("""
                    INSERT INTO evento_viaje (guia_id, tipo, instante, recibido_en) VALUES
                    (1, 'FIN', '2026-11-04T03:00Z', '2026-11-09T12:00Z'),
                    (1, 'INICIO', '2026-11-04T02:00Z', '2026-11-09T12:00Z'),
                    (3, 'FIN', '2026-11-04T02:59:59Z', '2026-11-09T12:00Z'),
                    (4, 'FIN', '2026-11-06T03:00Z', '2026-11-09T12:00Z'),
                    (5, 'FIN', '2026-11-06T02:59:59Z', '2026-11-09T12:00Z'),
                    (6, 'INICIO', '2026-11-04T12:00Z', '2026-11-09T12:00Z'),
                    (7, 'FIN', '2026-11-05T02:59:59Z', '2026-11-09T12:00Z'),
                    (7, 'FIN', '2026-11-05T02:59:59Z', '2026-11-09T12:00Z')
                    """);
            var r = consultarFinalizados(c, "2026-11-04T03:00Z", "2026-11-06T03:00Z", null);
            assertArrayEquals(new long[]{3L, 2L}, r.get("TOTAL:"));
            assertEquals(2L, r.get("DIA:2026-11-04")[0]); assertEquals(1L, r.get("DIA:2026-11-05")[0]);
            assertEquals(3L, r.get("ESTADO:FINALIZADA")[0]);
            assertEquals(1L, r.get("RUBRO:1")[0]); assertEquals(2L, r.get("RUBRO:SIN_RUBRO")[0]);
            assertEquals(6, r.size());
            var tipo = consultarFinalizados(c, "2026-11-04T03:00Z", "2026-11-06T03:00Z", 7L);
            assertArrayEquals(new long[]{1L, 1L}, tipo.get("TOTAL:"));
            assertEquals(1L, tipo.get("DIA:2026-11-04")[0]); assertEquals(1L, tipo.get("RUBRO:1")[0]);
            var vacio = consultarFinalizados(c, "2026-11-04T03:00Z", "2026-11-06T03:00Z", 99L);
            assertEquals(1, vacio.size()); assertArrayEquals(new long[]{0L, 0L}, vacio.get("TOTAL:"));
        }
    }

    @Test void primerFinPorGuiaSeReduceAntesDeFiltrarElPeriodo() throws Exception {
        try (var c = conexion(); var sql = c.createStatement()) {
            fixture(c);
            // Fixture defensivo: filas repetidas no representan nuevas finalizaciones ni reapertura.
            sql.execute("INSERT INTO guia (empresa_id, estado, creada_en) VALUES (1, 'FINALIZADA', '2026-10-01T12:00Z')");
            sql.execute("""
                    INSERT INTO evento_viaje (guia_id, tipo, instante) VALUES
                    (1, 'FIN', '2026-11-03T12:00Z'), (1, 'FIN', '2026-11-04T12:00Z')
                    """);
            var r = consultarFinalizados(c, "2026-11-04T03:00Z", "2026-11-05T03:00Z", null);
            assertEquals(1, r.size()); assertArrayEquals(new long[]{0L, 0L}, r.get("TOTAL:"));
            assertArrayEquals(new long[]{1L, 1L}, consultarFinalizados(c, "2026-11-03T03:00Z", "2026-11-04T03:00Z", null).get("TOTAL:"));
        }
    }

    private static Connection conexion() throws Exception {
        return DriverManager.getConnection("jdbc:postgresql://" + postgres.getHost() + ":"
                + postgres.getMappedPort(5432) + "/reporte", "reporte", "clave-sintetica");
    }

    private static void fixture(Connection c) throws Exception {
        try (var s = c.createStatement()) {
            s.execute("SET TIME ZONE 'Asia/Tokyo'");
            // pgJDBC exige formato ISO; DMY y la zona ajena al negocio siguen siendo explicitos.
            s.execute("SET DateStyle TO 'ISO, DMY'");
            s.execute("CREATE TEMP TABLE rubro (id bigint, nombre text)");
            s.execute("CREATE TEMP TABLE guia (empresa_id bigint, rubro_id bigint, estado text, creada_en timestamptz, inicio timestamptz, tipo_carga_id bigint, id bigint GENERATED ALWAYS AS IDENTITY)");
            s.execute("CREATE TEMP TABLE evento_viaje (guia_id bigint, tipo text, instante timestamptz, recibido_en timestamptz)");
        }
    }

    private static Map<String, long[]> consultar(Connection c, String desde, String hasta) throws Exception {
        return consultar(c, desde, hasta, null);
    }

    private static Map<String, long[]> consultar(Connection c, String desde, String hasta, Long tipoCargaId) throws Exception {
        // Adaptar exclusivamente placeholders JPA a JDBC: es la misma sentencia de produccion.
        String consulta = tipoCargaId == null ? ReporteActividadDAO.SQL : ReporteActividadDAO.SQL_POR_TIPO;
        return consultarSql(c, desde, hasta, tipoCargaId, consulta);
    }

    private static Map<String, long[]> consultarFinalizados(Connection c, String desde, String hasta, Long tipoCargaId) throws Exception {
        String consulta = tipoCargaId == null ? ReporteActividadDAO.SQL_FINALIZADOS : ReporteActividadDAO.SQL_FINALIZADOS_POR_TIPO;
        return consultarSql(c, desde, hasta, tipoCargaId, consulta);
    }

    private static Map<String, long[]> consultarSql(Connection c, String desde, String hasta, Long tipoCargaId, String consulta) throws Exception {
        try (var s = c.prepareStatement(consulta.replace("?1", "?").replace("?2", "?").replace("?3", "?"))) {
            s.setObject(1, OffsetDateTime.parse(desde)); s.setObject(2, OffsetDateTime.parse(hasta));
            if (tipoCargaId != null) s.setLong(3, tipoCargaId);
            try (var r = s.executeQuery()) {
                Map<String, long[]> resultado = new HashMap<>();
                while (r.next()) {
                    assertNull(resultado.put(r.getString(1) + ":" + r.getString(2), new long[]{r.getLong(4), r.getLong(5)}));
                }
                return resultado;
            }
        }
    }
}
