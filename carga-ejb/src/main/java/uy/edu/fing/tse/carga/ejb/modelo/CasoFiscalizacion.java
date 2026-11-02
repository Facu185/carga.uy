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
 * Caso de fiscalizacion (frente P5, ADR-0022). Lo crea solo la deteccion automatica y cambia de estado
 * solo por {@link #pasarA}, que aplica el ciclo de {@link EstadoCaso}.
 */
@Entity
@Table(name = "caso_fiscalizacion")
public class CasoFiscalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guia_id")
    private Long guiaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(length = 20)
    private String matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoInfraccion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCaso estado;

    @Column(columnDefinition = "TEXT")
    private String evidencia;

    @Column(nullable = false, unique = true, length = 160)
    private String hecho;

    @Column(name = "detectada_en", nullable = false)
    private Instant detectadaEn;

    @Column(name = "notificada_en")
    private Instant notificadaEn;

    @Column(name = "vence_descargo")
    private Instant venceDescargo;

    @Column(name = "resuelta_en")
    private Instant resueltaEn;

    @Column(name = "resuelta_por", length = 100)
    private String resueltaPor;

    @Column(columnDefinition = "TEXT")
    private String fundamento;

    protected CasoFiscalizacion() {
    }

    /** Solo para las pruebas: los casos reales los crea la base con ON CONFLICT (FiscalizacionDAO). */
    public CasoFiscalizacion(Long id, TipoInfraccion tipo, EstadoCaso estado, Empresa empresa, String matricula) {
        this.id = id;
        this.tipo = tipo;
        this.estado = estado;
        this.empresa = empresa;
        this.matricula = matricula;
        this.hecho = tipo + ":" + id;
        this.detectadaEn = Instant.now();
    }

    /**
     * Aplica una transicion del ciclo de vida.
     *
     * @throws IllegalStateException si el ciclo no la admite
     */
    public void pasarA(EstadoCaso destino) {
        if (!estado.puedePasarA(destino)) {
            throw new IllegalStateException("Un caso " + estado + " no puede pasar a " + destino + ".");
        }
        estado = destino;
    }

    public void notificar(Instant ahora, Instant vence) {
        pasarA(EstadoCaso.NOTIFICADA);
        notificadaEn = ahora;
        venceDescargo = vence;
    }

    public void resolver(boolean confirmar, String usuario, String fundamento, Instant ahora) {
        pasarA(confirmar ? EstadoCaso.CONFIRMADA : EstadoCaso.DESESTIMADA);
        this.resueltaPor = usuario;
        this.fundamento = fundamento;
        this.resueltaEn = ahora;
    }

    public boolean plazoVencido(Instant ahora) {
        return venceDescargo != null && ahora.isAfter(venceDescargo);
    }

    public Long getId() { return id; }
    public Long getGuiaId() { return guiaId; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public Empresa getEmpresa() { return empresa; }
    public String getMatricula() { return matricula; }
    public TipoInfraccion getTipo() { return tipo; }
    public EstadoCaso getEstado() { return estado; }
    public String getEvidencia() { return evidencia; }
    public String getHecho() { return hecho; }
    public Instant getDetectadaEn() { return detectadaEn; }
    public Instant getNotificadaEn() { return notificadaEn; }
    public Instant getVenceDescargo() { return venceDescargo; }
    public Instant getResueltaEn() { return resueltaEn; }
    public String getResueltaPor() { return resueltaPor; }
    public String getFundamento() { return fundamento; }
}
