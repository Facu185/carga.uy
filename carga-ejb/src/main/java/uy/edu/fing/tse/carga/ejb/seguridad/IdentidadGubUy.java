package uy.edu.fing.tse.carga.ejb.seguridad;

/** Lo que nos interesa del id_token de gub.uy, ya validado. */
public record IdentidadGubUy(String sub, String documento, String email, String nombre) {
}
