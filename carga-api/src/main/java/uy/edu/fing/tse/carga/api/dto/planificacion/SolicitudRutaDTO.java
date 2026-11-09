package uy.edu.fing.tse.carga.api.dto.planificacion;

import java.io.Serializable;
import java.math.BigDecimal;

/** Contrato de vista previa ADR-0024; no representa un recorrido persistido. */
public class SolicitudRutaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long vehiculoId;
    private BigDecimal cargaKg;
    private PuntoMapaDTO origen;
    private PuntoMapaDTO destino;

    public SolicitudRutaDTO() { }

    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }
    public BigDecimal getCargaKg() { return cargaKg; }
    public void setCargaKg(BigDecimal cargaKg) { this.cargaKg = cargaKg; }
    public PuntoMapaDTO getOrigen() { return origen; }
    public void setOrigen(PuntoMapaDTO origen) { this.origen = origen; }
    public PuntoMapaDTO getDestino() { return destino; }
    public void setDestino(PuntoMapaDTO destino) { this.destino = destino; }
}
