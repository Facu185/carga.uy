package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

public class CambioDependenciaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dependencia;

    public CambioDependenciaDTO() {
    }

    public CambioDependenciaDTO(String dependencia) {
        this.dependencia = dependencia;
    }

    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }

}
