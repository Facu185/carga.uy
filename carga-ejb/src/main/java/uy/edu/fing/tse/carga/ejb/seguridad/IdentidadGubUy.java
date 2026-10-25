package uy.edu.fing.tse.carga.ejb.seguridad;

/** Datos de UserInfo de gub.uy, vinculados al subject de un id_token ya validado. */
public record IdentidadGubUy(String sub, String documento, String email, String nombre) {
}
