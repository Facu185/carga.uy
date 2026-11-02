package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;

/** Casos de un tipo en el periodo del reporte. */
public class ConteoInfraccionesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tipo;
    private long detectados;
    private long confirmados;
    private long desestimados;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public long getDetectados() { return detectados; }
    public void setDetectados(long detectados) { this.detectados = detectados; }

    public long getConfirmados() { return confirmados; }
    public void setConfirmados(long confirmados) { this.confirmados = confirmados; }

    public long getDesestimados() { return desestimados; }
    public void setDesestimados(long desestimados) { this.desestimados = desestimados; }

}
