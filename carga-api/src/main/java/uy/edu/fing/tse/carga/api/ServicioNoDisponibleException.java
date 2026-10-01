package uy.edu.fing.tse.carga.api;

import jakarta.ejb.ApplicationException;

/**
 * Un sistema externo (directorio LDAP, gub.uy) no respondio o dio un error tecnico.
 * No es un error de credenciales ni de negocio: la capa web lo devuelve como 503.
 * Es ApplicationException para que el contenedor no la envuelva en EJBException.
 */
@ApplicationException(rollback = true)
public class ServicioNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
