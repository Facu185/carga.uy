package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.SolicitudesEmpresaLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.NuevaSolicitudEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.SolicitudAltaEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.VinculoEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.RechazoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.*;
import uy.edu.fing.tse.carga.ejb.persistencia.SolicitudAltaEmpresaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.UsuarioDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.CompatibilidadFuncionarioEmpresa;

import java.time.LocalDate;
import java.util.List;

/**
 * Mantiene separadas identidad, empresa y autorización. Ningún ciudadano se
 * vincula a una empresa por sí mismo: el funcionario valida la solicitud.
 */
@Stateless
@DeclareRoles({Roles.CIUDADANO, Roles.CHOFER, Roles.FUNCIONARIO, Roles.AUTORIDAD, Roles.ADMINISTRADOR})
public class SolicitudesEmpresaBean implements SolicitudesEmpresaLocal {
    private static final String ENTIDAD_SOLICITUD = "SolicitudAltaEmpresa";
    private static final String ENTIDAD_VINCULO = "VinculoEmpresaUsuario";
    private static final String ENTIDAD_EMPRESA = "Empresa";
    @Resource SessionContext contexto;
    @PersistenceContext(unitName = "cargaPU") EntityManager em;
    @Inject UsuarioDAO usuarios;
    @Inject SolicitudAltaEmpresaDAO solicitudes;
    @Inject VinculoEmpresaUsuarioDAO vinculos;
    @Inject CompatibilidadFuncionarioEmpresa compatibilidad;
    @EJB AuditoriaLocal auditoria;

    @Override @RolesAllowed({Roles.CIUDADANO, Roles.CHOFER})
    public SolicitudAltaEmpresaDTO solicitarAlta(NuevaSolicitudEmpresaDTO datos) throws NegocioException {
        validarDatosEmpresa(datos);
        String cedula = usuarioActual();
        if (solicitudes.pendientePorUsuario(cedula).isPresent()) throw new NegocioException("Ya tenés una solicitud de alta pendiente.");
        if (existeEmpresa(datos.getNumeroEmpresa())) throw new NegocioException("Ya existe una empresa con ese número.");
        Usuario persona = usuarios.buscarPorCedula(cedula).orElseThrow(() -> new NegocioException("No hay registro del usuario actual."));
        compatibilidad.verificarPuedeSerResponsable(persona);
        SolicitudAltaEmpresa solicitud = solicitudes.guardar(new SolicitudAltaEmpresa(persona, datos.getNombre().trim(),
                textoOpcional(datos.getRazonSocial()), datos.getDireccion().trim(), datos.getNumeroEmpresa()));
        auditoria.registrar(AccionAuditada.SOLICITUD_EMPRESA_CREADA, ENTIDAD_SOLICITUD, solicitud.getId(), null,
                "numeroEmpresa=" + solicitud.getNumeroEmpresa());
        return aDto(solicitud);
    }

    @Override @RolesAllowed({Roles.CIUDADANO, Roles.CHOFER})
    public SolicitudAltaEmpresaDTO miSolicitudAlta() { return solicitudes.ultimaPorUsuario(usuarioActual()).map(SolicitudesEmpresaBean::aDto).orElse(null); }

    @Override @RolesAllowed({Roles.CIUDADANO, Roles.CHOFER})
    public VinculoEmpresaDTO solicitarVinculo(Long empresaId) throws NegocioException {
        if (empresaId == null) throw new NegocioException("La empresa es obligatoria.");
        Empresa empresa = em.find(Empresa.class, empresaId);
        if (empresa == null || !empresa.isActiva()) throw new NegocioException("La empresa no existe o está inactiva.");
        Usuario persona = usuarios.buscarPorCedula(usuarioActual()).orElseThrow(() -> new NegocioException("No hay registro del usuario actual."));
        compatibilidad.verificarPuedeSerResponsable(persona);
        if (vinculos.pendiente(persona.getId(), empresaId).isPresent()) throw new NegocioException("Ya tenés una solicitud pendiente para esta empresa.");
        VinculoEmpresaUsuario vinculo = vinculos.guardar(new VinculoEmpresaUsuario(persona, empresa, TipoVinculoEmpresa.RESPONSABLE));
        auditoria.registrar(AccionAuditada.VINCULO_EMPRESA_SOLICITADO, ENTIDAD_VINCULO, vinculo.getId(), null,
                "empresaId=" + empresaId + ",tipo=RESPONSABLE");
        return aDto(vinculo);
    }

