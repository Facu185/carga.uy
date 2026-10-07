package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "tipo_carga")
public class TipoCarga {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String nombre;
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
