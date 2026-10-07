package uy.edu.fing.tse.carga.api.dto.comun;

import java.io.Serializable;
import java.util.Map;

/** estado: UP o DOWN. componentes: nombre -> UP, DEGRADADO o DOWN. instante: ISO-8601. */
public class EstadoSaludDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String estado;
    private Map<String, String> componentes;
    private String instante;

    public EstadoSaludDTO() {
    }

    public EstadoSaludDTO(String estado, Map<String, String> componentes, String instante) {
        this.estado = estado;
        this.componentes = componentes;
        this.instante = instante;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Map<String, String> getComponentes() { return componentes; }
    public void setComponentes(Map<String, String> componentes) { this.componentes = componentes; }

    public String getInstante() { return instante; }
    public void setInstante(String instante) { this.instante = instante; }
}
