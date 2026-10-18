package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;

/** Una posicion del recorrido. */
public class PuntoRecorridoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private double latitud;
    private double longitud;
    /** ISO-8601: marca de tiempo del dispositivo, no de llegada al central. */
    private String instante;

    public PuntoRecorridoDTO() {
    }

    public PuntoRecorridoDTO(double latitud, double longitud, String instante) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.instante = instante;
    }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public String getInstante() { return instante; }
    public void setInstante(String instante) { this.instante = instante; }
}
