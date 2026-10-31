package uy.edu.fing.tse.carga.api.exception;

/**
 * La operacion es valida en forma, pero choca con el estado actual de un recurso (por ejemplo, un
 * evento que el viaje ya no admite). La capa web la devuelve como 409.
 */
public class ConflictoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
