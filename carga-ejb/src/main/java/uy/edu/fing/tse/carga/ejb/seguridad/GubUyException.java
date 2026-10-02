package uy.edu.fing.tse.carga.ejb.seguridad;

/** Falla en la comunicacion con gub.uy o respuesta invalida. El mensaje se le muestra al usuario. */
public class GubUyException extends RuntimeException {
    public GubUyException(String mensaje) {
        super(mensaje);
    }

    public GubUyException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
