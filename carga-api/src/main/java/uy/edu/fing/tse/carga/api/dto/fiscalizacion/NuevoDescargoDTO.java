package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;

/** Lo que manda la empresa al presentar su descargo. */
public class NuevoDescargoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fundamento;

    public String getFundamento() { return fundamento; }
    public void setFundamento(String fundamento) { this.fundamento = fundamento; }

}
