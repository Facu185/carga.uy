package uy.edu.fing.tse.carga.api.dto.empresa;

import java.io.Serializable;

public class SolicitudAltaEmpresaDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id; private String cedula; private String nombreSolicitante; private String nombreEmpresa;
    private String razonSocial; private String direccion; private long numeroEmpresa; private String estado; private String creadaEn;
    private String resueltaEn; private String resueltaPor; private String motivoRechazo; private Long empresaId;
    public SolicitudAltaEmpresaDTO() { }
    public SolicitudAltaEmpresaDTO(Long id, String cedula, String nombreSolicitante, String nombreEmpresa, String razonSocial, String direccion,
                                   long numeroEmpresa, String estado, String creadaEn, String resueltaEn,
                                   String resueltaPor, String motivoRechazo, Long empresaId) {
        this.id=id; this.cedula=cedula; this.nombreSolicitante=nombreSolicitante; this.nombreEmpresa=nombreEmpresa;
        this.razonSocial=razonSocial; this.direccion=direccion; this.numeroEmpresa=numeroEmpresa; this.estado=estado; this.creadaEn=creadaEn;
        this.resueltaEn=resueltaEn; this.resueltaPor=resueltaPor; this.motivoRechazo=motivoRechazo; this.empresaId=empresaId;
    }
    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public String getCedula() { return cedula; } public void setCedula(String v) { cedula=v; }
    public String getNombreSolicitante() { return nombreSolicitante; } public void setNombreSolicitante(String v) { nombreSolicitante=v; }
    public String getNombreEmpresa() { return nombreEmpresa; } public void setNombreEmpresa(String v) { nombreEmpresa=v; }
    public String getRazonSocial() { return razonSocial; } public void setRazonSocial(String v) { razonSocial=v; }
    public String getDireccion() { return direccion; } public void setDireccion(String v) { direccion=v; }
    public long getNumeroEmpresa() { return numeroEmpresa; } public void setNumeroEmpresa(long v) { numeroEmpresa=v; }
    public String getEstado() { return estado; } public void setEstado(String v) { estado=v; }
    public String getCreadaEn() { return creadaEn; } public void setCreadaEn(String v) { creadaEn=v; }
    public String getResueltaEn() { return resueltaEn; } public void setResueltaEn(String v) { resueltaEn=v; }
    public String getResueltaPor() { return resueltaPor; } public void setResueltaPor(String v) { resueltaPor=v; }
    public String getMotivoRechazo() { return motivoRechazo; } public void setMotivoRechazo(String v) { motivoRechazo=v; }
    public Long getEmpresaId() { return empresaId; } public void setEmpresaId(Long v) { empresaId=v; }
}
