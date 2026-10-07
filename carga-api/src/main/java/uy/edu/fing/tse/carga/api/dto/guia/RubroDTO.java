package uy.edu.fing.tse.carga.api.dto.guia;

import java.io.Serializable;

/** Elemento del catÃ¡logo de rubros de actividad. */
public class RubroDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;

    public RubroDTO() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
