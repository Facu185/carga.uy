package uy.edu.fing.tse.carga.ejb.arranque;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.persistencia.CuentaInternaDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.HashContrasenas;

import java.util.logging.Logger;

/**
 * Crea el primer administrador al arrancar si no hay ninguna cuenta interna.
 * Usuario y clave salen de ADMIN_INICIAL_USUARIO y ADMIN_INICIAL_PASSWORD: nunca del repositorio.
 */
@Singleton
@Startup
@DependsOn("Migraciones") // primero tienen que existir las tablas
public class InicializadorAdmin {

    private static final Logger LOG = Logger.getLogger(InicializadorAdmin.class.getName());

    @Inject
    CuentaInternaDAO dao;

    @PostConstruct
    void crearAdminInicial() {
        crearSiHaceFalta(System.getenv("ADMIN_INICIAL_USUARIO"), System.getenv("ADMIN_INICIAL_PASSWORD"));
    }

    /** Separado del arranque para poder probarlo sin variables de entorno. */
    void crearSiHaceFalta(String usuario, String password) {
        if (dao.contar() > 0) {
            return;
        }
        if (usuario == null || usuario.isBlank() || password == null || password.isBlank()) {
            LOG.warning("No hay cuentas internas y faltan ADMIN_INICIAL_USUARIO / ADMIN_INICIAL_PASSWORD");
            return;
        }
        dao.guardar(new CuentaInterna(usuario.trim(), "Administrador inicial",
                HashContrasenas.generar(password), Rol.ADMINISTRADOR));
        LOG.info("Administrador inicial creado: " + usuario.trim());
    }
}
