package uy.edu.fing.tse.carga.web.seguridad;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.AutenticacionLocal;
import uy.edu.fing.tse.carga.api.contract.service.CuentaLocal;
import uy.edu.fing.tse.carga.api.dto.cuenta.CambioPasswordDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.LoginDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.SesionDTO;
import uy.edu.fing.tse.carga.api.exception.CredencialesInvalidasException;
import uy.edu.fing.tse.carga.api.exception.CuentaBloqueadaException;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SesionJsfServletTest {
    private final HttpJsfDoble http = new HttpJsfDoble();
    private final SesionJsfServlet servlet = new SesionJsfServlet();
    private final SesionDTO sesion = new SesionDTO("jwt-sintetico", "2099-01-01T00:00:00Z", "admin-prueba",
            "Administrador de prueba", Roles.ADMINISTRADOR, false, "BASE_DE_DATOS");
    private LoginDTO credenciales;

    @BeforeEach
    void configurar() {
        http.ruta = "/jsf/ingresar";
        http.metodo = "POST";
        http.headers.put("Origin", "http://localhost:8080");
        http.parametros.put("usuario", "admin-prueba");
        http.parametros.put("password", "clave-sintetica-no-real");
        servlet.autenticacion = HttpJsfDoble.doble(AutenticacionLocal.class, (nombre, args) -> {
            assertEquals("login", nombre);
            credenciales = (LoginDTO) args[0];
            return sesion;
        });
    }

    @Test
    void loginReutilizaP1YGuardaCookieSinHttpSession() throws Exception {
        servlet.doPost(http.pedido, http.respuesta);
        assertEquals("admin-prueba", credenciales.getUsuario());
        Cookie cookie = http.salidaCookies.getFirst();
        assertEquals("jwt-sintetico", cookie.getValue());
        assertEquals("/carga", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertEquals("Strict", cookie.getAttribute("SameSite"));
        assertNull(cookie.getDomain());
        assertEquals(-1, cookie.getMaxAge());
        assertEquals("/carga/empresas.xhtml", http.salidaHeaders.get("Location"));
    }

    @Test
    void httpsMarcaSecureYClaveTemporalRedirigeAlCambio() throws Exception {
        http.seguro = true;
        http.headers.put("Origin", "https://localhost");
        sesion.setDebeCambiarPassword(true);
        servlet.doPost(http.pedido, http.respuesta);
        assertTrue(http.salidaCookies.getFirst().getSecure());
        assertEquals("/carga/cambiar-clave-jsf.xhtml", http.salidaHeaders.get("Location"));
    }

    @Test
    void credencialesInvalidasYBloqueoNoCreanCookie() throws Exception {
        for (RuntimeException error : new RuntimeException[]{new CredencialesInvalidasException(), new CuentaBloqueadaException(15)}) {
            servlet.autenticacion = HttpJsfDoble.doble(AutenticacionLocal.class, (nombre, args) -> { throw error; });
            servlet.doPost(http.pedido, http.respuesta);
            assertTrue(http.salidaCookies.isEmpty());
            assertTrue(http.salidaHeaders.get("Location").startsWith("/carga/ingreso-jsf.xhtml?error="));
        }
    }

    @Test
    void postCruzadoNoInvocaLogin() throws Exception {
        http.headers.put("Origin", "https://otro.invalid");
        servlet.doPost(http.pedido, http.respuesta);
        assertEquals(403, http.estado);
        assertNull(credenciales);
        assertTrue(http.salidaCookies.isEmpty());
    }

    @Test
    void servicioNoDisponibleNoExponeDetallesNiCreaSesion() throws Exception {
        servlet.autenticacion = HttpJsfDoble.doble(AutenticacionLocal.class, (nombre, args) -> {
            throw new ServicioNoDisponibleException("detalle tecnico no publicable");
        });
        servlet.doPost(http.pedido, http.respuesta);
        assertEquals("/carga/ingreso-jsf.xhtml?error=servicio", http.salidaHeaders.get("Location"));
        assertTrue(http.salidaCookies.isEmpty());
    }

    @Test
    void cambiarClaveUsaLaCuentaAutenticadaYSustituyeToken() throws Exception {
        http.ruta = "/jsf/cambiar-clave";
        http.usuario = "admin-prueba";
        http.roles = Set.of(Roles.ADMINISTRADOR);
        http.parametros.put("actual", "clave-vieja-sintetica");
        http.parametros.put("nueva", "clave-nueva-sintetica");
        servlet.cuenta = HttpJsfDoble.doble(CuentaLocal.class, (nombre, args) -> {
            assertEquals("cambiarMiPassword", nombre);
            CambioPasswordDTO datos = (CambioPasswordDTO) args[0];
            assertEquals("clave-vieja-sintetica", datos.getActual());
            assertEquals("clave-nueva-sintetica", datos.getNueva());
            return sesion;
        });
        servlet.doPost(http.pedido, http.respuesta);
        assertEquals("jwt-sintetico", http.salidaCookies.getFirst().getValue());
        assertEquals("/carga/empresas.xhtml", http.salidaHeaders.get("Location"));
    }

    @Test
    void cambioInvalidoConservaTokenYNoExponeExcepcion() throws Exception {
        http.ruta = "/jsf/cambiar-clave";
        http.usuario = "admin-prueba";
        http.roles = Set.of(Roles.ADMINISTRADOR);
        servlet.cuenta = HttpJsfDoble.doble(CuentaLocal.class, (nombre, args) -> { throw new NegocioException("detalle interno"); });
        servlet.doPost(http.pedido, http.respuesta);
        assertTrue(http.salidaCookies.isEmpty());
        assertEquals("/carga/cambiar-clave-jsf.xhtml?error=clave", http.salidaHeaders.get("Location"));
    }

    @Test
    void cambioSinPrincipalNoInvocaCuenta() throws Exception {
        http.ruta = "/jsf/cambiar-clave";
        servlet.doPost(http.pedido, http.respuesta);
        assertEquals(403, http.estado);
        assertTrue(http.salidaCookies.isEmpty());
    }

    @Test
    void salirBorraCookieConElMismoPath() throws Exception {
        http.ruta = "/jsf/salir";
        servlet.doPost(http.pedido, http.respuesta);
        Cookie cookie = http.salidaCookies.getFirst();
        assertEquals(SesionJsf.COOKIE, cookie.getName());
        assertEquals(0, cookie.getMaxAge());
        assertEquals("/carga", cookie.getPath());
        assertEquals("/carga/ingreso-jsf.xhtml", http.salidaHeaders.get("Location"));
    }
}
