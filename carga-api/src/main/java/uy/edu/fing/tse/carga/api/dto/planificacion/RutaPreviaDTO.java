package uy.edu.fing.tse.carga.api.dto.planificacion;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** Contrato de vista previa ADR-0024; no representa un recorrido persistido. */
public class RutaPreviaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<PuntoMapaDTO> puntos;
    private BigDecimal distanciaMetros;
    private BigDecimal duracionSegundos;
    private BigDecimal pesoTotalKg;
    private boolean advertenciasProveedor;

    public RutaPreviaDTO() { }

    public List<PuntoMapaDTO> getPuntos() { return puntos; }
    public void setPuntos(List<PuntoMapaDTO> puntos) { this.puntos = puntos; }
    public BigDecimal getDistanciaMetros() { return distanciaMetros; }
    public void setDistanciaMetros(BigDecimal distanciaMetros) { this.distanciaMetros = distanciaMetros; }
    public BigDecimal getDuracionSegundos() { return duracionSegundos; }
    public void setDuracionSegundos(BigDecimal duracionSegundos) { this.duracionSegundos = duracionSegundos; }
    public BigDecimal getPesoTotalKg() { return pesoTotalKg; }
    public void setPesoTotalKg(BigDecimal pesoTotalKg) { this.pesoTotalKg = pesoTotalKg; }
    public boolean isAdvertenciasProveedor() { return advertenciasProveedor; }
    public void setAdvertenciasProveedor(boolean advertenciasProveedor) { this.advertenciasProveedor = advertenciasProveedor; }
}
