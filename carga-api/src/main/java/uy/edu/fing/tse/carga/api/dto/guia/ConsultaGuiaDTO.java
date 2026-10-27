package uy.edu.fing.tse.carga.api.dto.guia;

import java.io.Serializable;

/** C14: guia y datos actuales relacionados, no una instantanea ni un historial real. */
public class ConsultaGuiaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private GuiaDTO guia;
    private String nombreEmpresa;
    private String matricula;
    private String nombreChofer;

    public ConsultaGuiaDTO() { }

    public GuiaDTO getGuia() { return guia; }
    public void setGuia(GuiaDTO guia) { this.guia = guia; }
    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getNombreChofer() { return nombreChofer; }
    public void setNombreChofer(String nombreChofer) { this.nombreChofer = nombreChofer; }
}
