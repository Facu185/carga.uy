package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

public class RechazoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String motivo;

    public RechazoDTO() {
    }

    public RechazoDTO(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

}
