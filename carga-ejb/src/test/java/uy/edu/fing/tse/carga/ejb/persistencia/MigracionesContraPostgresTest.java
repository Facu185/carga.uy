package uy.edu.fing.tse.carga.ejb.persistencia;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Levanta el mismo PostGIS del docker-compose, aplica TODAS las migraciones de Flyway desde cero
 * y verifica las reglas que la base hace cumplir por si sola (no solo la aplicacion).
 *
 * Es de integracion (etiqueta "integracion"): no corre con "mvn verify", sino con
 * "mvn verify -Pintegracion" y en el job de integracion del CI. Sin Docker se saltea.
 */
@Tag("integracion")
class MigracionesContraPostgresTest {

    private static final String USUARIO = "carga";
    private static final String CLAVE = "carga-test";

    private static GenericContainer<?> postgres;
    private static String url;
    private static MigrateResult resultado;

    @BeforeAll
    static void levantarYMigrar() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "sin Docker no se puede levantar PostgreSQL");
        postgres = new GenericContainer<>(DockerImageName.parse("postgis/postgis:17-3.5"))
                // La imagen solo existe para amd64: en Mac con Apple Silicon Docker la emula
                .withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/amd64"))
                .withEnv("POSTGRES_DB", "carga")
                .withEnv("POSTGRES_USER", USUARIO)
                .withEnv("POSTGRES_PASSWORD", CLAVE)
                .withExposedPorts(5432)
                // PostgreSQL arranca, se reinicia una vez tras inicializar y recien ahi queda listo
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        postgres.start();
        url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/carga";
        resultado = flyway().migrate();
    }

    @AfterAll
    static void bajar() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    /** Misma configuracion que el bean Migraciones del central. */
    private static Flyway flyway() {
        return Flyway.configure()
                .dataSource(url, USUARIO, CLAVE)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    private static Connection conexion() throws SQLException {
        return DriverManager.getConnection(url, USUARIO, CLAVE);
    }

    @Test
    void todasLasMigracionesSeAplicanDesdeCeroYVolverAMigrarNoHaceNada() {
        assertTrue(resultado.success);
        assertTrue(resultado.migrationsExecuted >= 4, "V1 a V4 como minimo");
        assertEquals(0, flyway().info().pending().length, "no puede quedar ninguna migracion pendiente");
        assertEquals(0, flyway().migrate().migrationsExecuted);
    }

    @Test
    void laAuditoriaNoSePuedeModificarNiBorrarNiSiquieraConSqlDirecto() throws SQLException {
        try (Connection c = conexion(); Statement sql = c.createStatement()) {
            sql.executeUpdate("INSERT INTO auditoria (usuario, accion, entidad, entidad_id) "
                    + "VALUES ('test', 'INGRESO_EXITOSO', 'CuentaInterna', 'test')");

            assertThrows(SQLException.class, () -> sql.executeUpdate("UPDATE auditoria SET usuario = 'otro'"));
            assertThrows(SQLException.class, () -> sql.executeUpdate("DELETE FROM auditoria"));
            assertThrows(SQLException.class, () -> sql.executeUpdate("TRUNCATE auditoria"));
        }
    }

    @Test
    void unaCuentaInternaSoloPuedeSerAdministradorOAutoridadYConClaveSegunSuOrigen() throws SQLException {
        String cuenta = "INSERT INTO cuenta_interna (nombre_usuario, nombre, rol, origen, password_hash) "
                + "VALUES ('%s', 'Prueba', '%s', '%s', %s)";
        try (Connection c = conexion(); Statement sql = c.createStatement()) {
            assertDoesNotThrow(() -> sql.executeUpdate(
                    cuenta.formatted("autoridad-test", "AUTORIDAD", "BASE_DE_DATOS", "'hash'")));
            assertDoesNotThrow(() -> sql.executeUpdate(cuenta.formatted("ldap-test", "AUTORIDAD", "LDAP", "NULL")));

            assertThrows(SQLException.class, () -> sql.executeUpdate(
                    cuenta.formatted("chofer-test", "CHOFER", "BASE_DE_DATOS", "'hash'")), "solo roles internos");
            assertThrows(SQLException.class, () -> sql.executeUpdate(
                    cuenta.formatted("autoridad-test", "AUTORIDAD", "BASE_DE_DATOS", "'hash'")), "el usuario es unico");
            assertThrows(SQLException.class, () -> sql.executeUpdate(
                    cuenta.formatted("sin-clave", "AUTORIDAD", "BASE_DE_DATOS", "NULL")),
                    "una cuenta local necesita clave");
            assertThrows(SQLException.class, () -> sql.executeUpdate(
                    cuenta.formatted("ldap-con-clave", "AUTORIDAD", "LDAP", "'hash'")),
                    "la clave de una cuenta LDAP la tiene el directorio, no esta base");
        }
    }

    @Test
    void unaPersonaNoPuedeTenerDosSolicitudesPendientes() throws SQLException {
        try (Connection c = conexion(); Statement sql = c.createStatement()) {
            sql.executeUpdate("INSERT INTO usuario (cedula, nombre) VALUES ('12345672', 'Ana')");
            // Una resuelta tiene fecha de resolucion; una pendiente no (ck_solicitud_resuelta)
            String solicitud = "INSERT INTO solicitud_funcionario (usuario_id, dependencia, estado, resuelta_en) "
                    + "SELECT id, 'DNT', '%s', %s FROM usuario WHERE cedula = '12345672'";

            sql.executeUpdate(solicitud.formatted("RECHAZADA", "now()"));
            sql.executeUpdate(solicitud.formatted("PENDIENTE", "NULL"));
            assertThrows(SQLException.class, () -> sql.executeUpdate(solicitud.formatted("PENDIENTE", "NULL")));
            assertThrows(SQLException.class, () -> sql.executeUpdate(solicitud.formatted("APROBADA", "NULL")),
                    "una solicitud resuelta tiene que tener fecha");
        }
    }
}
