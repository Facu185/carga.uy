package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;

import java.time.LocalDate;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "numero_empresa", nullable = false, unique = true)
    private long numeroEmpresa;

    @Column(name = "fecha_alta")
    private LocalDate fechaAlta;

    @Column(nullable = false)
    private boolean activa = true;

    public static Empresa desdeDTO(EmpresaDTO dto) {
        Empresa e = new Empresa();
        e.nombre = dto.getNombre();
        e.razonSocial = dto.getRazonSocial();
        e.numeroEmpresa = dto.getNumeroEmpresa();
        e.fechaAlta = dto.getFechaAlta();
        e.activa = dto.isActiva();
        return e;
    }

    public EmpresaDTO aDTO() {
        EmpresaDTO dto = new EmpresaDTO(nombre, razonSocial, numeroEmpresa, fechaAlta);
        dto.setId(id);
        dto.setActiva(activa);
        return dto;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public long getNumeroEmpresa() { return numeroEmpresa; }
    public void setNumeroEmpresa(long numeroEmpresa) { this.numeroEmpresa = numeroEmpresa; }
    public LocalDate getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
