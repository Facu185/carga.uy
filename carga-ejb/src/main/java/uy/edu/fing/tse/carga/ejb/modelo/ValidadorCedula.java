package uy.edu.fing.tse.carga.ejb.modelo;

import java.util.regex.Pattern;

/**
 * Cedula uruguaya: hasta 7 digitos de numero + 1 digito verificador.
 * Verificador = (10 - (suma ponderada con 2,9,8,7,6,3,4) mod 10) mod 10.
 */
public final class ValidadorCedula {

    private static final int[] PESOS = {2, 9, 8, 7, 6, 3, 4};
    /**
     * Formatos aceptados: 12345672, 1234567-2, 1.234.567-2 (y los de 7 digitos: 123456-1, 123.456-1).
     * Los puntos solo pueden separar miles: "60000008." o "1.2.3.4" se rechazan.
     */
    private static final Pattern FORMATO = Pattern.compile("(\\d{6,7}|\\d{1,3}(\\.\\d{3}){1,2})-?\\d");

    private ValidadorCedula() {
    }

    /** Devuelve solo los digitos si el formato es valido; null si no lo es. */
    public static String normalizar(String cedula) {
        if (cedula == null) {
            return null;
        }
        String recortada = cedula.trim();
        if (!FORMATO.matcher(recortada).matches()) {
            return null;
        }
        String limpia = recortada.replaceAll("[.\\-]", "");
        return limpia.matches("\\d{7,8}") ? limpia : null;
    }

    public static boolean esValida(String cedula) {
        String normalizada = normalizar(cedula);
        if (normalizada == null) {
            return false;
        }
        String numero = String.format("%7s", normalizada.substring(0, normalizada.length() - 1)).replace(' ', '0');
        int verificador = normalizada.charAt(normalizada.length() - 1) - '0';
        int suma = 0;
        for (int i = 0; i < PESOS.length; i++) {
            suma += (numero.charAt(i) - '0') * PESOS[i];
        }
        return (10 - suma % 10) % 10 == verificador;
    }
}