    @Override @RolesAllowed({Roles.CIUDADANO, Roles.CHOFER})
    public List<VinculoEmpresaDTO> misVinculos() { return vinculos.deUsuario(usuarioActual()).stream().map(SolicitudesEmpresaBean::aDto).toList(); }

    @Override @RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
    public List<SolicitudAltaEmpresaDTO> listarSolicitudes(String estado) throws NegocioException {
        EstadoSolicitudEmpresa filtro = estadoSolicitud(estado);
        return solicitudes.listar(filtro).stream().map(SolicitudesEmpresaBean::aDto).toList();
    }

    @Override @RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
    public List<VinculoEmpresaDTO> listarVinculos(String estado) throws NegocioException {
        return vinculos.listar(estadoVinculo(estado)).stream().map(SolicitudesEmpresaBean::aDto).toList();
    }

    @Override @RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
    public SolicitudAltaEmpresaDTO aprobarAlta(Long id) throws NegocioException {
        SolicitudAltaEmpresa solicitud = pendiente(id);
        compatibilidad.verificarPuedeSerResponsable(solicitud.getSolicitante());
        if (existeEmpresa(solicitud.getNumeroEmpresa())) throw new NegocioException("Ya existe una empresa con ese número.");
        EmpresaDTO datos = new EmpresaDTO(solicitud.getNombre(), solicitud.getRazonSocial(), solicitud.getNumeroEmpresa(), LocalDate.now());
        datos.setDireccion(solicitud.getDireccion());
        datos.setActiva(true);
        Empresa empresa = Empresa.desdeDTO(datos);
        em.persist(empresa); em.flush();
        String aprobador = usuarioActual();
        solicitud.aprobar(aprobador); solicitud.asociarEmpresa(empresa);
        VinculoEmpresaUsuario vinculo = vinculos.guardar(new VinculoEmpresaUsuario(solicitud.getSolicitante(), empresa, TipoVinculoEmpresa.RESPONSABLE));
        vinculo.aprobar(aprobador);
        auditoria.registrar(AccionAuditada.SOLICITUD_EMPRESA_APROBADA, ENTIDAD_SOLICITUD, solicitud.getId(), null,
                "empresaId=" + empresa.getId());
        auditoria.registrar(AccionAuditada.VINCULO_EMPRESA_APROBADO, ENTIDAD_VINCULO, vinculo.getId(), null,
                "empresaId=" + empresa.getId() + ",tipo=RESPONSABLE");
        return aDto(solicitud);
    }

    @Override @RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
    public SolicitudAltaEmpresaDTO rechazarAlta(Long id, RechazoDTO datos) throws NegocioException {
        String motivo = motivo(datos);
        SolicitudAltaEmpresa solicitud = pendiente(id);
        solicitud.rechazar(usuarioActual(), motivo);
        auditoria.registrar(AccionAuditada.SOLICITUD_EMPRESA_RECHAZADA, ENTIDAD_SOLICITUD, id, motivo, null);
        return aDto(solicitud);
    }

