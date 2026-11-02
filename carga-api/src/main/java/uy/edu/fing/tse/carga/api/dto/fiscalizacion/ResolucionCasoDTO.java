package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;

/** Lo que manda el funcionario al resolver un caso. */
public class ResolucionCasoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** true confirma la infraccion; false la desestima. */
    private boolean confirmar;
    /** Obligatorio: queda en el caso y en la auditoria. */
    private String fundamento;

    public boolean isConfirmar() { return confirmar; }
    public void setConfirmar(boolean confirmar) { this.confirmar = confirmar; }

    public String getFundamento() { return fundamento; }
    public void setFundamento(String fundamento) { this.fundamento = fundamento; }

}
