package uy.edu.fing.tse.carga.api.exception;

/**
 * El recurso no existe o no pertenece a quien lo pide. Se responde igual en los dos casos para no
 * revelar que existe algo ajeno. La capa web la devuelve como 403.
 */
public class RecursoAjenoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public RecursoAjenoException(String mensaje) {
        super(mensaje);
    }
}
