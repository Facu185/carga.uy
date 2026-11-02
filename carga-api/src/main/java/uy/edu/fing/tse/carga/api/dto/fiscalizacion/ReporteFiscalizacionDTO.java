package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Indicadores de fiscalizacion para la autoridad: infracciones por tipo y tasa de confirmacion. */
public class ReporteFiscalizacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** aaaa-mm-dd, incluido. */
    private String desde;
    /** aaaa-mm-dd, incluido. */
    private String hasta;
    private long detectados;
    private long confirmados;
    private long desestimados;
    /** Todavia sin resolver. */
    private long pendientes;
    /** Confirmados sobre detectados, entre 0 y 1; null si no hubo casos. */
    private Double tasaConfirmacion;
    private List<ConteoInfraccionesDTO> porTipo = new ArrayList<>();

    public String getDesde() { return desde; }
    public void setDesde(String desde) { this.desde = desde; }

    public String getHasta() { return hasta; }
    public void setHasta(String hasta) { this.hasta = hasta; }

    public long getDetectados() { return detectados; }
    public void setDetectados(long detectados) { this.detectados = detectados; }

    public long getConfirmados() { return confirmados; }
    public void setConfirmados(long confirmados) { this.confirmados = confirmados; }

    public long getDesestimados() { return desestimados; }
    public void setDesestimados(long desestimados) { this.desestimados = desestimados; }

    public long getPendientes() { return pendientes; }
    public void setPendientes(long pendientes) { this.pendientes = pendientes; }

    public Double getTasaConfirmacion() { return tasaConfirmacion; }
    public void setTasaConfirmacion(Double tasaConfirmacion) { this.tasaConfirmacion = tasaConfirmacion; }

    public List<ConteoInfraccionesDTO> getPorTipo() { return porTipo; }
    public void setPorTipo(List<ConteoInfraccionesDTO> porTipo) { this.porTipo = porTipo; }

}
