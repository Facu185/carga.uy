package uy.edu.fing.tse.carga.api.dto.comun;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.io.Serializable;
import java.time.LocalDate;

public class EmpresaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String razonSocial;
    private long numeroEmpresa;
    private LocalDate fechaAlta;
    private boolean activa = true;

    public EmpresaDTO() {
    }

    public EmpresaDTO(String nombre, String razonSocial, long numeroEmpresa, LocalDate fechaAlta) {
        this.nombre = nombre;
        this.razonSocial = razonSocial;
        this.numeroEmpresa = numeroEmpresa;
        this.fechaAlta = fechaAlta;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public long getNumeroEmpresa() { return numeroEmpresa; }
    public void setNumeroEmpresa(long numeroEmpresa) { this.numeroEmpresa = numeroEmpresa; }

    @XmlJavaTypeAdapter(FechaAdapter.class)
    public LocalDate getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    @Override
    public String toString() {
        return "EmpresaDTO{id=" + id + ", nombre='" + nombre + "', numeroEmpresa=" + numeroEmpresa + '}';
    }
}
