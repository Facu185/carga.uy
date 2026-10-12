package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persona identificada por cedula que ingresa con gub.uy (ciudadano, chofer, funcionario...).
 * Los roles se guardan en usuario_rol.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(length = 200)
    private String correo;

    @Column(length = 200)
    private String nombre;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    /** Viene de DNIC (via PDI). null mientras no se pudo verificar. */
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "verificado_dnic_en")
    private Instant verificadoDnicEn;

    @Column(name = "ultimo_ingreso")
    private Instant ultimoIngreso;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<RolCatalogo> roles = new HashSet<>();

    protected Usuario() {
        // JPA
    }

    public Usuario(String cedula, String nombre, String correo) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.correo = correo;
        this.creadoEn = Instant.now();
    }

    /** Cada ingreso con gub.uy: actualiza lo que haya cambiado ahi y la fecha del ultimo ingreso. */
    public void registrarIngreso(String nombreGubUy, String correoGubUy) {
        sincronizarDatos(nombreGubUy, correoGubUy);
        this.ultimoIngreso = Instant.now();
    }

    public void registrarDatosDnic(LocalDate fechaNacimientoDnic) {
        this.fechaNacimiento = fechaNacimientoDnic;
        this.verificadoDnicEn = Instant.now();
    }

    public boolean estaVerificadoConDnic() {
        return verificadoDnicEn != null;
    }

    /** null = todavia no se sabe (DNIC no respondio o no devolvio la fecha). */
    public Boolean esMayorDeEdad(LocalDate hoy) {
        return fechaNacimiento == null ? null : Period.between(fechaNacimiento, hoy).getYears() >= 18;
    }

    public boolean tieneRol(Rol rol) {
        return roles.stream().anyMatch(r -> r.getNombre().equals(rol.name()));
    }

    /** No duplica: si ya lo tiene, no hace nada. */
    public void asignarRol(RolCatalogo rol) {
        if (!tieneRol(rol.aRol())) {
            roles.add(rol);
        }
    }

    public void quitarRol(Rol rol) {
        roles.removeIf(r -> r.getNombre().equals(rol.name()));
    }

    /** gub.uy es la fuente de verdad: en cada ingreso se actualiza lo que haya cambiado ahi. */
    public void sincronizarDatos(String nombreGubUy, String correoGubUy) {
        if (nombreGubUy != null && !nombreGubUy.isBlank()) {
            this.nombre = nombreGubUy;
        }
        if (correoGubUy != null && !correoGubUy.isBlank()) {
            this.correo = correoGubUy;
        }
    }

    public Long getId() { return id; }
    public String getCedula() { return cedula; }
    public String getCorreo() { return correo; }
    public String getNombre() { return nombre; }
    public Instant getCreadoEn() { return creadoEn; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public Instant getVerificadoDnicEn() { return verificadoDnicEn; }
    public Instant getUltimoIngreso() { return ultimoIngreso; }
    public Set<RolCatalogo> getRoles() { return Collections.unmodifiableSet(roles); }
}
