package uy.edu.fing.tse.carga.ejb.modelo;

/** Mismos nombres que la tabla rol (cargados en V2__datos_iniciales). */
public enum Rol {
    CIUDADANO,
    CHOFER,
    FUNCIONARIO,
    AUTORIDAD,
    ADMINISTRADOR;

    /** La letra pide autenticacion interna solo para el backoffice: autoridades y administradores. */
    public boolean esInterno() {
        return this == ADMINISTRADOR || this == AUTORIDAD;
    }
}
