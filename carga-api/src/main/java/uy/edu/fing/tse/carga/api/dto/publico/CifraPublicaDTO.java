package uy.edu.fing.tse.carga.api.dto.publico;

import java.io.Serializable;

/**
 * Una celda de las estadisticas publicas. Si esta reservada, guias y toneladas vienen en null:
 * publicarla permitiria identificar a una empresa (ADR-0021).
 */
public class CifraPublicaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String clave;
    private String etiqueta;
    private boolean reservada;
    private Long guias;
    private Double toneladas;

    public CifraPublicaDTO() { }

    public CifraPublicaDTO(String clave, String etiqueta, boolean reservada, Long guias, Double toneladas) {
        this.clave = clave; this.etiqueta = etiqueta; this.reservada = reservada;
        this.guias = guias; this.toneladas = toneladas;
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

    public boolean isReservada() { return reservada; }
    public void setReservada(boolean reservada) { this.reservada = reservada; }

    public Long getGuias() { return guias; }
    public void setGuias(Long guias) { this.guias = guias; }

    public Double getToneladas() { return toneladas; }
    public void setToneladas(Double toneladas) { this.toneladas = toneladas; }
}
