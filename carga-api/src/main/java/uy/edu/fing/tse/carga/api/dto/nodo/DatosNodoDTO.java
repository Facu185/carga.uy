package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;

/** Alta o modificacion de un nodo. El tipo solo se usa en el alta: despues no cambia. */
public class DatosNodoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private String tipo;
    private String url;

    public DatosNodoDTO() {
    }

    public DatosNodoDTO(String nombre, String tipo, String url) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.url = url;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
