package uy.edu.fing.tse.carga.api.dto.cuenta;

import java.io.Serializable;

/**
 * verificacionDnic: VERIFICADO o PENDIENTE (si la PDI no respondio).
 * mayorDeEdad: null mientras este pendiente. fechaNacimiento: ISO-8601.
 */
public class PerfilDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;
    private String nombre;
    private String email;
    private String fechaNacimiento;
    private Boolean mayorDeEdad;
    private String verificacionDnic;

    public PerfilDTO() {
    }

    public PerfilDTO(String cedula, String nombre, String email, String fechaNacimiento, Boolean mayorDeEdad, String verificacionDnic) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.email = email;
        this.fechaNacimiento = fechaNacimiento;
        this.mayorDeEdad = mayorDeEdad;
        this.verificacionDnic = verificacionDnic;
    }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public Boolean getMayorDeEdad() { return mayorDeEdad; }
    public void setMayorDeEdad(Boolean mayorDeEdad) { this.mayorDeEdad = mayorDeEdad; }

    public String getVerificacionDnic() { return verificacionDnic; }
    public void setVerificacionDnic(String verificacionDnic) { this.verificacionDnic = verificacionDnic; }

}
