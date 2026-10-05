package uy.edu.fing.tse.carga.ejb.seguridad;

/** Lo que nos interesa del id_token de gub.uy, ya validado. */
public record IdentidadGubUy(String sub, String documento, String email, String nombre) {

    /** Con documento y nombre alcanza para ingresar; el correo es opcional (la persona puede no compartirlo). */
    public boolean estaCompleta() {
        return documento != null && !documento.isBlank() && nombre != null && !nombre.isBlank();
    }
}
