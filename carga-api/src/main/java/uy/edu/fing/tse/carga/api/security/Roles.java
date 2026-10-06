package uy.edu.fing.tse.carga.api.security;

/**
 * Nombres de los roles (los mismos de la tabla rol) como constantes, para usarlos en
 * {@code @RolesAllowed}: las anotaciones necesitan constantes, no valores de un enum.
 */
public final class Roles {

    public static final String CIUDADANO = "CIUDADANO";
    public static final String CHOFER = "CHOFER";
    public static final String FUNCIONARIO = "FUNCIONARIO";
    public static final String AUTORIDAD = "AUTORIDAD";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    private Roles() {
    }
}
