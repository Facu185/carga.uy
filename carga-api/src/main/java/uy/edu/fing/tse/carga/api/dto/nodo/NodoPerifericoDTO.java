package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;

/** Nodo periferico registrado, con su estado observado en el momento de la consulta. */
public class NodoPerifericoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    /** BALANZAS o TRACKING. */
    private String tipo;
    /** Solo los nodos Balanzas: el nodo Tracking empuja y no se consulta. */
    private String url;
    private boolean activo;
    /** ISO-8601. */
    private String actualizadoEn;
    /** null si el nodo esta desactivado: no se verifica. */
    private Boolean enLinea;
    private String detalle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(String actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    public Boolean getEnLinea() { return enLinea; }
    public void setEnLinea(Boolean enLinea) { this.enLinea = enLinea; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
