package uy.edu.fing.tse.carga.api.dto.guia;

import java.io.Serializable;

/** Guia con datos de sus asignaciones, incluidos choferes actualmente inactivos. */
public class GuiaEmpresaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private GuiaDTO guia;
    private String nombreChofer;
    private String matricula;

    public GuiaEmpresaDTO() { }

    public GuiaDTO getGuia() { return guia; }
    public void setGuia(GuiaDTO guia) { this.guia = guia; }
    public String getNombreChofer() { return nombreChofer; }
    public void setNombreChofer(String nombreChofer) { this.nombreChofer = nombreChofer; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
}
