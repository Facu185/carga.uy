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

import java.util.Set;

/**
 * Lee "Authorization: Bearer token" en cada pedido y, si el token es valido y la sesion sigue vigente,
 * le informa al contenedor quien es y con que roles. A partir de ahi @RolesAllowed funciona en los EJB.
 * Sin header no autentica pero deja pasar: lo publico sigue accesible (salud, tracking, JSF, SOAP)
 * y lo protegido lo corta la capa de servicios.
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
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIJO)) {
            return contexto.doNothing();
        }
        DatosSesionDTO sesion = autenticacion.validarToken(header.substring(PREFIJO.length()).trim());
        if (sesion == null) {
            return contexto.responseUnauthorized();
        }
        if (sesion.isDebeCambiarPassword() && !permitidoConClaveTemporal(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return AuthenticationStatus.SEND_FAILURE;
        }
        return contexto.notifyContainerAboutLogin(sesion.getUsuario(), sesion.getRoles());
    }

    private static boolean permitidoConClaveTemporal(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && PERMITIDOS_CON_CLAVE_TEMPORAL.stream().anyMatch(uri::endsWith);
    }
}
