package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.AccionAuditada;
import uy.edu.fing.tse.carga.api.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.Roles;
import uy.edu.fing.tse.carga.api.dto.PaginaAuditoriaDTO;
import uy.edu.fing.tse.carga.api.dto.RegistroAuditoriaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.RegistroAuditoria;
import uy.edu.fing.tse.carga.ejb.persistencia.RegistroAuditoriaDAO;

import java.security.Principal;
import java.util.List;

/**
 * Punto unico para registrar en la pista de auditoria. El usuario y el rol los toma del contenedor
 * (de la sesion), no de parametros: nadie puede registrar una accion a nombre de otro.
 */
@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
public class AuditoriaBean implements AuditoriaLocal {

    static final String USUARIO_SISTEMA = "SISTEMA";
    static final int TAMANO_POR_DEFECTO = 50;
    static final int TAMANO_MAXIMO = 100;
    /** Si alguien tiene varios roles, queda registrado el de mayor nivel. */
    private static final List<String> ROLES = List.of(
            Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO);

    @Resource
    SessionContext contexto;

    @Inject
    RegistroAuditoriaDAO dao;

    /** MANDATORY: se llama dentro de la transaccion de la accion que se audita. */
    @Override
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void registrar(AccionAuditada accion, String entidad, Object entidadId, String fundamento, String detalle)
            throws NegocioException {
        exigirDatosMinimos(accion, entidad, entidadId);
        if (accion.requiereFundamento() && (fundamento == null || fundamento.isBlank())) {
            throw new NegocioException("La acci\u00f3n " + accion + " requiere indicar el fundamento");
        }
        dao.agregar(new RegistroAuditoria(usuarioActual(), rolActual(), accion, entidad,
                recortar(String.valueOf(entidadId), 60), fundamento, detalle));
    }

    @Override
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void registrarComo(String usuario, String rol, AccionAuditada accion, String entidad, Object entidadId,
                              String fundamento, String detalle) {
        exigirDatosMinimos(accion, entidad, entidadId);
        String quien = usuario == null || usuario.isBlank() ? "desconocido" : recortar(usuario.trim(), 100);
        dao.agregar(new RegistroAuditoria(quien, rol, accion, entidad, recortar(String.valueOf(entidadId), 60),
                fundamento, detalle));
    }

    /** Paginada: la pista crece con cada accion (y mucho, cuando entren los casos de fiscalizacion). */
    @Override
    @RolesAllowed({Roles.ADMINISTRADOR, Roles.AUTORIDAD})
    public PaginaAuditoriaDTO buscar(String usuario, String entidad, String entidadId, Integer pagina, Integer tamano) {
        int porPagina = tamano == null || tamano <= 0 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        int numero = pagina == null || pagina < 0 ? 0 : pagina;
        long total = dao.contar(usuario, entidad, entidadId);
        List<RegistroAuditoriaDTO> registros = dao.buscar(usuario, entidad, entidadId, numero * porPagina, porPagina)
                .stream().map(AuditoriaBean::aDto).toList();
        return new PaginaAuditoriaDTO(registros, numero, porPagina, total);
    }

    static RegistroAuditoriaDTO aDto(RegistroAuditoria r) {
        return new RegistroAuditoriaDTO(r.getId(), r.getInstante().toString(), r.getUsuario(), r.getRol(),
                r.getAccion(), r.getEntidad(), r.getEntidadId(), r.getFundamento(), r.getDetalle());
    }

    private static void exigirDatosMinimos(AccionAuditada accion, String entidad, Object entidadId) {
        if (accion == null || entidad == null || entidad.isBlank() || entidadId == null) {
            throw new IllegalArgumentException("Acci\u00f3n, entidad e id son obligatorios para auditar");
        }
    }

    private String usuarioActual() {
        Principal p = contexto.getCallerPrincipal();
        // Sin usuario (por ejemplo, una deteccion automatica en segundo plano) queda como SISTEMA
        return p == null || p.getName() == null || "anonymous".equals(p.getName()) ? USUARIO_SISTEMA : p.getName();
    }

    private String rolActual() {
        return ROLES.stream().filter(contexto::isCallerInRole).findFirst().orElse(null);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
