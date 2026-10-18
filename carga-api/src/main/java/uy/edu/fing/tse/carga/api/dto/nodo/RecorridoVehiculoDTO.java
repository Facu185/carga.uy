package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Recorrido de un vehiculo en un periodo [desde, hasta), ordenado por la hora del dispositivo.
 * Es la evidencia que usa la regla de desvio de ruta (frente P5).
 */
public class RecorridoVehiculoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String matricula;
    /** Solo si se pidio por guia. */
    private Long guiaId;
    /** ISO-8601. */
    private String desde;
    /** ISO-8601, excluido. */
    private String hasta;
    private List<PuntoRecorridoDTO> puntos = new ArrayList<>();
    /** true si habia mas puntos que el maximo y se devolvieron solo los primeros. */
    private boolean truncado;
    /** Largo del recorrido completo del periodo (aunque este truncado), en metros sobre la Tierra. */
    private double distanciaMetros;

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }

    public String getDesde() { return desde; }
    public void setDesde(String desde) { this.desde = desde; }

    public String getHasta() { return hasta; }
    public void setHasta(String hasta) { this.hasta = hasta; }

    public List<PuntoRecorridoDTO> getPuntos() { return puntos; }
    public void setPuntos(List<PuntoRecorridoDTO> puntos) { this.puntos = puntos; }

    public int getCantidad() { return puntos == null ? 0 : puntos.size(); }

    public boolean isTruncado() { return truncado; }
    public void setTruncado(boolean truncado) { this.truncado = truncado; }

    public double getDistanciaMetros() { return distanciaMetros; }
    public void setDistanciaMetros(double distanciaMetros) { this.distanciaMetros = distanciaMetros; }
}