    @Override @RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
    public VinculoEmpresaDTO aprobarVinculo(Long id) throws NegocioException {
        VinculoEmpresaUsuario vinculo = vinculoPendiente(id);
        if (!vinculo.getEmpresa().isActiva()) throw new NegocioException("No se puede vincular a una empresa inactiva.");

        if (vinculo.getTipo() == TipoVinculoEmpresa.RESPONSABLE) {
            compatibilidad.verificarPuedeSerResponsable(vinculo.getUsuario());
        }
        if (vinculo.getTipo() == TipoVinculoEmpresa.RESPONSABLE && hayResponsableActivo(vinculo.getEmpresa().getId()))
            throw new NegocioException("La empresa ya tiene un responsable activo; primero transferí o revocá esa responsabilidad.");
        vinculo.aprobar(usuarioActual());
        auditoria.registrar(AccionAuditada.VINCULO_EMPRESA_APROBADO, ENTIDAD_VINCULO, id, null, "empresaId=" + vinculo.getEmpresa().getId());
        return aDto(vinculo);
    }

    /** Aprueba al nuevo responsable y revoca al anterior dentro de la misma transacción. */
    @Override @RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
    public VinculoEmpresaDTO transferirResponsabilidad(Long id, RechazoDTO datos) throws NegocioException {
        String motivo = motivo(datos);
        VinculoEmpresaUsuario nuevo = vinculoPendiente(id);
        if (nuevo.getTipo() != TipoVinculoEmpresa.RESPONSABLE) throw new NegocioException("El vínculo no corresponde a un responsable.");
        if (!nuevo.getEmpresa().isActiva()) throw new NegocioException("No se puede transferir una empresa inactiva.");
        compatibilidad.verificarPuedeSerResponsable(nuevo.getUsuario());
        String aprobador = usuarioActual();
        for (VinculoEmpresaUsuario actual : vinculos.activosDeEmpresa(nuevo.getEmpresa().getId())) {
            if (actual.getTipo() == TipoVinculoEmpresa.RESPONSABLE) actual.revocar(aprobador, "Transferencia: " + motivo);
        }
        nuevo.aprobar(aprobador);
        auditoria.registrar(AccionAuditada.VINCULO_EMPRESA_TRANSFERIDO, ENTIDAD_VINCULO, id, motivo,
                "transferencia=true,empresaId=" + nuevo.getEmpresa().getId());
        return aDto(nuevo);
    }

    @Override @RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
    public VinculoEmpresaDTO rechazarVinculo(Long id, RechazoDTO datos) throws NegocioException {
        String motivo = motivo(datos);
        VinculoEmpresaUsuario vinculo = vinculoPendiente(id);
        vinculo.rechazar(usuarioActual(), motivo);
        auditoria.registrar(AccionAuditada.VINCULO_EMPRESA_RECHAZADO, ENTIDAD_VINCULO, id, motivo, null);
        return aDto(vinculo);
    }

    @Override @RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
    public void darDeBajaEmpresa(Long empresaId, RechazoDTO datos) throws NegocioException {
        String motivo = motivo(datos);
        Empresa empresa = em.find(Empresa.class, empresaId);
        if (empresa == null) throw new NegocioException("La empresa no existe.");
        if (!empresa.isActiva()) throw new NegocioException("La empresa ya está inactiva.");
        empresa.setActiva(false);
        String aprobador = usuarioActual();
        for (VinculoEmpresaUsuario vinculo : vinculos.activosDeEmpresa(empresaId)) vinculo.revocar(aprobador, motivo);
        auditoria.registrar(AccionAuditada.EMPRESA_DADA_DE_BAJA, ENTIDAD_EMPRESA, empresaId, motivo, "vinculosRevocados=true");
    }

