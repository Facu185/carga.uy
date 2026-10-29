package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Pesadas vinculadas a una guia (frente P4), para el monitoreo de viajes del funcionario (F14, P2).
 * Son las que la sincronizacion asocio a la guia al guardarlas, de todos los dias del viaje: no se
 * vuelve a asociar por matricula y fecha. Una pesada sin guia vigente (guiaId null) nunca aparece aca.
 */
public class PesajesDeGuiaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long guiaId;
    /** Matricula actual del vehiculo de la guia. */
    private String matricula;
    /** ISO-8601, inicio del periodo de la guia. */
    private String desde;
    /** ISO-8601, fin del periodo de la guia, excluido. */
    private String hasta;
    /** Ordenadas por la hora de la balanza. */
    private List<PesadaRegistradaDTO> pesadas = new ArrayList<>();
    /** true si habia mas pesadas que el maximo y se devolvieron solo las primeras. */
    private boolean truncado;
    /**
     * false si el nodo Balanzas esta caido ahora (circuito abierto): puede haber pesajes del viaje
     * que todavia no se obtuvieron. La agenda los vuelve a pedir cuando el nodo responda.
     */
    private boolean nodoDisponible;

    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getDesde() { return desde; }
    public void setDesde(String desde) { this.desde = desde; }

    public String getHasta() { return hasta; }
    public void setHasta(String hasta) { this.hasta = hasta; }

    public List<PesadaRegistradaDTO> getPesadas() { return pesadas; }
    public void setPesadas(List<PesadaRegistradaDTO> pesadas) { this.pesadas = pesadas; }

    public int getCantidad() { return pesadas.size(); }

    public boolean isTruncado() { return truncado; }
    public void setTruncado(boolean truncado) { this.truncado = truncado; }

    public boolean isNodoDisponible() { return nodoDisponible; }
    public void setNodoDisponible(boolean nodoDisponible) { this.nodoDisponible = nodoDisponible; }
}
