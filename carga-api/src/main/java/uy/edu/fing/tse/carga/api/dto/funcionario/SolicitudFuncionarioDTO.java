package uy.edu.fing.tse.carga.api.dto.funcionario;

import java.io.Serializable;

/**
 * Fechas en ISO-8601.
 */
public class SolicitudFuncionarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String cedula;
    private String nombre;
    private String email;
    private String dependencia;
    private String estado;
    private String creadaEn;
    private String resueltaEn;
    private String resueltaPor;
    private String motivoRechazo;

    public SolicitudFuncionarioDTO() {
    }

    public SolicitudFuncionarioDTO(Long id, String cedula, String nombre, String email, String dependencia, String estado, String creadaEn, String resueltaEn, String resueltaPor, String motivoRechazo) {
        this.id = id;
        this.cedula = cedula;
        this.nombre = nombre;
        this.email = email;
        this.dependencia = dependencia;
        this.estado = estado;
        this.creadaEn = creadaEn;
        this.resueltaEn = resueltaEn;
        this.resueltaPor = resueltaPor;
        this.motivoRechazo = motivoRechazo;
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

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCreadaEn() { return creadaEn; }
    public void setCreadaEn(String creadaEn) { this.creadaEn = creadaEn; }

    public String getResueltaEn() { return resueltaEn; }
    public void setResueltaEn(String resueltaEn) { this.resueltaEn = resueltaEn; }

    public String getResueltaPor() { return resueltaPor; }
    public void setResueltaPor(String resueltaPor) { this.resueltaPor = resueltaPor; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

}
