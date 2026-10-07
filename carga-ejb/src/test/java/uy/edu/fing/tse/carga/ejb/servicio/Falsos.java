package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.SessionContext;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.comun.PaginaAuditoriaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoSolicitud;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.RolCatalogo;
import uy.edu.fing.tse.carga.ejb.modelo.SolicitudFuncionario;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.persistencia.CuentaInternaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.ChoferDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.FuncionarioDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.SolicitudFuncionarioDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.UsuarioDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.TokenService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dobles de prueba escritos a mano (sin librerias de mocks): repositorios en memoria,
 * una auditoria que anota lo registrado y un emisor de tokens que no firma nada.
 */
final class Falsos {

    private Falsos() {
    }

    // ---------------- sesion del contenedor ----------------

    /** usuario null = nadie logueado. */
    static SessionContext sesion(String usuario, String... roles) {
        Set<String> conjunto = Set.of(roles);
        return (SessionContext) Proxy.newProxyInstance(Falsos.class.getClassLoader(),
                new Class<?>[]{SessionContext.class}, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "getCallerPrincipal" -> usuario == null ? null : (Principal) () -> usuario;
                    case "isCallerInRole" -> conjunto.contains((String) args[0]);
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
    }

    // ---------------- entidades ----------------

    static <T> T conId(T entidad, Long id) {
        try {
            Field campo = entidad.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidad, id);
            return entidad;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static RolCatalogo rol(Rol rol) {
        try {
            Constructor<RolCatalogo> c = RolCatalogo.class.getDeclaredConstructor(String.class);
            c.setAccessible(true);
            return c.newInstance(rol.name());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------- auditoria ----------------

    record Registro(String usuario, String rol, AccionAuditada accion, Object entidadId, String fundamento,
                    String detalle) {
    }

    static class AuditoriaFalsa implements AuditoriaLocal {
        final List<Registro> registros = new ArrayList<>();

        @Override
        public void registrar(AccionAuditada accion, String entidad, Object entidadId, String fundamento,
                              String detalle) throws NegocioException {
            registros.add(new Registro(null, null, accion, entidadId, fundamento, detalle));
        }

        @Override
        public void registrarComo(String usuario, String rol, AccionAuditada accion, String entidad,
                                  Object entidadId, String fundamento, String detalle) {
            registros.add(new Registro(usuario, rol, accion, entidadId, fundamento, detalle));
        }

        @Override
        public PaginaAuditoriaDTO buscar(String usuario, String entidad, String entidadId, Integer pagina,
                                         Integer tamano) {
            throw new UnsupportedOperationException();
        }

        Registro ultimo() {
            return registros.get(registros.size() - 1);
        }

        List<AccionAuditada> acciones() {
            return registros.stream().map(Registro::accion).toList();
        }
    }

    // ---------------- tokens ----------------

    /** No firma: el token es "token-usuario". Anota los roles con que se emitio el ultimo. */
    static class TokensFalsos extends TokenService {
        Set<String> ultimosRoles;
        String ultimoOrigen;

        @Override
        public TokenEmitido emitir(String usuario, Set<String> roles, String origen, int version,
                                   boolean debeCambiarPassword) {
            ultimosRoles = roles;
            ultimoOrigen = origen;
            return new TokenEmitido("token-" + usuario, Instant.parse("2030-01-01T00:00:00Z"));
        }
    }

    // ---------------- repositorios en memoria ----------------

    static class CuentasEnMemoria extends CuentaInternaDAO {
        final List<CuentaInterna> cuentas = new ArrayList<>();
        private long siguienteId = 1;

        CuentasEnMemoria con(CuentaInterna... nuevas) {
            Arrays.stream(nuevas).forEach(this::guardar);
            return this;
        }

        @Override
        public Optional<CuentaInterna> buscarPorUsuario(String nombreUsuario) {
            return cuentas.stream().filter(c -> c.getNombreUsuario().equals(nombreUsuario)).findFirst();
        }

        @Override
        public Optional<CuentaInterna> buscarPorId(Long id) {
            return cuentas.stream().filter(c -> c.getId().equals(id)).findFirst();
        }

        @Override
        public List<CuentaInterna> listar() {
            return List.copyOf(cuentas);
        }

        @Override
        public long contarAdministradoresActivos() {
            return cuentas.stream().filter(c -> c.getRol() == Rol.ADMINISTRADOR && c.isActivo()).count();
        }

        @Override
        public long contar() {
            return cuentas.size();
        }

        @Override
        public CuentaInterna guardar(CuentaInterna cuenta) {
            cuentas.add(conId(cuenta, siguienteId++));
            return cuenta;
        }
    }

    static class UsuariosEnMemoria extends UsuarioDAO {
        final List<Usuario> usuarios = new ArrayList<>();
        private long siguienteId = 1;

        @Override
        public Optional<Usuario> buscarPorCedula(String cedula) {
            return usuarios.stream().filter(u -> u.getCedula().equals(cedula)).findFirst();
        }

        @Override
        public Usuario guardar(Usuario usuario) {
            usuarios.add(conId(usuario, siguienteId++));
            return usuario;
        }

        @Override
        public RolCatalogo rol(Rol rol) {
            return Falsos.rol(rol);
        }
    }

    static class FuncionariosEnMemoria extends FuncionarioDAO {
        final List<Funcionario> funcionarios = new ArrayList<>();
        private long siguienteId = 1;

        @Override
        public Optional<Funcionario> buscarPorCedula(String cedula) {
            return funcionarios.stream().filter(f -> f.getCedula().equals(cedula)).findFirst();
        }

        @Override
        public Optional<Funcionario> buscarPorId(Long id) {
            return funcionarios.stream().filter(f -> f.getId().equals(id)).findFirst();
        }

        @Override
        public List<Funcionario> listar() {
            return List.copyOf(funcionarios);
        }

        @Override
        public Funcionario guardar(Funcionario funcionario) {
            funcionarios.add(conId(funcionario, siguienteId++));
            return funcionario;
        }
    }

    static class ChoferesEnMemoria extends ChoferDAO {
        final List<Chofer> choferes = new ArrayList<>();
        private long siguienteId = 1;

        @Override
        public Optional<Chofer> buscarPorCedula(String cedula) {
            return choferes.stream().filter(c -> c.getCedula().equals(cedula)).findFirst();
        }

        Chofer guardar(Chofer chofer) {
            choferes.add(conId(chofer, siguienteId++));
            return chofer;
        }
    }

    static class SolicitudesEnMemoria extends SolicitudFuncionarioDAO {
        final List<SolicitudFuncionario> solicitudes = new ArrayList<>();
        private long siguienteId = 1;

        @Override
        public SolicitudFuncionario guardar(SolicitudFuncionario solicitud) {
            solicitudes.add(conId(solicitud, siguienteId++));
            return solicitud;
        }

        @Override
        public Optional<SolicitudFuncionario> buscarPorId(Long id) {
            return solicitudes.stream().filter(s -> s.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<SolicitudFuncionario> ultimaPorCedula(String cedula) {
            return solicitudes.stream()
                    .filter(s -> s.getSolicitante().getCedula().equals(cedula))
                    .max(Comparator.comparing(SolicitudFuncionario::getId));
        }

        @Override
        public List<SolicitudFuncionario> listar(EstadoSolicitud estado) {
            return solicitudes.stream().filter(s -> estado == null || s.getEstado() == estado).toList();
        }
    }
}
