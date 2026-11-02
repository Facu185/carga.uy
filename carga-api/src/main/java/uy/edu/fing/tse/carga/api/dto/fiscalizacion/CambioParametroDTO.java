package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;

/** Nuevo valor de un parametro y el motivo del cambio (queda en la auditoria). */
public class CambioParametroDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String valor;
    private String motivo;

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

}
