package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.IngresoGubUyLocal;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.contract.service.PdiLocal;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.api.dto.comun.PersonaDTO;
import uy.edu.fing.tse.carga.api.dto.comun.RedireccionDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.SesionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;
import uy.edu.fing.tse.carga.ejb.modelo.RolCatalogo;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.modelo.ValidadorCedula;
import uy.edu.fing.tse.carga.ejb.persistencia.FuncionarioDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.UsuarioDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.ClienteOidcGubUy;
import uy.edu.fing.tse.carga.ejb.seguridad.ConfiguracionGubUy;
import uy.edu.fing.tse.carga.ejb.seguridad.GubUyException;
import uy.edu.fing.tse.carga.ejb.seguridad.IdentidadGubUy;
import uy.edu.fing.tse.carga.ejb.seguridad.TokenService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Ingreso con gub.uy para ciudadanos y funcionarios (web y app movil).
 *
 * 1. iniciar: arma la redireccion a gub.uy. state, nonce y el verifier de PKCE viajan firmados en una
 *    cookie HttpOnly (no se guarda nada en el servidor).
 * 2. completar: gub.uy vuelve con code y state; se valida el state, se canjea el code, se valida el
 *    id_token y se redirige al frontend (o a la app) con NUESTRO token en el fragmento (#). El fragmento
 *    no se manda a ningun servidor ni queda en logs.
 *
 * gub.uy dice QUIEN es la persona; los ROLES los decide carga.uy.
 */
@Stateless
@PermitAll
public class IngresoGubUyBean implements IngresoGubUyLocal {

    private static final Logger LOG = Logger.getLogger(IngresoGubUyBean.class.getName());
    static final Duration VIGENCIA_TRANSACCION = Duration.ofMinutes(10);
    private static final String ENTIDAD = "Usuario";

    @Inject
    ConfiguracionGubUy config;

    @Inject
    ClienteOidcGubUy cliente;

    @Inject
    TokenService tokens;

    @Inject
    UsuarioDAO usuarios;

    @Inject
    FuncionarioDAO funcionarios;

    @EJB
    PdiLocal pdi;

    @EJB
    AuditoriaLocal auditoria;

    @EJB
    MetricasLocal metricas;

    // ---------------- paso 1: ir a gub.uy ----------------

    @Override
    public RedireccionDTO iniciar(String destino) throws NegocioException {
        if (config.urlDestino(destino).isEmpty()) {
            throw new NegocioException("Destino inv\u00e1lido");
        }
        String state = ClienteOidcGubUy.aleatorio();
        String nonce = ClienteOidcGubUy.aleatorio();
        String verifier = ClienteOidcGubUy.aleatorio();
        String transaccion = tokens.firmarTransaccion(
                Map.of("state", state, "nonce", nonce, "verifier", verifier, "destino", destino),
                VIGENCIA_TRANSACCION);
        try {
            return new RedireccionDTO(cliente.urlAutorizacion(state, nonce, verifier), transaccion,
                    (int) VIGENCIA_TRANSACCION.toSeconds());
        } catch (GubUyException e) {
            LOG.log(Level.WARNING, "No se pudo iniciar el ingreso con gub.uy", e);
            return conError(destino, e.getMessage());
        }
    }

    // ---------------- paso 2: vuelta de gub.uy ----------------

    @Override
    public RedireccionDTO completar(String code, String state, String error, String transaccion) {
        Optional<Map<String, String>> datos = tokens.leerTransaccion(transaccion);
        if (datos.isEmpty()) {
            return conError("web", "El ingreso expir\u00f3 o no se inici\u00f3 desde carga.uy. Intent\u00e1 de nuevo.");
        }
        String destino = datos.get().get("destino");
        if ("access_denied".equals(error)) {
            // La persona cancelo en gub.uy: no es un error, vuelve a donde estaba
            return redireccion(config.urlDestino(destino).orElseThrow() + "#cancelado=1");
        }
        if (error != null) {
            return conError(destino, "gub.uy no pudo completar el ingreso. Intent\u00e1 de nuevo.");
        }
        if (code == null || state == null || !iguales(state, datos.get().get("state"))) {
            return conError(destino, "Respuesta de gub.uy inv\u00e1lida. Intent\u00e1 de nuevo.");
        }
        try {
            IdentidadGubUy identidad = cliente.canjearCodigo(code, datos.get().get("verifier"), datos.get().get("nonce"));
            SesionDTO sesion = ingresar(identidad);
            String fragmento = "token=" + enc(sesion.getToken())
                    + "&expiraEn=" + enc(sesion.getExpiraEn())
                    + "&usuario=" + enc(sesion.getUsuario())
                    + "&nombre=" + enc(sesion.getNombre())
                    + "&rol=" + enc(sesion.getRol());
            return redireccion(config.urlDestino(destino).orElseThrow() + "#" + fragmento);
        } catch (GubUyException | NegocioException e) {
            LOG.log(Level.WARNING, "Fall\u00f3 el ingreso con gub.uy", e);
            return conError(destino, e.getMessage());
        }
    }

    @Override
    public boolean cookieSegura() {
        return config.cookieSegura();
    }

    // ---------------- lo que pasa despues de que gub.uy confirma la identidad ----------------

    /** Registra a la persona la primera vez, sincroniza sus roles y emite nuestro token. */
    public SesionDTO ingresar(IdentidadGubUy identidad) throws NegocioException {
        String cedula = ValidadorCedula.normalizar(identidad.documento());
        if (cedula == null || !ValidadorCedula.esValida(cedula)) {
            throw new NegocioException("La c\u00e9dula recibida de gub.uy no es v\u00e1lida");
        }
        Usuario persona = usuarios.buscarPorCedula(cedula)
                .orElseGet(() -> usuarios.guardar(new Usuario(cedula, identidad.nombre(), identidad.email())));
        persona.registrarIngreso(identidad.nombre(), identidad.email());
        verificarConDnic(persona);
        sincronizarRoles(persona);

        SesionDTO sesion = sesion(persona, tokens);
        auditoria.registrarComo(cedula, sesion.getRol(), AccionAuditada.INGRESO_EXITOSO, ENTIDAD, cedula, null,
                "gub.uy");
        metricas.contar("carga_ingresos_total", "mecanismo", "gubuy", "resultado", "exitoso");
        return sesion;
    }

    /**
     * Todos los que ingresan con gub.uy son ciudadanos; FUNCIONARIO solo si hay un funcionario activo
     * con esa cedula. Se corrige en cada ingreso, asi usuario_rol nunca queda desalineado.
     */
    private void sincronizarRoles(Usuario persona) {
        persona.asignarRol(usuarios.rol(Rol.CIUDADANO));
        boolean esFuncionario = funcionarios.buscarPorCedula(persona.getCedula())
                .map(Funcionario::isActivo).orElse(false);
        if (esFuncionario) {
            persona.asignarRol(usuarios.rol(Rol.FUNCIONARIO));
        } else {
            persona.quitarRol(Rol.FUNCIONARIO);
        }
    }

    /**
     * Completa la fecha de nacimiento con DNIC la primera vez. Modo degradado: si la PDI no responde,
     * el ingreso sigue igual y la persona queda "pendiente de verificacion" hasta el proximo ingreso.
     */
    private void verificarConDnic(Usuario persona) {
        if (persona.estaVerificadoConDnic()) {
            return;
        }
        PersonaDTO datos = pdi.obtenerPersona(persona.getCedula());
        if (datos == null) {
            LOG.warning("Ingreso sin verificaci\u00f3n DNIC para " + persona.getCedula());
            return;
        }
        persona.registrarDatosDnic(fecha(datos.getFechaNacimiento()));
    }

    /** Sesion de una persona que ingreso con gub.uy (tambien la usa la renovacion). */
    static SesionDTO sesion(Usuario persona, TokenService tokens) {
        Set<String> roles = persona.getRoles().stream().map(RolCatalogo::getNombre).collect(Collectors.toSet());
        String principal = roles.contains(Roles.FUNCIONARIO) ? Roles.FUNCIONARIO : Roles.CIUDADANO;
        TokenService.TokenEmitido token = tokens.emitir(persona.getCedula(), roles, TokenService.ORIGEN_GUBUY, 0,
                false);
        String nombre = persona.getNombre() == null || persona.getNombre().isBlank()
                ? persona.getCedula() : persona.getNombre();
        return new SesionDTO(token.token(), token.expiraEn().toString(), persona.getCedula(), nombre, principal,
                false, "GUBUY");
    }

    // ---------------- utilidades ----------------

    private RedireccionDTO conError(String destino, String mensaje) {
        String base = config.urlDestino(destino).or(() -> config.urlDestino("web")).orElseThrow();
        return redireccion(base + "#error=" + enc(mensaje));
    }

    /** transaccion null: la capa web borra la cookie del ingreso. */
    private static RedireccionDTO redireccion(String url) {
        return new RedireccionDTO(url, null, 0);
    }

    private static LocalDate fecha(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean iguales(String a, String b) {
        return b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String enc(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }
}
