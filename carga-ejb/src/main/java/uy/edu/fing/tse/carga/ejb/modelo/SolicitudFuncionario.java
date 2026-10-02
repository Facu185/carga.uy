package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Pedido de una persona ya identificada con gub.uy para acceder como funcionario del MTOP.
 * El solicitante solo indica su dependencia: el resto sale de su usuario verificado.
 */
@Entity
@Table(name = "solicitud_funcionario")
public class SolicitudFuncionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario solicitante;

    @Column(nullable = false, length = 150)
    private String dependencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "resuelta_en")
    private Instant resueltaEn;

    @Column(name = "resuelta_por", length = 100)
    private String resueltaPor;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    protected SolicitudFuncionario() {
        // JPA
    }

    public SolicitudFuncionario(Usuario solicitante, String dependencia) {
        this.solicitante = solicitante;
        this.dependencia = dependencia;
        this.creadaEn = Instant.now();
    }

    public boolean estaPendiente() {
        return estado == EstadoSolicitud.PENDIENTE;
    }

    public void aprobar(String administrador) {
        resolver(EstadoSolicitud.APROBADA, administrador);
    }

    public void rechazar(String administrador, String motivo) {
        resolver(EstadoSolicitud.RECHAZADA, administrador);
        this.motivoRechazo = motivo;
    }

    private void resolver(EstadoSolicitud nuevo, String administrador) {
        if (!estaPendiente()) {
            throw new IllegalStateException("La solicitud ya fue resuelta");
        }
        this.estado = nuevo;
        this.resueltaPor = administrador;
        this.resueltaEn = Instant.now();
    }

    public Long getId() { return id; }
    public Usuario getSolicitante() { return solicitante; }
    public String getDependencia() { return dependencia; }
    public EstadoSolicitud getEstado() { return estado; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getResueltaEn() { return resueltaEn; }
    public String getResueltaPor() { return resueltaPor; }
    public String getMotivoRechazo() { return motivoRechazo; }
}
