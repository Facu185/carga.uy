package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.FuncionariosLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.api.dto.funcionario.CambioDependenciaDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.FuncionarioDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.NuevoFuncionarioDTO;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.modelo.ValidadorCedula;
import uy.edu.fing.tse.carga.ejb.persistencia.FuncionarioDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.UsuarioDAO;

import java.util.List;

/**
 * Funcionarios del MTOP. Nombre y correo vienen de gub.uy (fuente de verdad) y no se editan aca;
 * el administrador gestiona el acceso (alta, baja) y la dependencia. Solo administradores.
 * El rol FUNCIONARIO de usuario_rol se mantiene alineado con el estado del funcionario.
 */
@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
@RolesAllowed(Roles.ADMINISTRADOR)
public class FuncionariosBean implements FuncionariosLocal {

    static final int LARGO_MAXIMO_DEPENDENCIA = 150;
    private static final String ENTIDAD = "Funcionario";

    @Inject
    FuncionarioDAO dao;

    @Inject
    UsuarioDAO usuarios;

    @EJB
    AuditoriaLocal auditoria;

    @Override
    public List<FuncionarioDTO> listar() {
        return dao.listar().stream().map(FuncionariosBean::aDto).toList();
    }

    @Override
    public FuncionarioDTO crear(NuevoFuncionarioDTO datos) throws NegocioException {
        if (datos == null || datos.getNombre() == null || datos.getNombre().isBlank()) {
            throw new NegocioException("El nombre es obligatorio");
        }
        String cedula = ValidadorCedula.normalizar(datos.getCedula());
        if (cedula == null || !ValidadorCedula.esValida(cedula)) {
            throw new NegocioException("La c\u00e9dula no es v\u00e1lida");
        }
        if (dao.buscarPorCedula(cedula).isPresent()) {
            throw new NegocioException("Ya existe un funcionario con esa c\u00e9dula");
        }
        // Si ya ingreso alguna vez con gub.uy se usa su registro; si no, se crea y gub.uy lo completa al ingresar
        Usuario persona = usuarios.buscarPorCedula(cedula)
                .orElseGet(() -> usuarios.guardar(new Usuario(cedula, datos.getNombre().trim(), null)));
        Funcionario guardado = dao.guardar(new Funcionario(persona, dependencia(datos.getDependencia())));
        persona.asignarRol(usuarios.rol(Rol.FUNCIONARIO));
        auditoria.registrar(AccionAuditada.FUNCIONARIO_HABILITADO, ENTIDAD, cedula, null,
                "nombre=" + persona.getNombre() + ", alta manual");
        return aDto(guardado);
    }

    @Override
    public FuncionarioDTO cambiarEstado(Long id, boolean activo) throws NegocioException {
        Funcionario f = buscar(id);
        f.setActivo(activo);
        if (activo) {
            f.getUsuario().asignarRol(usuarios.rol(Rol.FUNCIONARIO));
        } else {
            f.getUsuario().quitarRol(Rol.FUNCIONARIO);
        }
        auditoria.registrar(AccionAuditada.FUNCIONARIO_ESTADO_CAMBIADO, ENTIDAD, f.getCedula(), null,
                "activo=" + activo);
        return aDto(f);
    }

    @Override
    public FuncionarioDTO cambiarDependencia(Long id, CambioDependenciaDTO datos) throws NegocioException {
        if (datos == null || datos.getDependencia() == null || datos.getDependencia().isBlank()) {
            throw new NegocioException("Indic\u00e1 la dependencia");
        }
        Funcionario f = buscar(id);
        String anterior = f.getDependencia();
        f.setDependencia(dependencia(datos.getDependencia()));
        auditoria.registrar(AccionAuditada.FUNCIONARIO_DEPENDENCIA_CAMBIADA, ENTIDAD, f.getCedula(), null,
                "antes=" + anterior + ", ahora=" + f.getDependencia());
        return aDto(f);
    }

    private Funcionario buscar(Long id) throws NegocioException {
        return dao.buscarPorId(id).orElseThrow(() -> new NegocioException("Funcionario inexistente"));
    }

    static FuncionarioDTO aDto(Funcionario f) {
        return new FuncionarioDTO(f.getId(), f.getCedula(), f.getNombre(), f.getUsuario().getCorreo(),
                f.getDependencia(), f.isActivo());
    }

    static String dependencia(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.length() <= LARGO_MAXIMO_DEPENDENCIA ? limpio : limpio.substring(0, LARGO_MAXIMO_DEPENDENCIA);
    }
}
