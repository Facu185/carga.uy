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
import uy.edu.fing.tse.carga.api.contract.service.CuentasInternasLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.api.dto.cuenta.CambioRolDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.CuentaInternaDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.NuevaCuentaInternaDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.PasswordTemporalDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.persistencia.CuentaInternaDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.HashContrasenas;
import uy.edu.fing.tse.carga.ejb.seguridad.PoliticaContrasenas;

import java.time.Instant;
import java.util.List;

/**
 * Gestion de cuentas del backoffice. El control de acceso esta aca (capa de servicios), no en el
 * frontend: aunque alguien llame a la API directo, sin rol ADMINISTRADOR recibe 403.
 * Reglas: siempre queda al menos un administrador activo, y nadie se da de baja ni se cambia el rol a si mismo.
 */
@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
@RolesAllowed(Roles.ADMINISTRADOR)
public class CuentasInternasBean implements CuentasInternasLocal {

    private static final String ENTIDAD = AutenticacionBean.ENTIDAD;

    @Resource
    SessionContext contexto;

    @Inject
    CuentaInternaDAO dao;

    @EJB
    AuditoriaLocal auditoria;

    @Override
    public List<CuentaInternaDTO> listar() {
        return dao.listar().stream().map(CuentasInternasBean::aDto).toList();
    }

    @Override
    public CuentaInternaDTO crear(NuevaCuentaInternaDTO datos) throws NegocioException {
        if (datos == null || vacio(datos.getUsuario()) || vacio(datos.getNombre())) {
            throw new NegocioException("Usuario y nombre son obligatorios");
        }
        PoliticaContrasenas.validar(datos.getPassword());
        Rol rol = parsearRolInterno(datos.getRol());
        String usuario = datos.getUsuario().trim();
        if (dao.buscarPorUsuario(usuario).isPresent()) {
            throw new NegocioException("Ya existe un usuario con ese nombre");
        }
        CuentaInterna guardada = dao.guardar(new CuentaInterna(usuario, datos.getNombre().trim(),
                HashContrasenas.generar(datos.getPassword()), rol));
        auditoria.registrar(AccionAuditada.USUARIO_INTERNO_CREADO, ENTIDAD, guardada.getNombreUsuario(), null,
                "rol=" + rol);
        return aDto(guardada);
    }

    /** Baja logica: se conserva por la auditoria. Cierra sus sesiones en el acto. */
    @Override
    public CuentaInternaDTO cambiarEstado(Long id, boolean activo) throws NegocioException {
        CuentaInterna cuenta = buscar(id);
        if (!activo) {
            noSobreSiMisma(cuenta, "No pod\u00e9s darte de baja a vos mismo");
            protegerUltimoAdministrador(cuenta);
        }
        cuenta.setActivo(activo);
        auditoria.registrar(AccionAuditada.USUARIO_INTERNO_ESTADO_CAMBIADO, ENTIDAD, cuenta.getNombreUsuario(), null,
                "activo=" + activo);
        return aDto(cuenta);
    }

    @Override
    public CuentaInternaDTO cambiarRol(Long id, CambioRolDTO datos) throws NegocioException {
        CuentaInterna cuenta = buscar(id);
        if (cuenta.esDeLdap()) {
            throw new NegocioException("El rol de este usuario lo define su grupo en el directorio LDAP");
        }
        Rol nuevo = parsearRolInterno(datos == null ? null : datos.getRol());
        if (nuevo == cuenta.getRol()) {
            return aDto(cuenta);
        }
        noSobreSiMisma(cuenta, "No pod\u00e9s cambiarte el rol a vos mismo");
        protegerUltimoAdministrador(cuenta);
        Rol anterior = cuenta.getRol();
        cuenta.setRol(nuevo);
        auditoria.registrar(AccionAuditada.USUARIO_INTERNO_ROL_CAMBIADO, ENTIDAD, cuenta.getNombreUsuario(), null,
                "antes=" + anterior + ", ahora=" + nuevo);
        return aDto(cuenta);
    }

    /** Genera una clave temporal (se muestra una sola vez) y obliga a cambiarla en el proximo ingreso. */
    @Override
    public PasswordTemporalDTO resetearPassword(Long id) throws NegocioException {
        CuentaInterna cuenta = buscar(id);
        if (cuenta.esDeLdap()) {
            throw new NegocioException("La contrase\u00f1a de este usuario se gestiona en el directorio LDAP, no ac\u00e1");
        }
        noSobreSiMisma(cuenta, "Para tu propia cuenta us\u00e1 \"Cambiar contrase\u00f1a\"");
        String temporal = PoliticaContrasenas.generarTemporal();
        cuenta.cambiarPassword(HashContrasenas.generar(temporal), true);
        auditoria.registrar(AccionAuditada.PASSWORD_RESETEADA, ENTIDAD, cuenta.getNombreUsuario(), null, null);
        return new PasswordTemporalDTO(cuenta.getNombreUsuario(), temporal);
    }

    @Override
    public CuentaInternaDTO desbloquear(Long id) throws NegocioException {
        CuentaInterna cuenta = buscar(id);
        cuenta.desbloquear();
        auditoria.registrar(AccionAuditada.USUARIO_INTERNO_DESBLOQUEADO, ENTIDAD, cuenta.getNombreUsuario(), null,
                null);
        return aDto(cuenta);
    }

    // ---------------- reglas ----------------

    private CuentaInterna buscar(Long id) throws NegocioException {
        return dao.buscarPorId(id).orElseThrow(() -> new NegocioException("El usuario no existe"));
    }

    private void noSobreSiMisma(CuentaInterna cuenta, String mensaje) throws NegocioException {
        if (cuenta.getNombreUsuario().equals(contexto.getCallerPrincipal().getName())) {
            throw new NegocioException(mensaje);
        }
    }

    private void protegerUltimoAdministrador(CuentaInterna cuenta) throws NegocioException {
        if (cuenta.getRol() == Rol.ADMINISTRADOR && cuenta.isActivo() && dao.contarAdministradoresActivos() <= 1) {
            throw new NegocioException("Tiene que quedar al menos un administrador activo");
        }
    }

    private static Rol parsearRolInterno(String texto) throws NegocioException {
        try {
            Rol rol = Rol.valueOf(texto);
            if (rol.esInterno()) {
                return rol;
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            // cae al error de abajo
        }
        throw new NegocioException("Rol inv\u00e1lido: los usuarios internos solo pueden ser ADMINISTRADOR o AUTORIDAD");
    }

    static CuentaInternaDTO aDto(CuentaInterna c) {
        return new CuentaInternaDTO(c.getId(), c.getNombreUsuario(), c.getNombre(), c.getRol().name(), c.isActivo(),
                c.estaBloqueada(Instant.now()), c.isDebeCambiarPassword(), c.getOrigen().name());
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
