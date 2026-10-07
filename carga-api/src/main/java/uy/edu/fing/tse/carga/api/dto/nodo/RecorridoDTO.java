package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Ultima posicion de un vehiculo y su recorrido reciente, para dibujar el mapa. */
public class RecorridoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String matricula;
    private double latitud;
    private double longitud;
    private String instante;
    /** Pares [latitud, longitud] del mas antiguo al mas reciente. */
    private List<double[]> puntos = new ArrayList<>();

    public String getMatricula() { return matricula; }
    public void setMatricula(String v) { this.matricula = v; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double v) { this.latitud = v; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double v) { this.longitud = v; }

    public String getInstante() { return instante; }
    public void setInstante(String v) { this.instante = v; }

    public List<double[]> getPuntos() { return puntos; }
    public void setPuntos(List<double[]> v) { this.puntos = v; }
}
