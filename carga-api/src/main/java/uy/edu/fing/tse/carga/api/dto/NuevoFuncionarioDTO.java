package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

public class NuevoFuncionarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;
    private String nombre;
    private String dependencia;

    public NuevoFuncionarioDTO() {
    }

    public NuevoFuncionarioDTO(String cedula, String nombre, String dependencia) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.dependencia = dependencia;
    }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }

}
