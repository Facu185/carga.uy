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
                    INSERT INTO guia VALUES
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
            s.execute("CREATE TEMP TABLE guia (empresa_id bigint, rubro_id bigint, estado text, creada_en timestamptz, inicio timestamptz)");
        }
    }

    private static Map<String, long[]> consultar(Connection c, String desde, String hasta) throws Exception {
        // Adaptar exclusivamente placeholders JPA a JDBC: es la misma sentencia de produccion.
        try (var s = c.prepareStatement(ReporteActividadDAO.SQL.replace("?1", "?").replace("?2", "?"))) {
            s.setObject(1, OffsetDateTime.parse(desde)); s.setObject(2, OffsetDateTime.parse(hasta));
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
