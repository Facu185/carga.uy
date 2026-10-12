package uy.edu.fing.tse.carga.api.dto.empresa;

import java.io.Serializable;

public class VinculoEmpresaDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id; private Long empresaId; private String nombreEmpresa; private String tipo; private String estado;
    private String solicitadoEn; private String aprobadoEn; private String aprobadoPor; private String revocadoEn;
    private String revocadoPor; private String motivoRevocacion;
    public VinculoEmpresaDTO() { }
    public VinculoEmpresaDTO(Long id, Long empresaId, String nombreEmpresa, String tipo, String estado, String solicitadoEn,
                             String aprobadoEn, String aprobadoPor, String revocadoEn, String revocadoPor, String motivoRevocacion) {
        this.id=id; this.empresaId=empresaId; this.nombreEmpresa=nombreEmpresa; this.tipo=tipo; this.estado=estado;
        this.solicitadoEn=solicitadoEn; this.aprobadoEn=aprobadoEn; this.aprobadoPor=aprobadoPor;
        this.revocadoEn=revocadoEn; this.revocadoPor=revocadoPor; this.motivoRevocacion=motivoRevocacion;
    }
    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public Long getEmpresaId() { return empresaId; } public void setEmpresaId(Long v) { empresaId=v; }
    public String getNombreEmpresa() { return nombreEmpresa; } public void setNombreEmpresa(String v) { nombreEmpresa=v; }
    public String getTipo() { return tipo; } public void setTipo(String v) { tipo=v; }
    public String getEstado() { return estado; } public void setEstado(String v) { estado=v; }
    public String getSolicitadoEn() { return solicitadoEn; } public void setSolicitadoEn(String v) { solicitadoEn=v; }
    public String getAprobadoEn() { return aprobadoEn; } public void setAprobadoEn(String v) { aprobadoEn=v; }
    public String getAprobadoPor() { return aprobadoPor; } public void setAprobadoPor(String v) { aprobadoPor=v; }
    public String getRevocadoEn() { return revocadoEn; } public void setRevocadoEn(String v) { revocadoEn=v; }
    public String getRevocadoPor() { return revocadoPor; } public void setRevocadoPor(String v) { revocadoPor=v; }
    public String getMotivoRevocacion() { return motivoRevocacion; } public void setMotivoRevocacion(String v) { motivoRevocacion=v; }
}
