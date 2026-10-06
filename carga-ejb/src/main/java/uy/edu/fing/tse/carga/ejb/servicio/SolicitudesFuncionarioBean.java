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
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.api.contract.service.SolicitudesFuncionarioLocal;
import uy.edu.fing.tse.carga.api.dto.funcionario.NuevaSolicitudDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.RechazoDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.SolicitudFuncionarioDTO;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoSolicitud;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.SolicitudFuncionario;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.persistencia.FuncionarioDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.SolicitudFuncionarioDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.UsuarioDAO;

import java.util.List;

/**
 * Flujo de acceso de funcionarios:
 * 1. La persona entra con gub.uy (queda identificada) y pide acceso indicando su dependencia.
 * 2. Un administrador aprueba (se crea o reactiva el funcionario) o rechaza con motivo.
 * El administrador nunca tipea cedula ni nombre: vienen verificados de gub.uy.
 */
@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
public class SolicitudesFuncionarioBean implements SolicitudesFuncionarioLocal {

    private static final String ENTIDAD = "SolicitudFuncionario";

    @Resource
    SessionContext contexto;

    @Inject
    SolicitudFuncionarioDAO solicitudes;

    @Inject
    UsuarioDAO usuarios;

    @Inject
    FuncionarioDAO funcionarios;

    @EJB
    AuditoriaLocal auditoria;

    // ---------------- lado del solicitante ----------------

    @Override
    @RolesAllowed(Roles.CIUDADANO)
    public SolicitudFuncionarioDTO solicitar(NuevaSolicitudDTO datos) throws NegocioException {
        if (datos == null || datos.getDependencia() == null || datos.getDependencia().isBlank()) {
            throw new NegocioException("Indic\u00e1 la dependencia del MTOP donde trabaj\u00e1s");
        }
        String cedula = usuarioActual();
        Usuario solicitante = usuarios.buscarPorCedula(cedula)
                .orElseThrow(() -> new NegocioException("No hay registro del usuario actual"));
        if (funcionarios.buscarPorCedula(cedula).map(Funcionario::isActivo).orElse(false)) {
            throw new NegocioException("Ya ten\u00e9s acceso como funcionario");
        }
        if (solicitudes.ultimaPorCedula(cedula).map(SolicitudFuncionario::estaPendiente).orElse(false)) {
            throw new NegocioException("Ya ten\u00e9s una solicitud pendiente");
        }
        SolicitudFuncionario nueva = solicitudes.guardar(
                new SolicitudFuncionario(solicitante, FuncionariosBean.dependencia(datos.getDependencia())));
        auditoria.registrar(AccionAuditada.SOLICITUD_FUNCIONARIO_CREADA, ENTIDAD, nueva.getId(), null,
                "dependencia=" + nueva.getDependencia());
        return aDto(nueva);
    }

    @Override
    @RolesAllowed({Roles.CIUDADANO, Roles.FUNCIONARIO})
    public SolicitudFuncionarioDTO miSolicitud() {
        return solicitudes.ultimaPorCedula(usuarioActual()).map(SolicitudesFuncionarioBean::aDto).orElse(null);
    }

    // ---------------- lado del administrador ----------------

    @Override
    @RolesAllowed(Roles.ADMINISTRADOR)
    public List<SolicitudFuncionarioDTO> listar(String estado) throws NegocioException {
        EstadoSolicitud filtro = null;
        if (estado != null && !estado.isBlank()) {
            try {
                filtro = EstadoSolicitud.valueOf(estado.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new NegocioException("Estado inv\u00e1lido: " + estado);
            }
        }
        return solicitudes.listar(filtro).stream().map(SolicitudesFuncionarioBean::aDto).toList();
    }

    @Override
    @RolesAllowed(Roles.ADMINISTRADOR)
    public SolicitudFuncionarioDTO aprobar(Long id) throws NegocioException {
        SolicitudFuncionario solicitud = pendiente(id);
        solicitud.aprobar(usuarioActual());

        Usuario persona = solicitud.getSolicitante();
        Funcionario funcionario = funcionarios.buscarPorCedula(persona.getCedula())
                .orElseGet(() -> funcionarios.guardar(new Funcionario(persona, solicitud.getDependencia())));
        funcionario.setActivo(true);
        funcionario.setDependencia(solicitud.getDependencia());
        persona.asignarRol(usuarios.rol(Rol.FUNCIONARIO));
        auditoria.registrar(AccionAuditada.SOLICITUD_FUNCIONARIO_APROBADA, ENTIDAD, id, null,
                "cedula=" + persona.getCedula());
        return aDto(solicitud);
    }

    @Override
    @RolesAllowed(Roles.ADMINISTRADOR)
    public SolicitudFuncionarioDTO rechazar(Long id, RechazoDTO datos) throws NegocioException {
        if (datos == null || datos.getMotivo() == null || datos.getMotivo().isBlank()) {
            throw new NegocioException("Indic\u00e1 el motivo del rechazo");
        }
        SolicitudFuncionario solicitud = pendiente(id);
        String motivo = recortar(datos.getMotivo().trim(), 500);
        solicitud.rechazar(usuarioActual(), motivo);
        auditoria.registrar(AccionAuditada.SOLICITUD_FUNCIONARIO_RECHAZADA, ENTIDAD, id, motivo,
                "cedula=" + solicitud.getSolicitante().getCedula());
        return aDto(solicitud);
    }

    // ---------------- utilidades ----------------

    private SolicitudFuncionario pendiente(Long id) throws NegocioException {
        SolicitudFuncionario s = solicitudes.buscarPorId(id)
                .orElseThrow(() -> new NegocioException("La solicitud no existe"));
        if (!s.estaPendiente()) {
            throw new NegocioException("La solicitud ya fue resuelta");
        }
        return s;
    }

    private String usuarioActual() {
        return contexto.getCallerPrincipal().getName();
    }

    static SolicitudFuncionarioDTO aDto(SolicitudFuncionario s) {
        Usuario p = s.getSolicitante();
        return new SolicitudFuncionarioDTO(s.getId(), p.getCedula(), p.getNombre(), p.getCorreo(), s.getDependencia(),
                s.getEstado().name(), s.getCreadaEn().toString(),
                s.getResueltaEn() == null ? null : s.getResueltaEn().toString(),
                s.getResueltaPor(), s.getMotivoRechazo());
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
