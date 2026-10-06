package uy.edu.fing.tse.carga.api.exception;

import jakarta.ejb.ApplicationException;

/** Se lanza cuando no se cumple una regla de negocio. Deshace la transaccion. */
@ApplicationException(rollback = true)
public class NegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
