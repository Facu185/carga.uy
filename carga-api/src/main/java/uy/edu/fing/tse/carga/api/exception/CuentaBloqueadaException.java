package uy.edu.fing.tse.carga.api.exception;

import jakarta.ejb.ApplicationException;

/** rollback = false: el bloqueo y su registro en la auditoria tienen que quedar guardados. */
@ApplicationException(rollback = false)
public class CuentaBloqueadaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CuentaBloqueadaException(long minutos) {
        super("Demasiados intentos fallidos. Prob\u00e1 de nuevo en " + Math.max(1, minutos) + " minutos.");
    }
}
