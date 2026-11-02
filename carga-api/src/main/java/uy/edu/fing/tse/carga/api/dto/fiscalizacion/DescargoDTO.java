package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;

/** Descargo de la empresa sobre un caso. */
public class DescargoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fundamento;
    /** Cedula de quien lo presento. */
    private String presentadoPor;
    /** ISO-8601. */
    private String presentadoEn;

    public String getFundamento() { return fundamento; }
    public void setFundamento(String fundamento) { this.fundamento = fundamento; }

    public String getPresentadoPor() { return presentadoPor; }
    public void setPresentadoPor(String presentadoPor) { this.presentadoPor = presentadoPor; }

    public String getPresentadoEn() { return presentadoEn; }
    public void setPresentadoEn(String presentadoEn) { this.presentadoEn = presentadoEn; }

}
