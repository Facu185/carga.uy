package uy.edu.fing.tse.carga.api.dto.guia;

import java.io.Serializable;

/** Elemento del catÃ¡logo de tipos de carga. */
public class TipoCargaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;

    public TipoCargaDTO() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
