package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/** Contadores de la ingesta de posiciones, para el panel de operacion. */
public class MetricasDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long posicionesTotales;
    private long posicionesUltimosCincoMinutos;
    private long vehiculosReportando;
    private String ultimaRecepcion;

    public long getPosicionesTotales() { return posicionesTotales; }
    public void setPosicionesTotales(long v) { this.posicionesTotales = v; }

    public long getPosicionesUltimosCincoMinutos() { return posicionesUltimosCincoMinutos; }
    public void setPosicionesUltimosCincoMinutos(long v) { this.posicionesUltimosCincoMinutos = v; }

    public long getVehiculosReportando() { return vehiculosReportando; }
    public void setVehiculosReportando(long v) { this.vehiculosReportando = v; }

    public String getUltimaRecepcion() { return ultimaRecepcion; }
    public void setUltimaRecepcion(String v) { this.ultimaRecepcion = v; }
}
