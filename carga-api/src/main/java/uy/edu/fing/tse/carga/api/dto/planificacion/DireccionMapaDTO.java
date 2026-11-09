package uy.edu.fing.tse.carga.api.dto.planificacion;

import java.io.Serializable;

/** Contrato de vista previa ADR-0024; no representa un recorrido persistido. */
public class DireccionMapaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String direccion;
    private PuntoMapaDTO punto;
    private boolean aproximada;
    private String detalle;

    public DireccionMapaDTO() { }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public PuntoMapaDTO getPunto() { return punto; }
    public void setPunto(PuntoMapaDTO punto) { this.punto = punto; }
    public boolean isAproximada() { return aproximada; }
    public void setAproximada(boolean aproximada) { this.aproximada = aproximada; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
