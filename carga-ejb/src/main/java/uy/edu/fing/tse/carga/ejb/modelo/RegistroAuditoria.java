package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;

import java.time.Instant;

/**
 * Fila de la pista de auditoria. Inmutable: sin setters y con todas las columnas updatable = false.
 * Ademas la base rechaza UPDATE, DELETE y TRUNCATE con un trigger (V4).
 */
@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant instante;

    @Column(nullable = false, updatable = false, length = 100)
    private String usuario;

    @Column(updatable = false, length = 50)
    private String rol;

    /** Texto y no @Enumerated: el enum crece con cada frente y la columna no tiene que cambiar. */
    @Column(nullable = false, updatable = false, length = 100)
    private String accion;

    @Column(updatable = false, length = 100)
    private String entidad;

    @Column(name = "entidad_id", updatable = false, length = 60)
    private String entidadId;

    @Column(updatable = false)
    private String fundamento;

    @Column(updatable = false)
    private String detalle;

    protected RegistroAuditoria() {
        // JPA
    }

    public RegistroAuditoria(String usuario, String rol, AccionAuditada accion, String entidad, String entidadId,
                             String fundamento, String detalle) {
        this.instante = Instant.now();
        this.usuario = usuario;
        this.rol = rol;
        this.accion = accion.name();
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.fundamento = fundamento;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Instant getInstante() { return instante; }
    public String getUsuario() { return usuario; }
    public String getRol() { return rol; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public String getEntidadId() { return entidadId; }
    public String getFundamento() { return fundamento; }
    public String getDetalle() { return detalle; }
}
