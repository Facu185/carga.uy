package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/** Datos de una persona devueltos por el servicio de DNIC a traves de la PDI. */
public class PersonaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;
    private String nombreCompleto;
    /** ISO-8601 (yyyy-MM-dd). */
    private String fechaNacimiento;

    public PersonaDTO() {
    }

    public PersonaDTO(String cedula, String nombreCompleto, String fechaNacimiento) {
        this.cedula = cedula;
        this.nombreCompleto = nombreCompleto;
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
}
