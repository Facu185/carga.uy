package uy.edu.fing.tse.carga.ejb.persistencia;

import org.flywaydb.core.Flyway;
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
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Las sentencias de InformacionPublicaDAO contra el esquema real: todas las migraciones Flyway
 * aplicadas sobre PostGIS. No prueba el mapeo de Hibernate ni la seguridad del contenedor.
 */
@Tag("integracion")
class InformacionPublicaPostgresTest {

    private static GenericContainer<?> postgres;

    @BeforeAll
    static void preparar() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "requiere Docker");
        postgres = new GenericContainer<>(DockerImageName.parse("postgis/postgis:17-3.5"))
                .withCreateContainerCmdModifier(c -> c.withPlatform("linux/amd64"))
                .withEnv("POSTGRES_DB", "publico").withEnv("POSTGRES_USER", "publico")
                .withEnv("POSTGRES_PASSWORD", "clave-sintetica").withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        postgres.start();
        Flyway.configure().dataSource(url(), "publico", "clave-sintetica")
                .locations("filesystem:src/main/resources/db/migration").load().migrate();
    }

    @AfterAll
    static void cerrar() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    void agrupaPorMesTipoYRubroConLasEmpresasDeCadaCelda() throws Exception {
        try (Connection c = conexion(); var s = c.createStatement()) {
            long empresa = valor(c, "SELECT id FROM empresa ORDER BY id LIMIT 1");
            long vehiculo = valor(c, "SELECT id FROM vehiculo WHERE empresa_id = " + empresa + " LIMIT 1");
            long chofer = valor(c, "SELECT id FROM chofer WHERE empresa_id = " + empresa + " LIMIT 1");
            long tipo = valor(c, "SELECT id FROM tipo_carga ORDER BY id LIMIT 1");
            s.execute("DELETE FROM guia");
            for (String inicio : new String[]{"2026-10-31T23:30:00-03:00", "2026-11-01T00:30:00-03:00", "2026-11-15T10:00:00-03:00"}) {
                s.execute("INSERT INTO guia (empresa_id, vehiculo_id, chofer_id, tipo_carga_id, fecha, origen, destino,"
                        + " estado, creada_en, peso_kg, inicio, fin) VALUES (" + empresa + ", " + vehiculo + ", " + chofer
                        + ", " + tipo + ", '2026-11-01', 'A', 'B', 'ASIGNADA', now(), 10000, '" + inicio + "', '"
                        + inicio + "'::timestamptz + interval '1 day')");
            }

            Map<String, long[]> r = guias(c, "2026-10-01T03:00:00Z", "2026-12-01T03:00:00Z");

            // la medianoche que cuenta es la de Montevideo, no la de UTC
            assertEquals(1, r.get("MES:2026-10")[0]);
            assertEquals(2, r.get("MES:2026-11")[0]);
            assertEquals(3, r.get("TIPO:" + tipo)[0]);
            assertEquals(1, r.get("TIPO:" + tipo)[1], "una sola empresa distinta");
            assertEquals(30_000, r.get("TIPO:" + tipo)[2]);
            assertEquals(3, r.get("RUBRO:SIN_DATO")[0]);
        }
    }

    @Test
    void laFlotaCuentaSoloVehiculosHabilitadosDeEmpresasActivas() throws Exception {
        try (Connection c = conexion();
             PreparedStatement s = preparar(c, InformacionPublicaDAO.SQL_FLOTA)) {
            long esperadosHabilitados = valor(c, "SELECT count(*) FROM vehiculo v JOIN empresa e ON e.id = v.empresa_id"
                    + " WHERE v.habilitado AND e.activa");
            parametros(s, InformacionPublicaDAO.SQL_FLOTA, Map.of(1, LocalDate.of(2026, 11, 2)));
            Map<String, Long> r = new HashMap<>();
            try (var rs = s.executeQuery()) {
                while (rs.next()) {
                    r.put(rs.getString(1), rs.getLong(2));
                }
            }
            assertEquals(esperadosHabilitados, r.get("HABILITADOS"));
            assertEquals(r.get("HABILITADOS"), r.get("AL_DIA") + r.get("NO_AL_DIA"));
        }
    }

    private static Map<String, long[]> guias(Connection c, String desde, String hasta) throws Exception {
        try (PreparedStatement s = preparar(c, InformacionPublicaDAO.SQL_GUIAS)) {
            parametros(s, InformacionPublicaDAO.SQL_GUIAS,
                    Map.of(1, OffsetDateTime.parse(desde), 2, OffsetDateTime.parse(hasta)));
            Map<String, long[]> r = new HashMap<>();
            try (var rs = s.executeQuery()) {
                while (rs.next()) {
                    long empresas = InformacionPublicaDAO.ids(rs.getString(5)).size();
                    r.put(rs.getString(1) + ":" + rs.getString(2), new long[]{rs.getLong(4), empresas, rs.getLong(6)});
                }
            }
            return r;
        }
    }

    /** Adapta solo los parametros ?N de JPA a ? de JDBC: es la misma sentencia de produccion. */
    private static PreparedStatement preparar(Connection c, String sql) throws Exception {
        return c.prepareStatement(sql.replaceAll("\\?\\d", "?"));
    }

    private static void parametros(PreparedStatement s, String sql, Map<Integer, Object> valores) throws Exception {
        Matcher m = Pattern.compile("\\?(\\d)").matcher(sql);
        int i = 1;
        while (m.find()) {
            s.setObject(i++, valores.get(Integer.parseInt(m.group(1))));
        }
    }

    private static long valor(Connection c, String sql) throws Exception {
        try (var s = c.createStatement(); var r = s.executeQuery(sql)) {
            r.next();
            return r.getLong(1);
        }
    }

    private static Connection conexion() throws Exception {
        return DriverManager.getConnection(url(), "publico", "clave-sintetica");
    }

    private static String url() {
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/publico";
    }
}
