package uy.edu.fing.tse.carga.ejb.modelo;

/** Quien valida la clave de una cuenta interna. */
public enum OrigenCuenta {
    /** Clave propia de carga.uy, guardada como hash. */
    BASE_DE_DATOS,
    /** La valida un directorio LDAP externo; aca no se guarda ninguna clave. */
    LDAP
}
