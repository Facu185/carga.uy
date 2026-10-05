package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "chofer")
public class Chofer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "empresa_id")
    private Empresa empresa;
    @Column(nullable = false, unique = true, length = 20)
    private String cedula;
    @Column(nullable = false, length = 200)
    private String nombre;
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
