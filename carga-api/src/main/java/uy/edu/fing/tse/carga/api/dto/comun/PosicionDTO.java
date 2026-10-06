package uy.edu.fing.tse.carga.api.dto.comun;

import java.io.Serializable;

/**
 * Posicion reportada por el nodo Tracking.
 * CONTRATO acordado con el nodo periferico: ver docs/contratos.md
 */
public class PosicionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Identificador generado en el origen: permite descartar duplicados (idempotencia). */
    private String idEvento;
    private String matricula;
    private double latitud;
    private double longitud;
    /** Marca de tiempo del evento en el origen, en ISO-8601. Se usa para ordenar, no el orden de llegada. */
    private String instante;

    public PosicionDTO() {
    }

    public String getIdEvento() { return idEvento; }
    public void setIdEvento(String idEvento) { this.idEvento = idEvento; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public String getInstante() { return instante; }
    public void setInstante(String instante) { this.instante = instante; }

    @Override
    public String toString() {
        return "PosicionDTO{" + idEvento + " " + matricula + " (" + latitud + "," + longitud + ") " + instante + '}';
    }
}
