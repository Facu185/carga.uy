package uy.edu.fing.tse.carga.api.dto.comun;

import java.io.Serializable;

/**
 * Pesada devuelta por el nodo Balanzas.
 * CONTRATO acordado con el nodo periferico: ver docs/contratos.md
 */
public class PesadaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idPesada;
    private String matricula;
    private String peaje;
    private int pesoKg;
    private String instante;

    public PesadaDTO() {
    }

    public String getIdPesada() { return idPesada; }
    public void setIdPesada(String idPesada) { this.idPesada = idPesada; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getPeaje() { return peaje; }
    public void setPeaje(String peaje) { this.peaje = peaje; }

    public int getPesoKg() { return pesoKg; }
    public void setPesoKg(int pesoKg) { this.pesoKg = pesoKg; }

    public String getInstante() { return instante; }
    public void setInstante(String instante) { this.instante = instante; }
}
