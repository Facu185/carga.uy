package uy.edu.fing.tse.carga.api.dto.guia;

import java.io.Serializable;
import java.math.BigDecimal;

/** Datos de un vehÃ­culo perteneciente a una empresa transportista. */
public class VehiculoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long empresaId;
    private String matricula;
    private String marca;
    private String modelo;
    private Integer pesoKg;
    private Integer capacidadCargaKg;
    private BigDecimal capacidadVolumenM3;
    private String numeroPermiso;
    private String permisoDesde;
    private String permisoHasta;
    private String itvVencimiento;
    private boolean habilitado = true;

    public VehiculoDTO() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public Integer getPesoKg() { return pesoKg; }
    public void setPesoKg(Integer pesoKg) { this.pesoKg = pesoKg; }
    public Integer getCapacidadCargaKg() { return capacidadCargaKg; }
    public void setCapacidadCargaKg(Integer capacidadCargaKg) { this.capacidadCargaKg = capacidadCargaKg; }
    public BigDecimal getCapacidadVolumenM3() { return capacidadVolumenM3; }
    public void setCapacidadVolumenM3(BigDecimal capacidadVolumenM3) { this.capacidadVolumenM3 = capacidadVolumenM3; }
    public String getNumeroPermiso() { return numeroPermiso; }
    public void setNumeroPermiso(String numeroPermiso) { this.numeroPermiso = numeroPermiso; }
    public String getPermisoDesde() { return permisoDesde; }
    public void setPermisoDesde(String permisoDesde) { this.permisoDesde = permisoDesde; }
    public String getPermisoHasta() { return permisoHasta; }
    public void setPermisoHasta(String permisoHasta) { this.permisoHasta = permisoHasta; }
    public String getItvVencimiento() { return itvVencimiento; }
    public void setItvVencimiento(String itvVencimiento) { this.itvVencimiento = itvVencimiento; }
    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
}
