package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.cuenta.CambioPasswordDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.SesionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.seguridad.HashContrasenas;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuentaBeanTest {

    private static final String ACTUAL = "clave-actual-123";

    private CuentaBean servicio;
    private CuentaInterna ministra;
    private Falsos.AuditoriaFalsa auditoria;
    private Falsos.UsuariosEnMemoria usuarios;
    private Falsos.TokensFalsos tokens;

    @BeforeEach
    void preparar() {
        ministra = new CuentaInterna("ministra", "Ministra", HashContrasenas.generar(ACTUAL), Rol.AUTORIDAD);
        ministra.cambiarPassword(HashContrasenas.generar(ACTUAL), true); // temporal, como despues de un reseteo
        auditoria = new Falsos.AuditoriaFalsa();
        usuarios = new Falsos.UsuariosEnMemoria();
        tokens = new Falsos.TokensFalsos();
        servicio = new CuentaBean();
        servicio.cuentas = new Falsos.CuentasEnMemoria().con(ministra);
        servicio.usuarios = usuarios;
        servicio.tokenService = tokens;
        servicio.auditoria = auditoria;
        servicio.contexto = Falsos.sesion("ministra", "AUTORIDAD");
    }

    @Test
    void cambiaLaClaveYDevuelveUnaSesionNuevaSinClaveTemporal() throws NegocioException {
        int antes = ministra.getVersionCredenciales();

        SesionDTO sesion = servicio.cambiarMiPassword(new CambioPasswordDTO(ACTUAL, "una-nueva-bien-larga"));

        assertFalse(sesion.isDebeCambiarPassword());
        assertTrue(HashContrasenas.verificar("una-nueva-bien-larga", ministra.getPasswordHash()));
        assertEquals(antes + 1, ministra.getVersionCredenciales());
        assertEquals(AccionAuditada.PASSWORD_CAMBIADA, auditoria.ultimo().accion());
    }

    @Test
    void exigeLaClaveActualCorrecta() {
        assertThrows(NegocioException.class,
                () -> servicio.cambiarMiPassword(new CambioPasswordDTO("otra", "una-nueva-bien-larga")));
        assertThrows(NegocioException.class, () -> servicio.cambiarMiPassword(null));
    }

    @Test
    void laNuevaTieneQueSerLargaYDistintaDeLaActual() {
        assertThrows(NegocioException.class, () -> servicio.cambiarMiPassword(new CambioPasswordDTO(ACTUAL, "corta")));
        assertThrows(NegocioException.class, () -> servicio.cambiarMiPassword(new CambioPasswordDTO(ACTUAL, ACTUAL)));
    }

    @Test
    void unaCuentaDeLdapNoCambiaSuClaveDesdeCarga() {
        servicio.cuentas = new Falsos.CuentasEnMemoria()
                .con(CuentaInterna.provisionarDesdeLdap("ministra", "Ministra", Rol.AUTORIDAD));
        assertThrows(NegocioException.class,
                () -> servicio.cambiarMiPassword(new CambioPasswordDTO(ACTUAL, "una-nueva-bien-larga")));
    }

    @Test
    void renovarUnaSesionDeGubUyReleeLosRoles() throws NegocioException {
        Usuario ana = usuarios.guardar(new Usuario("12345672", "Ana", null));
        ana.asignarRol(Falsos.rol(Rol.CIUDADANO));
        ana.asignarRol(Falsos.rol(Rol.FUNCIONARIO));
        servicio.contexto = Falsos.sesion("12345672", "CIUDADANO");

        SesionDTO sesion = servicio.renovar();

        assertEquals("FUNCIONARIO", sesion.getRol());
        assertEquals(Set.of("CIUDADANO", "FUNCIONARIO"), tokens.ultimosRoles);
    }

    @Test
    void renovarUnaCuentaDadaDeBajaFalla() {
        ministra.setActivo(false);
        assertThrows(NegocioException.class, () -> servicio.renovar());
    }
}
