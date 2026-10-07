package uy.edu.fing.tse.carga.ejb.seguridad;

import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.security.SecureRandom;

/** Reglas de contrasenas de los usuarios internos, en un solo lugar. */
public final class PoliticaContrasenas {

    public static final int LARGO_MINIMO = 12;
    /** Sin caracteres que se confunden al dictarlos o leerlos (0/O, 1/l/I). */
    private static final String ALFABETO_TEMPORAL = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PoliticaContrasenas() {
    }

    public static void validar(String password) throws NegocioException {
        if (password == null || password.length() < LARGO_MINIMO) {
            throw new NegocioException("La contrase\u00f1a debe tener al menos " + LARGO_MINIMO + " caracteres");
        }
        if (password.isBlank()) {
            throw new NegocioException("La contrase\u00f1a no puede ser solo espacios");
        }
    }

    /** Contrasena temporal facil de dictar: xxxx-xxxx-xxxx (12 caracteres aleatorios, unos 70 bits). */
    public static String generarTemporal() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(ALFABETO_TEMPORAL.charAt(RANDOM.nextInt(ALFABETO_TEMPORAL.length())));
        }
        return sb.toString();
    }
}
