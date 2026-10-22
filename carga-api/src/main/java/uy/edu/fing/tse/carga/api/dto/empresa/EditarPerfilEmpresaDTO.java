package uy.edu.fing.tse.carga.api.dto.empresa;

import java.io.Serializable;

/** Entrada F10 deliberadamente separada de los campos administrativos de EmpresaDTO. */
public final class EditarPerfilEmpresaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nombre;
    private String razonSocial;
    private long numeroEmpresa;
    private String direccion;

    public EditarPerfilEmpresaDTO() { }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public long getNumeroEmpresa() { return numeroEmpresa; }
    public void setNumeroEmpresa(long numeroEmpresa) { this.numeroEmpresa = numeroEmpresa; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
