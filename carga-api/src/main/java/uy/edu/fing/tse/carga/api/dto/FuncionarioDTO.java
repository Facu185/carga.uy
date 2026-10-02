package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/**
 * email viene de gub.uy (solo lectura); la dependencia es lo unico editable.
 */
public class FuncionarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String cedula;
    private String nombre;
    private String email;
    private String dependencia;
    private boolean activo;

    public FuncionarioDTO() {
    }

    public FuncionarioDTO(Long id, String cedula, String nombre, String email, String dependencia, boolean activo) {
        this.id = id;
        this.cedula = cedula;
        this.nombre = nombre;
        this.email = email;
        this.dependencia = dependencia;
        this.activo = activo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

}
