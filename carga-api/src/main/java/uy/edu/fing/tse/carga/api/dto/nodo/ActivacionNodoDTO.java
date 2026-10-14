package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;

/** Activar o desactivar un nodo. Desactivar exige fundamento: deja al central sin esa integracion. */
public class ActivacionNodoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean activo;
    private String fundamento;

    public ActivacionNodoDTO() {
    }

    public ActivacionNodoDTO(boolean activo, String fundamento) {
        this.activo = activo;
        this.fundamento = fundamento;
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getFundamento() { return fundamento; }
    public void setFundamento(String fundamento) { this.fundamento = fundamento; }
}
