package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "vehiculo")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false, unique = true, length = 20)
    private String matricula;

    @Column(length = 60)
    private String marca;

    @Column(length = 60)
    private String modelo;

    /** Peso del vehiculo vacio, en kg. */
    @Column(name = "peso_kg", nullable = false)
    private int pesoKg;

    /** Capacidad de carga declarada, en kg. Junto con el peso define el limite de exceso de carga. */
    @Column(name = "capacidad_carga_kg", nullable = false)
    private int capacidadCargaKg;

    /**
     * Capacidad volumetrica declarada, en metros cubicos. Puede ser null para
     * vehiculos heredados pendientes de configuracion; no pueden recibir guias
     * hasta que se informe este dato.
     */
    @Column(name = "capacidad_volumen_m3", precision = 12, scale = 2)
    private BigDecimal capacidadVolumenM3;

    @Column(name = "numero_permiso", length = 50)
    private String numeroPermiso;

    @Column(name = "permiso_desde")
    private LocalDate permisoDesde;

    @Column(name = "permiso_hasta")
    private LocalDate permisoHasta;

    @Column(name = "itv_vencimiento")
    private LocalDate itvVencimiento;

    /**
     * Indica si el vehiculo esta habilitado para circular en la fecha dada.
     * Regla "vehiculo sin habilitacion vigente" (Seccion 3.5 de la letra).
     */
    public boolean habilitadoEn(LocalDate fecha) {
        boolean permisoVigente = permisoDesde != null && permisoHasta != null
                && !fecha.isBefore(permisoDesde) && !fecha.isAfter(permisoHasta);
        boolean itvVigente = itvVencimiento != null && !fecha.isAfter(itvVencimiento);
        return permisoVigente && itvVigente;
    }

    /** Peso maximo admitido en una balanza antes de configurar exceso de carga. */
    public int pesoMaximoAdmitidoKg() {
        return pesoKg + capacidadCargaKg;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public int getPesoKg() { return pesoKg; }
    public void setPesoKg(int pesoKg) { this.pesoKg = pesoKg; }
    public int getCapacidadCargaKg() { return capacidadCargaKg; }
    public void setCapacidadCargaKg(int capacidadCargaKg) { this.capacidadCargaKg = capacidadCargaKg; }
    public BigDecimal getCapacidadVolumenM3() { return capacidadVolumenM3; }
    public void setCapacidadVolumenM3(BigDecimal capacidadVolumenM3) { this.capacidadVolumenM3 = capacidadVolumenM3; }
    public String getNumeroPermiso() { return numeroPermiso; }
    public void setNumeroPermiso(String numeroPermiso) { this.numeroPermiso = numeroPermiso; }
    public LocalDate getPermisoDesde() { return permisoDesde; }
    public void setPermisoDesde(LocalDate permisoDesde) { this.permisoDesde = permisoDesde; }
    public LocalDate getPermisoHasta() { return permisoHasta; }
    public void setPermisoHasta(LocalDate permisoHasta) { this.permisoHasta = permisoHasta; }
    public LocalDate getItvVencimiento() { return itvVencimiento; }
    public void setItvVencimiento(LocalDate itvVencimiento) { this.itvVencimiento = itvVencimiento; }
}
