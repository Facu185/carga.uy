package uy.edu.fing.tse.carga.web.seguridad;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;

import java.io.IOException;
import java.util.logging.Logger;

/** Formularios JSF sin guardar credenciales/JWT en beans de vista o HttpSession. */
@WebServlet(urlPatterns = {"/jsf/ingresar", "/jsf/cambiar-clave", "/jsf/salir"})
public class SesionJsfServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SesionJsfServlet.class.getName());

    @EJB AutenticacionLocal autenticacion;
    @EJB CuentaLocal cuenta;

    @Override
    protected void doPost(HttpServletRequest pedido, HttpServletResponse respuesta) throws ServletException, IOException {
        pedido.setCharacterEncoding("UTF-8");
        SesionJsf.noGuardarCache(respuesta);
        if (!SesionJsf.origenPermitido(pedido)) {
            respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        switch (pedido.getServletPath()) {
            case "/jsf/ingresar" -> ingresar(pedido, respuesta);
            case "/jsf/cambiar-clave" -> cambiarClave(pedido, respuesta);
            case "/jsf/salir" -> {
                SesionJsf.borrar(pedido, respuesta);
                descartarVistas(pedido);
                pedido.logout();
                SesionJsf.redirigir(pedido, respuesta, SesionJsf.INGRESO);
            }
            default -> respuesta.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void ingresar(HttpServletRequest pedido, HttpServletResponse respuesta) {
        LoginDTO datos = new LoginDTO(pedido.getParameter("usuario"), pedido.getParameter("password"));
        try {
            continuar(pedido, respuesta, autenticacion.login(datos));
        } catch (CredencialesInvalidasException e) {
            SesionJsf.redirigir(pedido, respuesta, SesionJsf.INGRESO + "?error=credenciales");
        } catch (CuentaBloqueadaException e) {
            SesionJsf.redirigir(pedido, respuesta, SesionJsf.INGRESO + "?error=bloqueo");
        } catch (ServicioNoDisponibleException e) {
            // No registrar el mensaje/causa del directorio: pueden contener datos de la persona.
            LOG.warning(() -> "componente=SesionJsfServlet correlation_id=" + Correlacion.actual().orElse("sin-correlacion")
                    + " evento=autenticacion_no_disponible");
            SesionJsf.redirigir(pedido, respuesta, SesionJsf.INGRESO + "?error=servicio");
        } finally {
            datos.setPassword(null);
        }
    }

    private void cambiarClave(HttpServletRequest pedido, HttpServletResponse respuesta) {
        if (pedido.getUserPrincipal() == null
                || !(pedido.isUserInRole(Roles.ADMINISTRADOR) || pedido.isUserInRole(Roles.AUTORIDAD))) {
            respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        CambioPasswordDTO datos = new CambioPasswordDTO(pedido.getParameter("actual"), pedido.getParameter("nueva"));
        try {
            continuar(pedido, respuesta, cuenta.cambiarMiPassword(datos));
        } catch (NegocioException e) {
            SesionJsf.redirigir(pedido, respuesta, SesionJsf.CAMBIO + "?error=clave");
        } finally {
            datos.setActual(null);
            datos.setNueva(null);
        }
    }

    private static void continuar(HttpServletRequest pedido, HttpServletResponse respuesta, SesionDTO sesion) {
        descartarVistas(pedido);
        SesionJsf.guardar(pedido, respuesta, sesion.getToken());
        SesionJsf.redirigir(pedido, respuesta, sesion.isDebeCambiarPassword() ? SesionJsf.CAMBIO : "/empresas.xhtml");
    }

    private static void descartarVistas(HttpServletRequest pedido) {
        HttpSession sesion = pedido.getSession(false);
        if (sesion != null) sesion.invalidate();
    }
}
