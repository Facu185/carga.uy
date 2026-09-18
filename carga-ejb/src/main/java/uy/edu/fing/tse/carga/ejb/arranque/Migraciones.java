package uy.edu.fing.tse.carga.ejb.arranque;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Aplica las migraciones de la base al arrancar el componente central.
 *
 * El esquema es propiedad de las migraciones (carpeta db/migration) y NO de Hibernate:
 * con varias personas trabajando en paralelo, generar el esquema automaticamente hace
 * que la base de cada uno termine distinta y que los cambios no queden versionados.
 */
@Singleton
@Startup
// Flyway maneja sus propias transacciones y necesita autocommit; sin esto el
// contenedor abre una transaccion JTA para @PostConstruct y falla con IJ031017.
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class Migraciones {

    private static final Logger LOG = Logger.getLogger(Migraciones.class.getName());

    @Resource(lookup = "java:jboss/datasources/CargaDS")
    private DataSource dataSource;

    @PostConstruct
    public void migrar() {
        Flyway flyway = Flyway.configure(Migraciones.class.getClassLoader())
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        var resultado = flyway.migrate();
        LOG.info(() -> "Migraciones aplicadas: " + resultado.migrationsExecuted
                + " | version actual: " + resultado.targetSchemaVersion);
    }
}
