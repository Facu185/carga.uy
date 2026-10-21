package uy.edu.fing.tse.carga.web.seguridad;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uy.edu.fing.tse.carga.api.contract.service.AutenticacionLocal;
import uy.edu.fing.tse.carga.api.dto.cuenta.DatosSesionDTO;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.util.Set;

/**
 * Lee "Authorization: Bearer token" en cada pedido y, si el token es valido y la sesion sigue vigente,
 * le informa al contenedor quien es y con que roles. A partir de ahi @RolesAllowed funciona en los EJB.
 * Las vistas P2 tambien aceptan la cookie del login JSF. Esa cookie NO autentica REST, SOAP ni P4.
 * El acceso web se comprueba antes de construir los backing beans; los EJB conservan sus permisos.
 */
@ApplicationScoped
public class MecanismoJwt implements HttpAuthenticationMechanism {

    private static final String PREFIJO = "Bearer ";
    /** Con clave temporal solo se puede cambiarla (y consultar quien soy). */
    private static final Set<String> PERMITIDOS_CON_CLAVE_TEMPORAL = Set.of("/auth/cambiar-password", "/auth/yo");

    @EJB
    AutenticacionLocal autenticacion;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
                                                HttpMessageContext contexto) {
        boolean jsf = SesionJsf.esRuta(request);
        if (jsf) {
            SesionJsf.noGuardarCache(response);
            if (!SesionJsf.origenPermitido(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return AuthenticationStatus.SEND_FAILURE;
            }
            // Permite login/salida aun si quedo una cookie vencida o una clave temporal.
            if (Set.of("/jsf/ingresar", "/jsf/salir", SesionJsf.INGRESO).contains(request.getServletPath())) {
                return contexto.doNothing();
            }
        }
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith(PREFIJO)
                ? header.substring(PREFIJO.length()).trim()
                : header == null && jsf ? SesionJsf.token(request) : null;
        if (token == null) {
            return jsf && !SesionJsf.esPublica(request)
                    ? SesionJsf.redirigir(request, response, SesionJsf.INGRESO) : contexto.doNothing();
        }
        DatosSesionDTO sesion = autenticacion.validarToken(token);
        if (sesion == null) {
            if (jsf) {
                SesionJsf.borrar(request, response);
                return SesionJsf.esPublica(request) ? contexto.doNothing()
                        : SesionJsf.redirigir(request, response, SesionJsf.INGRESO);
            }
            return contexto.responseUnauthorized();
        }
        if (jsf) {
            boolean cambio = Set.of(SesionJsf.CAMBIO, "/jsf/cambiar-clave").contains(request.getServletPath());
            if (sesion.isDebeCambiarPassword() && !cambio) {
                return SesionJsf.redirigir(request, response, SesionJsf.CAMBIO);
            }
            if (!rolesPermitidos(request.getServletPath(), sesion.getRoles())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return AuthenticationStatus.SEND_FAILURE;
            }
            return contexto.notifyContainerAboutLogin(sesion.getUsuario(), sesion.getRoles());
        }
        if (sesion.isDebeCambiarPassword() && !permitidoConClaveTemporal(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return AuthenticationStatus.SEND_FAILURE;
        }
        return contexto.notifyContainerAboutLogin(sesion.getUsuario(), sesion.getRoles());
    }

    private static boolean rolesPermitidos(String ruta, Set<String> roles) {
        if (roles == null) return false;
        Set<String> permitidos = switch (ruta) {
            case "/vehiculos.xhtml", "/choferes.xhtml" -> Set.of(Roles.ADMINISTRADOR, Roles.CIUDADANO, Roles.CHOFER);
            case "/cambiar-clave-jsf.xhtml", "/jsf/cambiar-clave" -> Set.of(Roles.ADMINISTRADOR, Roles.AUTORIDAD);
            default -> Set.of(Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CIUDADANO, Roles.CHOFER);
        };
        return roles.stream().anyMatch(permitidos::contains);
    }

    private static boolean permitidoConClaveTemporal(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && PERMITIDOS_CON_CLAVE_TEMPORAL.stream().anyMatch(uri::endsWith);
    }
}
