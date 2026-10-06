package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.contract.service.PerfilLocal;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.api.dto.cuenta.PerfilDTO;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.persistencia.UsuarioDAO;

import java.time.LocalDate;

/** Perfil de quien esta logueado con gub.uy. Otros frentes lo extienden (empresa, vehiculos). */
@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
@RolesAllowed({Roles.CIUDADANO, Roles.FUNCIONARIO, Roles.CHOFER})
public class PerfilBean implements PerfilLocal {

    @Resource
    SessionContext contexto;

    @Inject
    UsuarioDAO usuarios;

    @Override
    public PerfilDTO miPerfil() throws NegocioException {
        // El usuario de una sesion de gub.uy es la cedula: nadie puede pedir el perfil de otro
        String cedula = contexto.getCallerPrincipal().getName();
        Usuario u = usuarios.buscarPorCedula(cedula)
                .orElseThrow(() -> new NegocioException("No hay perfil para el usuario actual"));
        return new PerfilDTO(
                u.getCedula(),
                u.getNombre(),
                u.getCorreo(),
                u.getFechaNacimiento() == null ? null : u.getFechaNacimiento().toString(),
                u.esMayorDeEdad(LocalDate.now()),
                u.estaVerificadoConDnic() ? "VERIFICADO" : "PENDIENTE");
    }
}
