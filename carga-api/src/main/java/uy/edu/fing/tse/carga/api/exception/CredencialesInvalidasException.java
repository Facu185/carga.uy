package uy.edu.fing.tse.carga.api.exception;

import jakarta.ejb.ApplicationException;

/**
 * Mensaje generico a proposito: no revela si el usuario existe.
 * rollback = false: el intento fallido (contador de bloqueo y auditoria) tiene que quedar guardado.
 */
@ApplicationException(rollback = false)
public class CredencialesInvalidasException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CredencialesInvalidasException() {
        super("Usuario o contrase\u00f1a incorrectos");
    }
}
