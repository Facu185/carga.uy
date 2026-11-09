package uy.edu.fing.tse.carga.api.dto.planificacion;

import java.io.Serializable;

/** Contrato de vista previa ADR-0024; no representa un recorrido persistido. */
public class PuntoMapaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Double latitud;
    private Double longitud;

    public PuntoMapaDTO() { }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
}
