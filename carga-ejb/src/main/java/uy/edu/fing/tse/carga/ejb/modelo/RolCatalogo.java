package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila de la tabla rol. Se llama asi para no chocar con el enum Rol. */
@Entity
@Table(name = "rol")
public class RolCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    protected RolCatalogo() {
        // JPA
    }

    /** Solo para pruebas: en ejecucion los roles se leen de la base. */
    RolCatalogo(String nombre) {
        this.nombre = nombre;
    }

    public Rol aRol() {
        return Rol.valueOf(nombre);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
