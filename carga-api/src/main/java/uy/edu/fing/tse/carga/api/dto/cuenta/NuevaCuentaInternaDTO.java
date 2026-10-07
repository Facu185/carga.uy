package uy.edu.fing.tse.carga.api.dto.cuenta;

import java.io.Serializable;

public class NuevaCuentaInternaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private String nombre;
    private String password;
    private String rol;

    public NuevaCuentaInternaDTO() {
    }

    public NuevaCuentaInternaDTO(String usuario, String nombre, String password, String rol) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.password = password;
        this.rol = rol;
    }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

}
