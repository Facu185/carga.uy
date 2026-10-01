package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Funcionario del MTOP habilitado por un administrador. Ingresa con gub.uy:
 * cedula y nombre son los del usuario asociado.
 */
@Entity
@Table(name = "funcionario")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /** La indica el funcionario al pedir acceso; la puede corregir un administrador. */
    @Column(length = 150)
    private String dependencia;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Funcionario() {
        // JPA
    }

    public Funcionario(Usuario usuario, String dependencia) {
        this.usuario = usuario;
        this.dependencia = dependencia;
        this.creadoEn = Instant.now();
    }

    public void setActivo(boolean activo) { this.activo = activo; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getCedula() { return usuario.getCedula(); }
    public String getNombre() { return usuario.getNombre(); }
    public String getDependencia() { return dependencia; }
    public boolean isActivo() { return activo; }
    public Instant getCreadoEn() { return creadoEn; }
}
