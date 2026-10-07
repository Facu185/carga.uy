package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.AutenticacionLocal;
import uy.edu.fing.tse.carga.api.exception.CredencialesInvalidasException;
import uy.edu.fing.tse.carga.api.exception.CuentaBloqueadaException;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.dto.cuenta.DatosSesionDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.LoginDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.SesionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.persistencia.CuentaInternaDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.ClienteLdap;
import uy.edu.fing.tse.carga.ejb.seguridad.ConfiguracionLdap;
import uy.edu.fing.tse.carga.ejb.seguridad.HashContrasenas;
import uy.edu.fing.tse.carga.ejb.seguridad.IdentidadLdap;
import uy.edu.fing.tse.carga.ejb.seguridad.TokenService;
import uy.edu.fing.tse.carga.ejb.seguridad.VerificadorSesiones;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Ingreso al backoffice con el mecanismo interno: contra esta base o contra un directorio LDAP,
 * segun AUTENTICACION_INTERNA. Tambien valida los tokens en cada pedido (lo usa la capa web).
 *
 * Tras MAXIMO_INTENTOS fallidos seguidos la cuenta se bloquea DURACION_BLOQUEO, aunque despues
 * se ingrese la clave correcta. Todo intento (exitoso, fallido o bloqueado) queda en la auditoria.
 * El bloqueo y la baja local se aplican en los dos modos: aunque LDAP acepte la clave, una cuenta
 * dada de baja en carga.uy no entra.
 */
@Stateless
@PermitAll
public class AutenticacionBean implements AutenticacionLocal {

