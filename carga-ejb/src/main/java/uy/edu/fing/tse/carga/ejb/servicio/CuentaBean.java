package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.CuentaLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.api.dto.cuenta.CambioPasswordDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.SesionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.persistencia.CuentaInternaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.UsuarioDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.HashContrasenas;
import uy.edu.fing.tse.carga.ejb.seguridad.PoliticaContrasenas;
import uy.edu.fing.tse.carga.ejb.seguridad.TokenService;

/** Lo que cada persona hace sobre su propia sesion o cuenta. */
@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
public class CuentaBean implements CuentaLocal {

    @Resource
    SessionContext contexto;

    @Inject
    CuentaInternaDAO cuentas;

    @Inject
    UsuarioDAO usuarios;

    @Inject
    TokenService tokenService;

    @EJB
    AuditoriaLocal auditoria;

    /**
     * Cambiar la clave cierra las demas sesiones abiertas (sube la version de credenciales).
     * Por eso devuelve un token nuevo: quien la cambio sigue trabajando sin volver a ingresar.
     */
    @Override
    @RolesAllowed({Roles.ADMINISTRADOR, Roles.AUTORIDAD})
    public SesionDTO cambiarMiPassword(CambioPasswordDTO datos) throws NegocioException {
        String nombreUsuario = contexto.getCallerPrincipal().getName();
        CuentaInterna cuenta = cuentas.buscarPorUsuario(nombreUsuario)
                .orElseThrow(() -> new NegocioException("No se encontr\u00f3 tu usuario"));
        if (cuenta.esDeLdap()) {
            throw new NegocioException("Tu contrase\u00f1a se gestiona en el directorio LDAP del MTOP, no en carga.uy");
        }
        if (datos == null || datos.getActual() == null
                || !HashContrasenas.verificar(datos.getActual(), cuenta.getPasswordHash())) {
            throw new NegocioException("La contrase\u00f1a actual no es correcta");
        }
        PoliticaContrasenas.validar(datos.getNueva());
        if (HashContrasenas.verificar(datos.getNueva(), cuenta.getPasswordHash())) {
            throw new NegocioException("La nueva contrase\u00f1a tiene que ser distinta de la actual");
        }
        cuenta.cambiarPassword(HashContrasenas.generar(datos.getNueva()), false);
        auditoria.registrar(AccionAuditada.PASSWORD_CAMBIADA, AutenticacionBean.ENTIDAD, nombreUsuario, null, null);
        return AutenticacionBean.sesion(cuenta, tokenService);
    }

    /**
     * Solo se puede pedir con una sesion todavia valida (la capa web ya verifico que no este revocada),
     * y siempre lo pide la persona desde el aviso de vencimiento: nunca se renueva sola.
     */
    @Override
    @RolesAllowed({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
    public SesionDTO renovar() throws NegocioException {
        String usuario = contexto.getCallerPrincipal().getName();
        if (contexto.isCallerInRole(Roles.ADMINISTRADOR) || contexto.isCallerInRole(Roles.AUTORIDAD)) {
            CuentaInterna cuenta = cuentas.buscarPorUsuario(usuario)
                    .filter(CuentaInterna::isActivo)
                    .orElseThrow(() -> new NegocioException("Tu usuario ya no est\u00e1 activo"));
            return AutenticacionBean.sesion(cuenta, tokenService);
        }
        // Ingreso con gub.uy: los roles se vuelven a leer, por si cambiaron desde el ingreso
        Usuario persona = usuarios.buscarPorCedula(usuario)
                .orElseThrow(() -> new NegocioException("No hay registro del usuario actual"));
        return IngresoGubUyBean.sesion(persona, tokenService);
    }
}
