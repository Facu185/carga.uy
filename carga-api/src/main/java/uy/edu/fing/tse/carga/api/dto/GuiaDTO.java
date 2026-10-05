package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/** Representación de una guía de viaje de carga ya registrada. */
public class GuiaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long empresaId;
    private Long vehiculoId;
    private Long choferId;
    private Long rubroId;
    private Long tipoCargaId;
    private BigDecimal pesoKg;
    private BigDecimal volumenM3;
    private String fecha;
    private String origen;
    private Double origenLat;
    private Double origenLon;
    private String destino;
    private Double destinoLat;
    private Double destinoLon;
    private String estado;
    private String creadaEn;

    public GuiaDTO() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }
    public Long getChoferId() { return choferId; }
    public void setChoferId(Long choferId) { this.choferId = choferId; }
    public Long getRubroId() { return rubroId; }
    public void setRubroId(Long rubroId) { this.rubroId = rubroId; }
    public Long getTipoCargaId() { return tipoCargaId; }
    public void setTipoCargaId(Long tipoCargaId) { this.tipoCargaId = tipoCargaId; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public BigDecimal getVolumenM3() { return volumenM3; }
    public void setVolumenM3(BigDecimal volumenM3) { this.volumenM3 = volumenM3; }
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
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCreadaEn() { return creadaEn; }
    public void setCreadaEn(String creadaEn) { this.creadaEn = creadaEn; }
}
