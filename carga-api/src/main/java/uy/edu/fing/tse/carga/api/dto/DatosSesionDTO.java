package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/**
 * Lo que el mecanismo de autenticacion de la capa web necesita saber de un token valido y vigente.
 */
public class DatosSesionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private java.util.Set<String> roles;
    private boolean debeCambiarPassword;

    public DatosSesionDTO() {
    }

    public DatosSesionDTO(String usuario, java.util.Set<String> roles, boolean debeCambiarPassword) {
        this.usuario = usuario;
        this.roles = roles;
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public java.util.Set<String> getRoles() { return roles; }
    public void setRoles(java.util.Set<String> roles) { this.roles = roles; }

    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }

}
