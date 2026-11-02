package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;

/** Umbral de una regla de fiscalizacion, modificable sin redesplegar. */
public class ParametroReglaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String clave;
    private String valor;
    private String descripcion;

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

}
