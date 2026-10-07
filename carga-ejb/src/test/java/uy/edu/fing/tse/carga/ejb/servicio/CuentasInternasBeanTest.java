package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.cuenta.CambioRolDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.CuentaInternaDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.NuevaCuentaInternaDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.PasswordTemporalDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.seguridad.HashContrasenas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuentasInternasBeanTest {

    private CuentasInternasBean servicio;
    private Falsos.CuentasEnMemoria cuentas;
    private Falsos.AuditoriaFalsa auditoria;
    private CuentaInterna yo;
    private CuentaInterna autoridad;

    @BeforeEach
    void preparar() {
        yo = new CuentaInterna("admin", "Admin", HashContrasenas.generar("clave-del-admin"), Rol.ADMINISTRADOR);
        autoridad = new CuentaInterna("ministra", "Ministra", HashContrasenas.generar("clave-ministra"),
                Rol.AUTORIDAD);
        cuentas = new Falsos.CuentasEnMemoria().con(yo, autoridad);
        auditoria = new Falsos.AuditoriaFalsa();
        servicio = new CuentasInternasBean();
        servicio.dao = cuentas;
        servicio.auditoria = auditoria;
        servicio.contexto = Falsos.sesion("admin", "ADMINISTRADOR");
    }

    @Test
    void creaUnaAutoridadConLaClaveHasheada() throws NegocioException {
        CuentaInternaDTO creada = servicio.crear(
                new NuevaCuentaInternaDTO(" nueva ", "Nueva", "una-clave-bien-larga", "AUTORIDAD"));

        assertEquals("nueva", creada.getUsuario());
        CuentaInterna guardada = cuentas.buscarPorUsuario("nueva").orElseThrow();
        assertTrue(HashContrasenas.verificar("una-clave-bien-larga", guardada.getPasswordHash()));
        assertEquals(AccionAuditada.USUARIO_INTERNO_CREADO, auditoria.ultimo().accion());
    }

    @Test
    void noPermiteRolesQueNoSonInternos() {
        assertThrows(NegocioException.class, () -> servicio.crear(
                new NuevaCuentaInternaDTO("x", "X", "una-clave-bien-larga", "FUNCIONARIO")));
        assertThrows(NegocioException.class, () -> servicio.crear(
                new NuevaCuentaInternaDTO("x", "X", "una-clave-bien-larga", "INVENTADO")));
    }

    @Test
    void exigeClaveLargaDatosCompletosYUsuarioNoRepetido() {
        assertThrows(NegocioException.class, () -> servicio.crear(
                new NuevaCuentaInternaDTO("x", "X", "corta", "AUTORIDAD")));
        assertThrows(NegocioException.class, () -> servicio.crear(
                new NuevaCuentaInternaDTO(" ", "X", "una-clave-bien-larga", "AUTORIDAD")));
        assertThrows(NegocioException.class, () -> servicio.crear(
                new NuevaCuentaInternaDTO("ministra", "Otra", "una-clave-bien-larga", "AUTORIDAD")));
    }

    @Test
    void darDeBajaCierraSusSesiones() throws NegocioException {
        int antes = autoridad.getVersionCredenciales();
        assertFalse(servicio.cambiarEstado(autoridad.getId(), false).isActivo());
        assertEquals(antes + 1, autoridad.getVersionCredenciales());
    }

    @Test
    void nadieSeDaDeBajaASiMismo() {
        assertThrows(NegocioException.class, () -> servicio.cambiarEstado(yo.getId(), false));
    }

    @Test
    void siempreQuedaAlMenosUnAdministradorActivo() {
        servicio.contexto = Falsos.sesion("otro", "ADMINISTRADOR");
        assertThrows(NegocioException.class, () -> servicio.cambiarEstado(yo.getId(), false));
        assertThrows(NegocioException.class, () -> servicio.cambiarRol(yo.getId(), new CambioRolDTO("AUTORIDAD")));
    }

    @Test
    void cambiarElRolCierraSusSesionesYNadieSeLoCambiaASiMismo() throws NegocioException {
        assertEquals("ADMINISTRADOR", servicio.cambiarRol(autoridad.getId(), new CambioRolDTO("ADMINISTRADOR")).getRol());
        assertEquals(1, autoridad.getVersionCredenciales());

        assertThrows(NegocioException.class, () -> servicio.cambiarRol(yo.getId(), new CambioRolDTO("AUTORIDAD")));
    }

    @Test
    void resetearGeneraUnaTemporalQueHayQueCambiar() throws NegocioException {
        PasswordTemporalDTO temporal = servicio.resetearPassword(autoridad.getId());

        assertTrue(HashContrasenas.verificar(temporal.getPasswordTemporal(), autoridad.getPasswordHash()));
        assertTrue(autoridad.isDebeCambiarPassword());
        assertThrows(NegocioException.class, () -> servicio.resetearPassword(yo.getId()));
    }

    @Test
    void unaCuentaDeLdapNoCambiaDeRolNiDeClaveDesdeCarga() {
        CuentaInterna ldap = cuentas.guardar(CuentaInterna.provisionarDesdeLdap("jperez", "Juan", Rol.AUTORIDAD));
        assertThrows(NegocioException.class, () -> servicio.cambiarRol(ldap.getId(), new CambioRolDTO("ADMINISTRADOR")));
        assertThrows(NegocioException.class, () -> servicio.resetearPassword(ldap.getId()));
    }

    @Test
    void cuentaInexistente() {
        assertThrows(NegocioException.class, () -> servicio.desbloquear(99L));
    }
}
