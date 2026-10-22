package uy.edu.fing.tse.carga.web.seguridad;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.AutenticacionLocal;
import uy.edu.fing.tse.carga.api.dto.cuenta.DatosSesionDTO;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MecanismoJwtTest {
    private final HttpJsfDoble http = new HttpJsfDoble();
    private final MecanismoJwt mecanismo = new MecanismoJwt();
    private DatosSesionDTO sesion = new DatosSesionDTO("admin-prueba", Set.of(Roles.ADMINISTRADOR), false);
    private String tokenValidado;

    @BeforeEach
    void configurar() {
        mecanismo.autenticacion = HttpJsfDoble.doble(AutenticacionLocal.class, (nombre, args) -> {
            assertEquals("validarToken", nombre);
            tokenValidado = (String) args[0];
            return sesion;
        });
    }

    private AuthenticationStatus validar() {
        return mecanismo.validateRequest(http.pedido, http.respuesta, http.contexto);
    }

    @Test
    void sinSesionRedirigeAntesDeInicializarLaVista() {
        assertEquals(AuthenticationStatus.SEND_CONTINUE, validar());
        assertEquals("/carga/ingreso-jsf.xhtml", http.salidaHeaders.get("Location"));
        assertNull(http.autenticado);
    }

    @Test
    void cookieValidaNotificaIdentidadYRolesDelServidor() {
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "jwt-sintetico")};
        assertEquals(AuthenticationStatus.SUCCESS, validar());
        assertEquals("jwt-sintetico", tokenValidado);
        assertEquals("admin-prueba", http.autenticado);
        assertEquals(Set.of(Roles.ADMINISTRADOR), http.rolesNotificados);
        assertEquals("no-store", http.salidaHeaders.get("Cache-Control"));
    }

    @Test
    void cookieNoAutenticaRestSoapNiPanelP4() {
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "jwt-sintetico")};
        for (String ruta : Set.of("/api/empresas", "/EmpresaService", "/panel.xhtml")) {
            http.ruta = ruta;
            assertEquals(AuthenticationStatus.NOT_DONE, validar());
        }
        assertNull(tokenValidado);
    }

    @Test
    void conservaBearerRestYSuRechazoDeClaveTemporal() {
        http.ruta = "/api/empresas";
        http.headers.put("Authorization", "Bearer jwt-header");
        assertEquals(AuthenticationStatus.SUCCESS, validar());
        assertEquals("jwt-header", tokenValidado);
        sesion.setDebeCambiarPassword(true);
        assertEquals(AuthenticationStatus.SEND_FAILURE, validar());
        assertEquals(403, http.estado);
        http.ruta = "/api/auth/cambiar-password";
        assertEquals(AuthenticationStatus.SUCCESS, validar());
    }

    @Test
    void bearerInvalidoNoSeRescataConCookieValida() {
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "jwt-cookie")};
        http.headers.put("Authorization", "Bearer jwt-invalido");
        sesion = null;
        assertEquals(AuthenticationStatus.SEND_CONTINUE, validar());
        assertEquals("jwt-invalido", tokenValidado);
        assertNull(http.autenticado);
    }

    @Test
    void cookieVencidaSeBorraYAjaxRecibeRedireccionJsf() {
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "vencido")};
        http.headers.put("Faces-Request", "partial/ajax");
        sesion = null;
        assertEquals(AuthenticationStatus.SEND_CONTINUE, validar());
        assertTrue(http.cuerpo.toString().contains("<partial-response><redirect url=\"/carga/ingreso-jsf.xhtml\""));
        assertEquals(0, http.salidaCookies.getFirst().getMaxAge());
        assertNull(http.autenticado);
    }

    @Test
    void claveTemporalSoloPermiteCambioYSalidaEnJsf() {
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "temporal")};
        sesion.setDebeCambiarPassword(true);
        assertEquals(AuthenticationStatus.SEND_CONTINUE, validar());
        assertEquals("/carga/cambiar-clave-jsf.xhtml", http.salidaHeaders.get("Location"));
        http.ruta = "/cambiar-clave-jsf.xhtml";
        assertEquals(AuthenticationStatus.SUCCESS, validar());
        http.ruta = "/jsf/cambiar-clave";
        http.metodo = "POST";
        http.headers.put("Origin", "http://localhost:8080");
        assertEquals(AuthenticationStatus.SUCCESS, validar());
    }

    @Test
    void rolInsuficienteNoLlegaAlBackingBean() {
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "autoridad")};
        sesion = new DatosSesionDTO("autoridad-prueba", Set.of(Roles.AUTORIDAD), false);
        http.ruta = "/vehiculos.xhtml";
        assertEquals(AuthenticationStatus.SEND_FAILURE, validar());
        assertEquals(403, http.estado);
        assertNull(http.autenticado);
        http.ruta = "/empresas.xhtml";
        assertEquals(AuthenticationStatus.SUCCESS, validar());
    }

    @Test
    void postSinOrigenODeOtroOrigenSeRechazaAntesDeAutenticar() {
        http.metodo = "POST";
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "jwt")};
        assertEquals(AuthenticationStatus.SEND_FAILURE, validar());
        http.headers.put("Origin", "http://localhost:8080.ejemplo.invalid");
        assertEquals(AuthenticationStatus.SEND_FAILURE, validar());
        assertEquals(403, http.estado);
        assertNull(tokenValidado);
    }

    @Test
    void postMismoOrigenSeAceptaYOriginTienePrecedenciaSobreReferer() {
        http.metodo = "POST";
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "jwt")};
        http.headers.put("Referer", "http://localhost:8080/carga/empresas.xhtml");
        assertEquals(AuthenticationStatus.SUCCESS, validar());
        http.headers.put("Origin", "null");
        assertEquals(AuthenticationStatus.SEND_FAILURE, validar());
    }

    @Test
    void loginTambienRechazaPostCruzadoPeroPermiteReingresarConCookieVencida() {
        http.ruta = "/jsf/ingresar";
        http.metodo = "POST";
        assertEquals(AuthenticationStatus.SEND_FAILURE, validar());
        http.headers.put("Origin", "http://localhost:8080");
        http.cookies = new Cookie[]{new Cookie(SesionJsf.COOKIE, "vencido")};
        sesion = null;
        assertEquals(AuthenticationStatus.NOT_DONE, validar());
    }
}