    private SolicitudAltaEmpresa pendiente(Long id) throws NegocioException {
        SolicitudAltaEmpresa solicitud = solicitudes.buscarPorId(id).orElseThrow(() -> new NegocioException("La solicitud no existe."));
        if (!solicitud.estaPendiente()) throw new NegocioException("La solicitud ya fue resuelta.");
        return solicitud;
    }
    private VinculoEmpresaUsuario vinculoPendiente(Long id) throws NegocioException {
        VinculoEmpresaUsuario vinculo = vinculos.buscarPorId(id).orElseThrow(() -> new NegocioException("El vínculo no existe."));
        if (!vinculo.estaPendiente()) throw new NegocioException("El vínculo ya fue resuelto.");
        return vinculo;
    }
    private boolean existeEmpresa(long numero) { return em.createQuery("select count(e) from Empresa e where e.numeroEmpresa = :numero", Long.class).setParameter("numero", numero).getSingleResult() > 0; }
    private boolean hayResponsableActivo(Long empresaId) { return em.createQuery("select count(v) from VinculoEmpresaUsuario v where v.empresa.id = :empresa and v.tipo = :tipo and v.estado = :estado", Long.class).setParameter("empresa", empresaId).setParameter("tipo", TipoVinculoEmpresa.RESPONSABLE).setParameter("estado", EstadoVinculoEmpresa.ACTIVO).getSingleResult() > 0; }
    private String usuarioActual() { return contexto.getCallerPrincipal().getName(); }
    private static void validarDatosEmpresa(NuevaSolicitudEmpresaDTO datos) throws NegocioException { if(datos==null || datos.getNombre()==null || datos.getNombre().isBlank()) throw new NegocioException("El nombre de la empresa es obligatorio."); if(datos.getDireccion()==null || datos.getDireccion().isBlank()) throw new NegocioException("La dirección de la empresa es obligatoria."); if(datos.getNumeroEmpresa()<=0) throw new NegocioException("El número de empresa debe ser positivo."); }
    private static String textoOpcional(String texto) { return texto == null || texto.isBlank() ? null : texto.trim(); }
    private static String motivo(RechazoDTO datos) throws NegocioException { if(datos==null || datos.getMotivo()==null || datos.getMotivo().isBlank()) throw new NegocioException("Indicá el motivo."); String motivo=datos.getMotivo().trim(); return motivo.length() <= 500 ? motivo : motivo.substring(0,500); }
    private static EstadoSolicitudEmpresa estadoSolicitud(String estado) throws NegocioException { if(estado==null || estado.isBlank()) return null; try{return EstadoSolicitudEmpresa.valueOf(estado.trim().toUpperCase());}catch(IllegalArgumentException e){throw new NegocioException("Estado inválido: " + estado);}}
    private static EstadoVinculoEmpresa estadoVinculo(String estado) throws NegocioException { if(estado==null || estado.isBlank()) return null; try{return EstadoVinculoEmpresa.valueOf(estado.trim().toUpperCase());}catch(IllegalArgumentException e){throw new NegocioException("Estado inválido: " + estado);}}
    static SolicitudAltaEmpresaDTO aDto(SolicitudAltaEmpresa s) { Usuario p=s.getSolicitante(); return new SolicitudAltaEmpresaDTO(s.getId(),p.getCedula(),p.getNombre(),s.getNombre(),s.getRazonSocial(),s.getDireccion(),s.getNumeroEmpresa(),s.getEstado().name(),s.getCreadaEn().toString(),s.getResueltaEn()==null?null:s.getResueltaEn().toString(),s.getResueltaPor(),s.getMotivoRechazo(),s.getEmpresa()==null?null:s.getEmpresa().getId()); }
    static VinculoEmpresaDTO aDto(VinculoEmpresaUsuario v) { Usuario solicitante=v.getUsuario(); return new VinculoEmpresaDTO(v.getId(),v.getEmpresa().getId(),v.getEmpresa().getNombre(),v.getTipo().name(),v.getEstado().name(),v.getSolicitadoEn().toString(),v.getAprobadoEn()==null?null:v.getAprobadoEn().toString(),v.getAprobadoPor(),v.getRevocadoEn()==null?null:v.getRevocadoEn().toString(),v.getRevocadoPor(),v.getMotivoRevocacion(),solicitante.getCedula(),solicitante.getNombre()); }
}
