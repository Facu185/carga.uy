package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;

/** Estado observado de un nodo periferico. */
public class EstadoNodoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String tipo;
    private String url;
    private boolean activo = true;
    private boolean enLinea;
    private String detalle;
    private long latenciaMs;

    public EstadoNodoDTO() {
    }

    public EstadoNodoDTO(String nombre, String tipo, String url) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.url = url;
    }

    /** Texto para la interfaz; evita repetir el if en cada pantalla. */
    public String getEtiqueta() {
        if (!activo) {
            return "Desactivado";
        }
        return enLinea ? "En linea" : "Sin respuesta";
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean v) { this.activo = v; }

    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }

    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }

    public String getUrl() { return url; }
    public void setUrl(String v) { this.url = v; }

    public boolean isEnLinea() { return enLinea; }
    public void setEnLinea(boolean v) { this.enLinea = v; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String v) { this.detalle = v; }

    public long getLatenciaMs() { return latenciaMs; }
    public void setLatenciaMs(long v) { this.latenciaMs = v; }
}
