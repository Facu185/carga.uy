package uy.edu.fing.tse.carga.ejb.seguridad;

import uy.edu.fing.tse.carga.ejb.modelo.Rol;

/** Lo que devuelve el directorio para alguien que se autentico correctamente y tiene un rol interno. */
public record IdentidadLdap(String uid, String nombre, Rol rol) {
}
