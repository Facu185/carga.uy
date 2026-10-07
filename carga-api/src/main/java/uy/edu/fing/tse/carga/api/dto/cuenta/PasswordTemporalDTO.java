package uy.edu.fing.tse.carga.api.dto.cuenta;

import java.io.Serializable;

/**
 * Clave generada por un administrador: se muestra una sola vez y hay que cambiarla al ingresar.
 */
public class PasswordTemporalDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private String passwordTemporal;

    public PasswordTemporalDTO() {
    }

    public PasswordTemporalDTO(String usuario, String passwordTemporal) {
        this.usuario = usuario;
        this.passwordTemporal = passwordTemporal;
    }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPasswordTemporal() { return passwordTemporal; }
    public void setPasswordTemporal(String passwordTemporal) { this.passwordTemporal = passwordTemporal; }

}
