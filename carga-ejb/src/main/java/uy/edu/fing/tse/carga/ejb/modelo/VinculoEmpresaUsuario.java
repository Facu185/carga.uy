package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/** Relación auditable entre una persona y la empresa sobre la que puede operar. */
@Entity
@Table(name = "vinculo_empresa_usuario")
public class VinculoEmpresaUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "empresa_id") private Empresa empresa;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TipoVinculoEmpresa tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoVinculoEmpresa estado = EstadoVinculoEmpresa.PENDIENTE;
    @Column(name = "solicitado_en", nullable = false) private Instant solicitadoEn;
    @Column(name = "aprobado_en") private Instant aprobadoEn;
    @Column(name = "aprobado_por", length = 100) private String aprobadoPor;
    @Column(name = "revocado_en") private Instant revocadoEn;
    @Column(name = "revocado_por", length = 100) private String revocadoPor;
    @Column(name = "motivo_revocacion", length = 500) private String motivoRevocacion;
    protected VinculoEmpresaUsuario() { }
    public VinculoEmpresaUsuario(Usuario usuario, Empresa empresa, TipoVinculoEmpresa tipo) { this.usuario=usuario; this.empresa=empresa; this.tipo=tipo; this.solicitadoEn=Instant.now(); }
    public boolean estaPendiente(){return estado==EstadoVinculoEmpresa.PENDIENTE;} public boolean estaActivo(){return estado==EstadoVinculoEmpresa.ACTIVO;}
    public void aprobar(String funcionario){if(!estaPendiente()) throw new IllegalStateException("El vinculo ya fue resuelto"); estado=EstadoVinculoEmpresa.ACTIVO; aprobadoPor=funcionario; aprobadoEn=Instant.now();}
    public void rechazar(String funcionario, String motivo){if(!estaPendiente()) throw new IllegalStateException("El vinculo ya fue resuelto"); estado=EstadoVinculoEmpresa.RECHAZADO; revocadoPor=funcionario; revocadoEn=Instant.now(); motivoRevocacion=motivo;}
    public void revocar(String funcionario, String motivo){if(!estaActivo()) throw new IllegalStateException("El vinculo no esta activo"); estado=EstadoVinculoEmpresa.REVOCADO; revocadoPor=funcionario; revocadoEn=Instant.now(); motivoRevocacion=motivo;}
    public Long getId(){return id;} public Usuario getUsuario(){return usuario;} public Empresa getEmpresa(){return empresa;} public TipoVinculoEmpresa getTipo(){return tipo;} public EstadoVinculoEmpresa getEstado(){return estado;} public Instant getSolicitadoEn(){return solicitadoEn;} public Instant getAprobadoEn(){return aprobadoEn;} public String getAprobadoPor(){return aprobadoPor;} public Instant getRevocadoEn(){return revocadoEn;} public String getRevocadoPor(){return revocadoPor;} public String getMotivoRevocacion(){return motivoRevocacion;}
}
