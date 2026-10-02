package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/** Datos de un chofer perteneciente a una empresa transportista. */
public class ChoferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long empresaId;
    private String cedula;
    private String nombre;

    public ChoferDTO() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