    static final int MAXIMO_INTENTOS = 5;
    static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);
    static final String ENTIDAD = "CuentaInterna";

    /**
     * Si la cuenta no existe igual se calcula un hash: la respuesta tarda lo mismo y no se puede
     * averiguar que cuentas existen midiendo tiempos (contra LDAP el tiempo lo define el directorio).
     */
    private static final String HASH_FALSO = HashContrasenas.generar("hash-para-igualar-tiempos");

    @Inject
    CuentaInternaDAO dao;

    @Inject
    TokenService tokenService;

    @Inject
    VerificadorSesiones verificador;

    @Inject
    ClienteLdap ldap;

    @EJB
    AuditoriaLocal auditoria;

    @EJB
    MetricasLocal metricas;

    boolean ldapHabilitado;

    @PostConstruct
    void inicializar() {
        ldapHabilitado = ConfiguracionLdap.habilitado();
    }

    @Override
    public SesionDTO login(LoginDTO credenciales) {
        if (credenciales == null || vacio(credenciales.getUsuario()) || vacio(credenciales.getPassword())) {
            throw new CredencialesInvalidasException();
        }
        String nombreUsuario = credenciales.getUsuario().trim();
        Instant ahora = Instant.now();
        Optional<CuentaInterna> existente = dao.buscarPorUsuario(nombreUsuario);

        if (existente.isPresent() && existente.get().estaBloqueada(ahora)) {
            auditoria.registrarComo(nombreUsuario, null, AccionAuditada.INGRESO_FALLIDO, ENTIDAD, nombreUsuario,
                    null, "cuenta bloqueada");
            contarIngreso("bloqueado");
            throw new CuentaBloqueadaException(minutosRestantes(existente.get(), ahora));
        }
        return ldapHabilitado
                ? loginLdap(nombreUsuario, credenciales.getPassword(), existente, ahora)
                : loginBaseDeDatos(nombreUsuario, credenciales.getPassword(), existente, ahora);
    }

    @Override
    public DatosSesionDTO validarToken(String token) {
        return tokenService.validar(token)
                .filter(verificador::sigueVigente)
                .map(d -> new DatosSesionDTO(d.usuario(), d.roles(), d.debeCambiarPassword()))
                .orElse(null);
    }

    // ---------------- modo base de datos ----------------

    private SesionDTO loginBaseDeDatos(String nombreUsuario, String password, Optional<CuentaInterna> existente,
                                       Instant ahora) {
        String hash = existente.map(CuentaInterna::getPasswordHash).orElse(HASH_FALSO);
        boolean passwordOk = hash != null && HashContrasenas.verificar(password, hash);

        if (existente.isEmpty() || !passwordOk || !existente.get().isActivo()) {
            String motivo = existente.isEmpty() ? "usuario inexistente"
                    : passwordOk ? "usuario dado de baja" : "contrase\u00f1a incorrecta";
            fallo(nombreUsuario, existente, ahora, motivo);
        }
        return exito(existente.get());
    }

    // ---------------- modo LDAP ----------------

    private SesionDTO loginLdap(String nombreUsuario, String password, Optional<CuentaInterna> existente,
                                Instant ahora) {
        Optional<IdentidadLdap> identidad = ldap.autenticar(nombreUsuario, password);

        if (identidad.isEmpty() || (existente.isPresent() && !existente.get().isActivo())) {
            String motivo = identidad.isEmpty() ? "credenciales LDAP incorrectas o sin rol interno"
                    : "usuario dado de baja";
            fallo(nombreUsuario, existente, ahora, motivo);
        }
        CuentaInterna cuenta = existente.orElseGet(() -> dao.guardar(
                CuentaInterna.provisionarDesdeLdap(nombreUsuario, identidad.get().nombre(), identidad.get().rol())));
        cuenta.sincronizarDesdeLdap(identidad.get().nombre(), identidad.get().rol());
        return exito(cuenta);
    }

    // ---------------- comun ----------------

    /** Suma un intento fallido (y bloquea si corresponde), audita y lanza la excepcion que corresponde. */
    private void fallo(String nombreUsuario, Optional<CuentaInterna> existente, Instant ahora, String motivo) {
        boolean seBloqueo = existente
                .map(c -> c.registrarIntentoFallido(ahora, MAXIMO_INTENTOS, DURACION_BLOQUEO))
                .orElse(false);
        auditoria.registrarComo(nombreUsuario, null,
                seBloqueo ? AccionAuditada.CUENTA_BLOQUEADA : AccionAuditada.INGRESO_FALLIDO,
                ENTIDAD, nombreUsuario, null, seBloqueo ? MAXIMO_INTENTOS + " intentos fallidos seguidos" : motivo);
        contarIngreso(seBloqueo ? "bloqueado" : "fallido");
        if (seBloqueo) {
            throw new CuentaBloqueadaException(DURACION_BLOQUEO.toMinutes());
        }
        throw new CredencialesInvalidasException();
    }

    private SesionDTO exito(CuentaInterna cuenta) {
        cuenta.registrarIngresoExitoso();
        auditoria.registrarComo(cuenta.getNombreUsuario(), cuenta.getRol().name(), AccionAuditada.INGRESO_EXITOSO,
                ENTIDAD, cuenta.getNombreUsuario(), null, ldapHabilitado ? "backoffice (LDAP)" : "backoffice");
        contarIngreso("exitoso");
        return sesion(cuenta, tokenService);
    }

    /** Intentos de ingreso al backoffice por resultado: un pico de fallidos es un ataque de fuerza bruta. */
    private void contarIngreso(String resultado) {
        metricas.contar("carga_ingresos_total", "mecanismo", ldapHabilitado ? "ldap" : "bd", "resultado", resultado);
    }

    /** Sesion de una cuenta interna (tambien la usan el cambio de clave y la renovacion). */
    static SesionDTO sesion(CuentaInterna cuenta, TokenService tokens) {
        TokenService.TokenEmitido token = tokens.emitir(cuenta.getNombreUsuario(), Set.of(cuenta.getRol().name()),
                TokenService.ORIGEN_INTERNO, cuenta.getVersionCredenciales(), cuenta.isDebeCambiarPassword());
        return new SesionDTO(token.token(), token.expiraEn().toString(), cuenta.getNombreUsuario(),
                cuenta.getNombre(), cuenta.getRol().name(), cuenta.isDebeCambiarPassword(), cuenta.getOrigen().name());
    }

    private static long minutosRestantes(CuentaInterna cuenta, Instant ahora) {
        long segundos = Duration.between(ahora, cuenta.getBloqueadoHasta()).getSeconds();
        return (segundos + 59) / 60;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
