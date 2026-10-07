package uy.edu.fing.tse.carga.api.dto.cuenta;

import java.io.Serializable;

/**
 * Cuenta del backoffice tal como la ve un administrador.
 */
public class CuentaInternaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String usuario;
    private String nombre;
    private String rol;
    private boolean activo;
    private boolean bloqueado;
    private boolean debeCambiarPassword;
    private String origen;

    public CuentaInternaDTO() {
    }

    public CuentaInternaDTO(Long id, String usuario, String nombre, String rol, boolean activo, boolean bloqueado, boolean debeCambiarPassword, String origen) {
        this.id = id;
        this.usuario = usuario;
        this.nombre = nombre;
        this.rol = rol;
        this.activo = activo;
        this.bloqueado = bloqueado;
        this.debeCambiarPassword = debeCambiarPassword;
        this.origen = origen;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }

    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

}
