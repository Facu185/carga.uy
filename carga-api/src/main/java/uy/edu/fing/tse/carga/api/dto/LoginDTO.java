package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/**
 * Credenciales del backoffice (mecanismo interno).
 */
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private String password;

    public LoginDTO() {
    }

    public LoginDTO(String usuario, String password) {
        this.usuario = usuario;
        this.password = password;
    }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}
