package uy.edu.fing.tse.carga.api.dto.viaje;

import java.io.Serializable;

/**
 * Evento de un viaje registrado por el chofer desde el componente movil (caso de uso critico 3).
 *
 * idEvento lo genera el dispositivo: si la app reenvia el mismo evento, el central no lo duplica.
 * fotoBase64 solo viaja hacia el central; en las respuestas va null y tieneFoto indica si hay foto.
 */
public class EventoViajeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idEvento;
    private Long guiaId;
    /** INICIO, FIN, CARGA_PARCIAL, DESCARGA_PARCIAL o INCIDENTE. */
    private String tipo;
    /** ISO-8601 con zona: el momento en que ocurrio en el dispositivo, no cuando llego al central. */
    private String instante;
    private String detalle;
    private Double latitud;
    private Double longitud;
    /** JPEG en base64, opcional. */
    private String fotoBase64;
    private boolean tieneFoto;
    /** ISO-8601, lo completa el central. */
    private String recibidoEn;

    public EventoViajeDTO() {
    }

    public String getIdEvento() { return idEvento; }
    public void setIdEvento(String idEvento) { this.idEvento = idEvento; }

    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getInstante() { return instante; }
    public void setInstante(String instante) { this.instante = instante; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public String getFotoBase64() { return fotoBase64; }
    public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }

    public boolean isTieneFoto() { return tieneFoto; }
    public void setTieneFoto(boolean tieneFoto) { this.tieneFoto = tieneFoto; }

    public String getRecibidoEn() { return recibidoEn; }
    public void setRecibidoEn(String recibidoEn) { this.recibidoEn = recibidoEn; }

    /** Sin la foto: este toString puede terminar en un log. */
    @Override
    public String toString() {
        return "EventoViajeDTO{idEvento=" + idEvento + ", guiaId=" + guiaId + ", tipo=" + tipo
                + ", instante=" + instante + ", conFoto=" + (fotoBase64 != null || tieneFoto) + "}";
    }
}
