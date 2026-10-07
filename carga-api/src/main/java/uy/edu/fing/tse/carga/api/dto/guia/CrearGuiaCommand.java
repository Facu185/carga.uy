package uy.edu.fing.tse.carga.api.dto.guia;

import java.io.Serializable;
import java.math.BigDecimal;

/** Datos necesarios para crear y asignar una guÃ­a en una Ãºnica operaciÃ³n. */
public class CrearGuiaCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long vehiculoId;
    private Long choferId;
    private Long rubroId;
    private Long tipoCargaId;
    private BigDecimal pesoKg;
    private BigDecimal volumenM3;
    private String inicio;
    private String fin;
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
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public BigDecimal getVolumenM3() { return volumenM3; }
    public void setVolumenM3(BigDecimal volumenM3) { this.volumenM3 = volumenM3; }
    public String getInicio() { return inicio; }
    public void setInicio(String inicio) { this.inicio = inicio; }
    public String getFin() { return fin; }
    public void setFin(String fin) { this.fin = fin; }
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
