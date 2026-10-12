package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/** Solicitud verificable: la Empresa solo se crea cuando un funcionario la aprueba. */
@Entity
@Table(name = "solicitud_alta_empresa")
public class SolicitudAltaEmpresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "usuario_id") private Usuario solicitante;
    @Column(nullable = false, length = 200) private String nombre;
    @Column(name = "razon_social", length = 200) private String razonSocial;
    @Column(length = 300) private String direccion;
    @Column(name = "numero_empresa", nullable = false) private long numeroEmpresa;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoSolicitudEmpresa estado = EstadoSolicitudEmpresa.PENDIENTE;
    @Column(name = "creada_en", nullable = false) private Instant creadaEn;
    @Column(name = "resuelta_en") private Instant resueltaEn;
    @Column(name = "resuelta_por", length = 100) private String resueltaPor;
    @Column(name = "motivo_rechazo", length = 500) private String motivoRechazo;
    @ManyToOne @JoinColumn(name = "empresa_id") private Empresa empresa;
    protected SolicitudAltaEmpresa() { }
    public SolicitudAltaEmpresa(Usuario solicitante, String nombre, String razonSocial, long numeroEmpresa) {
        this(solicitante, nombre, razonSocial, null, numeroEmpresa);
    }
    public SolicitudAltaEmpresa(Usuario solicitante, String nombre, String razonSocial, String direccion, long numeroEmpresa) {
        this.solicitante=solicitante; this.nombre=nombre; this.razonSocial=razonSocial; this.direccion=direccion; this.numeroEmpresa=numeroEmpresa; this.creadaEn=Instant.now();
    }
    public boolean estaPendiente() { return estado == EstadoSolicitudEmpresa.PENDIENTE; }
    public void aprobar(String funcionario) { resolver(EstadoSolicitudEmpresa.APROBADA, funcionario); }
    public void rechazar(String funcionario, String motivo) { resolver(EstadoSolicitudEmpresa.RECHAZADA, funcionario); this.motivoRechazo=motivo; }
    public void asociarEmpresa(Empresa empresa) { if (estado != EstadoSolicitudEmpresa.APROBADA) throw new IllegalStateException("La solicitud debe estar aprobada"); this.empresa=empresa; }
    private void resolver(EstadoSolicitudEmpresa nuevo, String funcionario) { if (!estaPendiente()) throw new IllegalStateException("La solicitud ya fue resuelta"); estado=nuevo; resueltaPor=funcionario; resueltaEn=Instant.now(); }
    public Long getId(){return id;} public Usuario getSolicitante(){return solicitante;} public String getNombre(){return nombre;} public String getRazonSocial(){return razonSocial;} public String getDireccion(){return direccion;} public long getNumeroEmpresa(){return numeroEmpresa;} public EstadoSolicitudEmpresa getEstado(){return estado;} public Instant getCreadaEn(){return creadaEn;} public Instant getResueltaEn(){return resueltaEn;} public String getResueltaPor(){return resueltaPor;} public String getMotivoRechazo(){return motivoRechazo;} public Empresa getEmpresa(){return empresa;}
}
