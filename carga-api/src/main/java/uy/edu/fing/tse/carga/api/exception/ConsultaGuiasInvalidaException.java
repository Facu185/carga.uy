package uy.edu.fing.tse.carga.api.exception;

import jakarta.ejb.ApplicationException;

/** Parametros invalidos de consulta; conserva la semantica de excepcion de aplicacion EJB. */
@ApplicationException(rollback = true)
public class ConsultaGuiasInvalidaException extends NegocioException {
    private static final long serialVersionUID = 1L;

    public ConsultaGuiasInvalidaException(String mensaje) { super(mensaje); }
}
