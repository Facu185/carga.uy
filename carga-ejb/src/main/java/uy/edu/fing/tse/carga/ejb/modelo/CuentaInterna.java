package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

/** Cuenta del backoffice (autoridad o administrador) con autenticacion interna. */
@Entity
@Table(name = "cuenta_interna")
public class CuentaInterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 60)
    private String nombreUsuario;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** null si el origen es LDAP: la clave la valida el directorio, no esta base. */
    @Column(name = "password_hash", length = 200)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenCuenta origen = OrigenCuenta.BASE_DE_DATOS;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    /** Cambia cada vez que hay que invalidar las sesiones abiertas. */
    @Column(name = "version_credenciales", nullable = false)
    private int versionCredenciales;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected CuentaInterna() {
        // JPA
    }

    public CuentaInterna(String nombreUsuario, String nombre, String passwordHash, Rol rol) {
        exigirRolInterno(rol);
        this.nombreUsuario = nombreUsuario;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.origen = OrigenCuenta.BASE_DE_DATOS;
        this.creadoEn = Instant.now();
    }

    /** Cuenta validada por LDAP: se crea la primera vez que entra y nunca guarda clave aca. */
    public static CuentaInterna provisionarDesdeLdap(String nombreUsuario, String nombre, Rol rol) {
        exigirRolInterno(rol);
        CuentaInterna c = new CuentaInterna();
        c.nombreUsuario = nombreUsuario;
        c.nombre = nombre;
        c.rol = rol;
        c.origen = OrigenCuenta.LDAP;
        c.creadoEn = Instant.now();
        return c;
    }

    /** El directorio es la fuente de verdad: si cambio el nombre o el rol, se actualiza aca. */
    public void sincronizarDesdeLdap(String nombreActual, Rol rolActual) {
        if (nombreActual != null && !nombreActual.isBlank()) {
            this.nombre = nombreActual;
        }
        if (rolActual != this.rol) {
            setRol(rolActual);
        }
    }

    private static void exigirRolInterno(Rol rol) {
        if (rol == null || !rol.esInterno()) {
            throw new IllegalArgumentException("Una cuenta interna solo puede ser AUTORIDAD o ADMINISTRADOR");
        }
    }

    // ---------------- bloqueo por intentos fallidos ----------------

    public boolean estaBloqueada(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** @return true si con este intento la cuenta quedo bloqueada. */
    public boolean registrarIntentoFallido(Instant ahora, int maximoIntentos, Duration duracionBloqueo) {
        if (bloqueadoHasta != null && !ahora.isBefore(bloqueadoHasta)) {
            desbloquear(); // el bloqueo anterior ya vencio: se empieza a contar de nuevo
        }
        intentosFallidos++;
        if (intentosFallidos >= maximoIntentos) {
            bloqueadoHasta = ahora.plus(duracionBloqueo);
            intentosFallidos = 0;
            return true;
        }
        return false;
    }

    public void registrarIngresoExitoso() {
        desbloquear();
    }

    public void desbloquear() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    // ---------------- cambios que cierran las sesiones abiertas ----------------

    /** temporal = la genero un administrador y hay que cambiarla en el proximo ingreso. */
    public void cambiarPassword(String nuevoHash, boolean temporal) {
        if (origen == OrigenCuenta.LDAP) {
            throw new IllegalStateException("La clave de una cuenta LDAP se cambia en el directorio");
        }
        this.passwordHash = nuevoHash;
        this.debeCambiarPassword = temporal;
        this.versionCredenciales++;
        desbloquear();
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
        this.versionCredenciales++;
    }

    public void setRol(Rol rol) {
        exigirRolInterno(rol);
        this.rol = rol;
        this.versionCredenciales++;
    }

    public Long getId() { return id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public OrigenCuenta getOrigen() { return origen; }
    public boolean esDeLdap() { return origen == OrigenCuenta.LDAP; }
    public boolean isActivo() { return activo; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public int getVersionCredenciales() { return versionCredenciales; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public Instant getCreadoEn() { return creadoEn; }
}
