package uy.edu.fing.tse.carga.api.dto.reporte;

import java.io.Serializable;

/** Grupo agregado; clave estable independiente de su etiqueta de presentacion. */
public class ConteoActividadDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String clave;
    private String etiqueta;
    private long cantidad;

    public ConteoActividadDTO() { }

    public ConteoActividadDTO(String clave, String etiqueta, long cantidad) {
        this.clave = clave; this.etiqueta = etiqueta; this.cantidad = cantidad;
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }
}
