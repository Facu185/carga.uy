package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

public class CambioRolDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String rol;

    public CambioRolDTO() {
    }

    public CambioRolDTO(String rol) {
        this.rol = rol;
    }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

}
