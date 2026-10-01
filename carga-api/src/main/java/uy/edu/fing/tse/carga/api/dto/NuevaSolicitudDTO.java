package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

public class NuevaSolicitudDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dependencia;

    public NuevaSolicitudDTO() {
    }

    public NuevaSolicitudDTO(String dependencia) {
        this.dependencia = dependencia;
    }

    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }

}
