package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;

/** Caso de fiscalizacion con su evidencia. Lo ven el funcionario (bandeja) y la empresa (sus infracciones). */
public class CasoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** EXCESO_CARGA, SIN_HABILITACION, VIAJE_SIN_GUIA o DESVIO_RUTA. */
    private String tipo;
    /** DETECTADA, NOTIFICADA, EN_DESCARGO, CONFIRMADA o DESESTIMADA. */
    private String estado;
    private Long empresaId;
    private String empresaNombre;
    private String matricula;
    private Long guiaId;
    /** Descripcion legible de los datos que originaron el caso. */
    private String evidencia;
    /** ISO-8601. */
    private String detectadaEn;
    /** ISO-8601. */
    private String notificadaEn;
    /** ISO-8601: hasta cuando la empresa puede presentar su descargo. */
    private String venceDescargo;
    /** true si vencio el plazo sin descargo: el funcionario ya puede resolver. */
    private boolean plazoVencido;
    /** true si la empresa todavia puede presentar su descargo. */
    private boolean admiteDescargo;
    /** ISO-8601. */
    private String resueltaEn;
    private String resueltaPor;
    /** Fundamento de la resolucion. */
    private String fundamento;
    /** null si la empresa no presento descargo. */
    private DescargoDTO descargo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getEmpresaNombre() { return empresaNombre; }
    public void setEmpresaNombre(String empresaNombre) { this.empresaNombre = empresaNombre; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }

    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }

    public String getDetectadaEn() { return detectadaEn; }
    public void setDetectadaEn(String detectadaEn) { this.detectadaEn = detectadaEn; }

    public String getNotificadaEn() { return notificadaEn; }
    public void setNotificadaEn(String notificadaEn) { this.notificadaEn = notificadaEn; }

    public String getVenceDescargo() { return venceDescargo; }
    public void setVenceDescargo(String venceDescargo) { this.venceDescargo = venceDescargo; }

    public boolean isPlazoVencido() { return plazoVencido; }
    public void setPlazoVencido(boolean plazoVencido) { this.plazoVencido = plazoVencido; }

    public boolean isAdmiteDescargo() { return admiteDescargo; }
    public void setAdmiteDescargo(boolean admiteDescargo) { this.admiteDescargo = admiteDescargo; }

    public String getResueltaEn() { return resueltaEn; }
    public void setResueltaEn(String resueltaEn) { this.resueltaEn = resueltaEn; }

    public String getResueltaPor() { return resueltaPor; }
    public void setResueltaPor(String resueltaPor) { this.resueltaPor = resueltaPor; }

    public String getFundamento() { return fundamento; }
    public void setFundamento(String fundamento) { this.fundamento = fundamento; }

    public DescargoDTO getDescargo() { return descargo; }
    public void setDescargo(DescargoDTO descargo) { this.descargo = descargo; }

}
