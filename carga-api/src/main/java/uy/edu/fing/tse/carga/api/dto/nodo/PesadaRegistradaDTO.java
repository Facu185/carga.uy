package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;

/**
 * Pesada ya guardada en el central. A diferencia de PesadaDTO (lo que manda el nodo), lleva la
 * guia a la que se vinculo: guiaId null significa que el vehiculo fue pesado sin una guia vigente,
 * dato que usa la deteccion de "viaje sin guia" (frente P5).
 */
public class PesadaRegistradaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String idPesada;
    private Long guiaId;
    private String matricula;
    private String peaje;
    private int pesoKg;
    /** ISO-8601, marca de tiempo de la balanza. */
    private String instante;
    /** ISO-8601, cuando la obtuvo el central. */
    private String obtenidaEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdPesada() { return idPesada; }
    public void setIdPesada(String idPesada) { this.idPesada = idPesada; }

    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getPeaje() { return peaje; }
    public void setPeaje(String peaje) { this.peaje = peaje; }

    public int getPesoKg() { return pesoKg; }
    public void setPesoKg(int pesoKg) { this.pesoKg = pesoKg; }

    public String getInstante() { return instante; }
    public void setInstante(String instante) { this.instante = instante; }

    public String getObtenidaEn() { return obtenidaEn; }
    public void setObtenidaEn(String obtenidaEn) { this.obtenidaEn = obtenidaEn; }
}
