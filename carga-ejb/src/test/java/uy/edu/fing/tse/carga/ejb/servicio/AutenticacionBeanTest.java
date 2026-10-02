package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.AccionAuditada;
import uy.edu.fing.tse.carga.api.CredencialesInvalidasException;
import uy.edu.fing.tse.carga.api.CuentaBloqueadaException;
import uy.edu.fing.tse.carga.api.dto.DatosSesionDTO;
import uy.edu.fing.tse.carga.api.dto.LoginDTO;
import uy.edu.fing.tse.carga.api.dto.SesionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.seguridad.ClienteLdap;
import uy.edu.fing.tse.carga.ejb.seguridad.HashContrasenas;
import uy.edu.fing.tse.carga.ejb.seguridad.IdentidadLdap;
import uy.edu.fing.tse.carga.ejb.seguridad.TokenService;
import uy.edu.fing.tse.carga.ejb.seguridad.VerificadorSesiones;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacionBeanTest {

    private static final String CLAVE = "clave-correcta-123";

    private AutenticacionBean servicio;
    private Falsos.CuentasEnMemoria cuentas;
    private Falsos.AuditoriaFalsa auditoria;
    private CuentaInterna admin;

    @BeforeEach
    void preparar() {
        admin = new CuentaInterna("admin", "Admin", HashContrasenas.generar(CLAVE), Rol.ADMINISTRADOR);
        cuentas = new Falsos.CuentasEnMemoria().con(admin);
        auditoria = new Falsos.AuditoriaFalsa();
        servicio = new AutenticacionBean();
        servicio.dao = cuentas;
        servicio.tokenService = new Falsos.TokensFalsos();
        servicio.auditoria = auditoria;
        servicio.metricas = new uy.edu.fing.tse.carga.ejb.observabilidad.Metricas();
    }

    private SesionDTO login(String usuario, String clave) {
        return servicio.login(new LoginDTO(usuario, clave));
    }

    @Test
    void loginCorrectoDevuelveLaSesionYQuedaEnLaAuditoria() {
        SesionDTO sesion = login(" admin ", CLAVE);

        assertEquals("token-admin", sesion.getToken());
        assertEquals("ADMINISTRADOR", sesion.getRol());
        assertEquals("BASE_DE_DATOS", sesion.getOrigen());
        assertFalse(sesion.isDebeCambiarPassword());
        assertEquals(AccionAuditada.INGRESO_EXITOSO, auditoria.ultimo().accion());
        assertEquals("admin", auditoria.ultimo().usuario());
    }

    @Test
    void claveIncorrectaSumaUnIntentoYSeAudita() {
        assertThrows(CredencialesInvalidasException.class, () -> login("admin", "otra"));

        assertEquals(1, admin.getIntentosFallidos());
        assertEquals(AccionAuditada.INGRESO_FALLIDO, auditoria.ultimo().accion());
    }

    @Test
    void alQuintoIntentoFallidoLaCuentaSeBloqueaAunqueDespuesLaClaveSeaCorrecta() {
        for (int i = 0; i < AutenticacionBean.MAXIMO_INTENTOS - 1; i++) {
            assertThrows(CredencialesInvalidasException.class, () -> login("admin", "mal"));
        }
        assertThrows(CuentaBloqueadaException.class, () -> login("admin", "mal"));
        assertEquals(AccionAuditada.CUENTA_BLOQUEADA, auditoria.ultimo().accion());

        assertThrows(CuentaBloqueadaException.class, () -> login("admin", CLAVE));
    }

    @Test
    void usuarioInexistenteDaElMismoErrorQueUnaClaveIncorrecta() {
        CredencialesInvalidasException e = assertThrows(CredencialesInvalidasException.class,
                () -> login("nadie", CLAVE));
        assertEquals(new CredencialesInvalidasException().getMessage(), e.getMessage());
    }

    @Test
    void unaCuentaDadaDeBajaNoEntraAunqueLaClaveSeaCorrecta() {
        admin.setActivo(false);
        assertThrows(CredencialesInvalidasException.class, () -> login("admin", CLAVE));
    }

    @Test
    void datosVaciosSeRechazanSinConsultar() {
        assertThrows(CredencialesInvalidasException.class, () -> servicio.login(null));
        assertThrows(CredencialesInvalidasException.class, () -> login(" ", CLAVE));
        assertThrows(CredencialesInvalidasException.class, () -> login("admin", ""));
    }

    // ---------------- LDAP ----------------

    private void conLdap(Optional<IdentidadLdap> respuesta) {
        servicio.ldapHabilitado = true;
        servicio.ldap = new ClienteLdap() {
            @Override
            public Optional<IdentidadLdap> autenticar(String uid, String password) {
                return respuesta;
            }
        };
    }

    @Test
    void conLdapElPrimerIngresoCreaLaCuentaSinClaveLocal() {
        conLdap(Optional.of(new IdentidadLdap("jperez", "Juan Perez", Rol.AUTORIDAD)));

        SesionDTO sesion = login("jperez", "la-del-directorio");

        assertEquals("LDAP", sesion.getOrigen());
        CuentaInterna creada = cuentas.buscarPorUsuario("jperez").orElseThrow();
        assertTrue(creada.esDeLdap());
        assertNull(creada.getPasswordHash());
    }

    @Test
    void conLdapUnaCuentaDadaDeBajaLocalmenteNoEntra() {
        CuentaInterna local = CuentaInterna.provisionarDesdeLdap("jperez", "Juan Perez", Rol.AUTORIDAD);
        local.setActivo(false);
        cuentas.guardar(local);
        conLdap(Optional.of(new IdentidadLdap("jperez", "Juan Perez", Rol.AUTORIDAD)));

        assertThrows(CredencialesInvalidasException.class, () -> login("jperez", "la-del-directorio"));
    }

    @Test
    void conLdapCredencialesIncorrectasNoEntran() {
        conLdap(Optional.empty());
        assertThrows(CredencialesInvalidasException.class, () -> login("jperez", "mal"));
    }

    // ---------------- validacion de tokens ----------------

    private void conToken(Optional<TokenService.DatosToken> datos, boolean vigente) {
        servicio.tokenService = new Falsos.TokensFalsos() {
            @Override
            public Optional<DatosToken> validar(String token) {
                return datos;
            }
        };
        servicio.verificador = new VerificadorSesiones() {
            @Override
            public boolean sigueVigente(TokenService.DatosToken d) {
                return vigente;
            }
        };
    }

    @Test
    void unTokenValidoYVigenteDevuelveUsuarioYRoles() {
        conToken(Optional.of(new TokenService.DatosToken("admin", Set.of("ADMINISTRADOR"),
                TokenService.ORIGEN_INTERNO, 0, false)), true);

        DatosSesionDTO datos = servicio.validarToken("x");

        assertNotNull(datos);
        assertEquals("admin", datos.getUsuario());
        assertEquals(Set.of("ADMINISTRADOR"), datos.getRoles());
    }

    @Test
    void unTokenRevocadoOInvalidoNoDevuelveSesion() {
        conToken(Optional.of(new TokenService.DatosToken("admin", Set.of("ADMINISTRADOR"),
                TokenService.ORIGEN_INTERNO, 0, false)), false);
        assertNull(servicio.validarToken("x"));

        conToken(Optional.empty(), true);
        assertNull(servicio.validarToken("x"));
    }
}
