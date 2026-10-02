package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/**
 * Sesion emitida por el central. origen: BASE_DE_DATOS o LDAP (backoffice) o GUBUY (ciudadanos y funcionarios).
 * expiraEn: ISO-8601.
 */
public class SesionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private String expiraEn;
    private String usuario;
    private String nombre;
    private String rol;
    private boolean debeCambiarPassword;
    private String origen;

    public SesionDTO() {
    }

    public SesionDTO(String token, String expiraEn, String usuario, String nombre, String rol, boolean debeCambiarPassword, String origen) {
        this.token = token;
        this.expiraEn = expiraEn;
        this.usuario = usuario;
        this.nombre = nombre;
        this.rol = rol;
        this.debeCambiarPassword = debeCambiarPassword;
        this.origen = origen;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getExpiraEn() { return expiraEn; }
    public void setExpiraEn(String expiraEn) { this.expiraEn = expiraEn; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

}
