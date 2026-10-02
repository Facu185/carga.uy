package uy.edu.fing.tse.carga.ejb.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashContrasenasTest {

    @Test
    void verificaLaContrasenaCorrecta() {
        String hash = HashContrasenas.generar("una-clave-segura");
        assertTrue(HashContrasenas.verificar("una-clave-segura", hash));
    }

    @Test
    void rechazaContrasenaIncorrecta() {
        String hash = HashContrasenas.generar("una-clave-segura");
        assertFalse(HashContrasenas.verificar("otra-clave", hash));
    }

    @Test
    void mismaContrasenaGeneraHashesDistintosPorElSalt() {
        assertNotEquals(HashContrasenas.generar("repetida"), HashContrasenas.generar("repetida"));
    }

    @Test
    void rechazaFormatosInvalidos() {
        assertFalse(HashContrasenas.verificar("x", null));
        assertFalse(HashContrasenas.verificar(null, "pbkdf2-sha256$1$AA==$AA=="));
        assertFalse(HashContrasenas.verificar("x", "texto-plano"));
        assertFalse(HashContrasenas.verificar("x", "pbkdf2-sha256$no-numero$AA==$AA=="));
    }

    @Test
    void noPermiteContrasenaVacia() {
        assertThrows(IllegalArgumentException.class, () -> HashContrasenas.generar(""));
    }
}
