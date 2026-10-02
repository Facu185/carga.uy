package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuentaInternaTest {

    private static final Instant AHORA = Instant.parse("2026-10-01T12:00:00Z");
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private CuentaInterna cuenta() {
        return new CuentaInterna("admin", "Administrador", "hash", Rol.ADMINISTRADOR);
    }

    @Test
    void seBloqueaAlLlegarAlMaximoDeIntentos() {
        CuentaInterna c = cuenta();
        c.registrarIntentoFallido(AHORA, 3, BLOQUEO);
        c.registrarIntentoFallido(AHORA, 3, BLOQUEO);
        assertTrue(c.registrarIntentoFallido(AHORA, 3, BLOQUEO));
        assertTrue(c.estaBloqueada(AHORA.plusSeconds(60)));
    }

    @Test
    void noSeBloqueaAntesDelMaximo() {
        CuentaInterna c = cuenta();
        assertFalse(c.registrarIntentoFallido(AHORA, 3, BLOQUEO));
        assertFalse(c.estaBloqueada(AHORA));
    }

    @Test
    void elBloqueoVencidoDejaDeBloquear() {
        CuentaInterna c = cuenta();
        for (int i = 0; i < 3; i++) {
            c.registrarIntentoFallido(AHORA, 3, BLOQUEO);
        }
        assertFalse(c.estaBloqueada(AHORA.plus(BLOQUEO)));
    }

    @Test
    void cambiarClaveInvalidaSesionesYDesbloquea() {
        CuentaInterna c = cuenta();
        c.registrarIntentoFallido(AHORA, 3, BLOQUEO);
        c.cambiarPassword("otro", true);
        assertEquals(1, c.getVersionCredenciales());
        assertEquals(0, c.getIntentosFallidos());
        assertTrue(c.isDebeCambiarPassword());
    }

    @Test
    void darDeBajaYCambiarRolInvalidanSesiones() {
        CuentaInterna c = cuenta();
        c.setActivo(false);
        c.setRol(Rol.AUTORIDAD);
        assertEquals(2, c.getVersionCredenciales());
    }

    @Test
    void aceptaRolesInternos() {
        assertEquals(Rol.AUTORIDAD, new CuentaInterna("a", "A", "h", Rol.AUTORIDAD).getRol());
    }

    @Test
    void rechazaRolesNoInternos() {
        assertThrows(IllegalArgumentException.class,
                () -> new CuentaInterna("f", "F", "h", Rol.FUNCIONARIO));
        assertThrows(IllegalArgumentException.class, () -> cuenta().setRol(Rol.CIUDADANO));
    }

    @Test
    void cuentaLdapNoGuardaClaveNiLaCambiaAca() {
        CuentaInterna c = CuentaInterna.provisionarDesdeLdap("jperez", "Juan Perez", Rol.AUTORIDAD);
        assertTrue(c.esDeLdap());
        assertNull(c.getPasswordHash());
        assertThrows(IllegalStateException.class, () -> c.cambiarPassword("x", false));
    }

    @Test
    void sincronizarDesdeLdapCambiaRolEInvalidaSesiones() {
        CuentaInterna c = CuentaInterna.provisionarDesdeLdap("jperez", "Juan Perez", Rol.AUTORIDAD);
        c.sincronizarDesdeLdap("Juan P. Perez", Rol.ADMINISTRADOR);
        assertEquals(Rol.ADMINISTRADOR, c.getRol());
        assertEquals("Juan P. Perez", c.getNombre());
        assertEquals(1, c.getVersionCredenciales());
    }

    @Test
    void sincronizarDesdeLdapSinCambiosNoInvalidaSesiones() {
        CuentaInterna c = CuentaInterna.provisionarDesdeLdap("jperez", "Juan Perez", Rol.AUTORIDAD);
        c.sincronizarDesdeLdap(" ", Rol.AUTORIDAD);
        assertEquals("Juan Perez", c.getNombre());
        assertEquals(0, c.getVersionCredenciales());
    }
}
