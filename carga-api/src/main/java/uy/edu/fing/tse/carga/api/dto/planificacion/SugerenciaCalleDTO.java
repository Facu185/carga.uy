package uy.edu.fing.tse.carga.api.dto.planificacion;

import java.io.Serializable;

/** Contrato de vista previa ADR-0024; no representa un recorrido persistido. */
public class SugerenciaCalleDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String direccion;
    private String tipo;
    private Long idCalle;
    private Long idCalleEsquina;
    private Integer numeroPuerta;
    private String letra;

    public SugerenciaCalleDTO() { }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Long getIdCalle() { return idCalle; }
    public void setIdCalle(Long idCalle) { this.idCalle = idCalle; }
    public Long getIdCalleEsquina() { return idCalleEsquina; }
    public void setIdCalleEsquina(Long idCalleEsquina) { this.idCalleEsquina = idCalleEsquina; }
    public Integer getNumeroPuerta() { return numeroPuerta; }
    public void setNumeroPuerta(Integer numeroPuerta) { this.numeroPuerta = numeroPuerta; }
    public String getLetra() { return letra; }
    public void setLetra(String letra) { this.letra = letra; }
}
