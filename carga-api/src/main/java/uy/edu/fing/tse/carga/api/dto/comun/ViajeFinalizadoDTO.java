package uy.edu.fing.tse.carga.api.dto.comun;

import java.io.Serializable;

/** Aviso de que termino un viaje (lo emite el frente P3 al registrar el evento de fin). */
public class ViajeFinalizadoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long guiaId;
    private String matricula;
    /** ISO-8601 con zona. Opcional: sin inicio se toma solo el dia del fin. */
    private String inicio;
    /** ISO-8601 con zona. Obligatorio. */
    private String fin;

    public ViajeFinalizadoDTO() {
    }

    public ViajeFinalizadoDTO(Long guiaId, String matricula, String inicio, String fin) {
        this.guiaId = guiaId;
        this.matricula = matricula;
        this.inicio = inicio;
        this.fin = fin;
    }

    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getInicio() { return inicio; }
    public void setInicio(String inicio) { this.inicio = inicio; }

    public String getFin() { return fin; }
    public void setFin(String fin) { this.fin = fin; }
}
