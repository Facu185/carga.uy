package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/** Datos necesarios para crear y asignar una guía en una única operación. */
public class CrearGuiaCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long vehiculoId;
    private Long choferId;
    private Long rubroId;
    private Long tipoCargaId;
    private BigDecimal cargaKg;
    private String fecha;
    private String origen;
    private Double origenLat;
    private Double origenLon;
    private String destino;
    private Double destinoLat;
    private Double destinoLon;

    public CrearGuiaCommand() { }

    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }
    public Long getChoferId() { return choferId; }
    public void setChoferId(Long choferId) { this.choferId = choferId; }
    public Long getRubroId() { return rubroId; }
    public void setRubroId(Long rubroId) { this.rubroId = rubroId; }
    public Long getTipoCargaId() { return tipoCargaId; }
    public void setTipoCargaId(Long tipoCargaId) { this.tipoCargaId = tipoCargaId; }
    public BigDecimal getCargaKg() { return cargaKg; }
    public void setCargaKg(BigDecimal cargaKg) { this.cargaKg = cargaKg; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public Double getOrigenLat() { return origenLat; }
    public void setOrigenLat(Double origenLat) { this.origenLat = origenLat; }
    public Double getOrigenLon() { return origenLon; }
    public void setOrigenLon(Double origenLon) { this.origenLon = origenLon; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public Double getDestinoLat() { return destinoLat; }
    public void setDestinoLat(Double destinoLat) { this.destinoLat = destinoLat; }
    public Double getDestinoLon() { return destinoLon; }
    public void setDestinoLon(Double destinoLon) { this.destinoLon = destinoLon; }
}
