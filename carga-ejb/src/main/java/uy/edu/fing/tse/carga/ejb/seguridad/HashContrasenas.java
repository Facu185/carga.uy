package uy.edu.fing.tse.carga.ejb.seguridad;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de contrasenas con PBKDF2-HMAC-SHA256 y salt aleatorio por usuario.
 * Formato almacenado: pbkdf2-sha256$iteraciones$salt(base64)$hash(base64).
 * Guardar las iteraciones permite subirlas en el futuro sin romper los hashes existentes.
 */
public final class HashContrasenas {

    private static final String PREFIJO = "pbkdf2-sha256";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 210_000; // recomendacion OWASP para PBKDF2-SHA256
    private static final int LARGO_SALT = 16;
    private static final int LARGO_HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private HashContrasenas() {
    }

    public static String generar(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contrase\u00f1a no puede ser vac\u00eda");
        }
        byte[] salt = new byte[LARGO_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = derivar(password.toCharArray(), salt, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIJO + "$" + ITERACIONES + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    public static boolean verificar(String password, String almacenado) {
        if (password == null || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(password.toCharArray(), salt, iteraciones);
            // comparacion en tiempo constante para no filtrar informacion por timing
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(char[] password, byte[] salt, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iteraciones, LARGO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contrase\u00f1a", e);
        } finally {
            spec.clearPassword();
        }
    }
}
