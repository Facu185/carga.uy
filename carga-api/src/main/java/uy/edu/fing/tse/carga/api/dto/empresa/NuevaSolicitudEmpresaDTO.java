package uy.edu.fing.tse.carga.api.dto.empresa;

import java.io.Serializable;

/** Datos de la empresa que una persona autenticada propone dar de alta. */
public class NuevaSolicitudEmpresaDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre;
    private String razonSocial;
    private String direccion;
    private long numeroEmpresa;

    public NuevaSolicitudEmpresaDTO() { }
    public NuevaSolicitudEmpresaDTO(String nombre, String razonSocial, long numeroEmpresa) {
        this.nombre = nombre; this.razonSocial = razonSocial; this.numeroEmpresa = numeroEmpresa;
    }
    public NuevaSolicitudEmpresaDTO(String nombre, String razonSocial, String direccion, long numeroEmpresa) {
        this.nombre = nombre; this.razonSocial = razonSocial; this.direccion = direccion; this.numeroEmpresa = numeroEmpresa;
    }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public long getNumeroEmpresa() { return numeroEmpresa; }
    public void setNumeroEmpresa(long numeroEmpresa) { this.numeroEmpresa = numeroEmpresa; }
}
