package uy.edu.fing.tse.carga.ejb.seguridad;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.Roles;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres";

    @Test
    void emiteYValidaUnToken() {
        TokenService servicio = new TokenService(SECRETO, Duration.ofMinutes(5));
        String token = servicio.emitir("admin", Set.of(Roles.ADMINISTRADOR), TokenService.ORIGEN_INTERNO, 0, false).token();

        TokenService.DatosToken datos = servicio.validar(token).orElseThrow();
        assertEquals("admin", datos.usuario());
        assertEquals(Set.of(Roles.ADMINISTRADOR), datos.roles());
    }

    @Test
    void rechazaTokenVencido() {
        TokenService servicio = new TokenService(SECRETO, Duration.ofSeconds(-1));
        String token = servicio.emitir("admin", Set.of(Roles.ADMINISTRADOR), TokenService.ORIGEN_INTERNO, 0, false).token();
        assertTrue(servicio.validar(token).isEmpty());
    }

    @Test
    void rechazaTokenFirmadoConOtroSecreto() {
        String token = new TokenService(SECRETO, Duration.ofMinutes(5)).emitir("admin", Set.of(), TokenService.ORIGEN_INTERNO, 0, false).token();
        TokenService otro = new TokenService("otro-secreto-distinto-tambien-de-32-caracteres", Duration.ofMinutes(5));
        assertTrue(otro.validar(token).isEmpty());
    }

    @Test
    void rechazaTokenAdulterado() {
        TokenService servicio = new TokenService(SECRETO, Duration.ofMinutes(5));
        String token = servicio.emitir("autoridad", Set.of(Roles.AUTORIDAD), TokenService.ORIGEN_INTERNO, 0, false).token();
        String[] partes = token.split("\\.");
        // cambiamos el payload por uno que diga ADMINISTRADOR sin volver a firmar
        String payloadFalso = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"autoridad\",\"roles\":[\"ADMINISTRADOR\"],\"iss\":\"carga.uy\"}".getBytes());
        assertTrue(servicio.validar(partes[0] + "." + payloadFalso + "." + partes[2]).isEmpty());
    }

    @Test
    void rechazaBasura() {
        TokenService servicio = new TokenService(SECRETO, Duration.ofMinutes(5));
        assertTrue(servicio.validar("no-es-un-jwt").isEmpty());
    }

    @Test
    void firmaYLeeTransaccionDeIngreso() {
        TokenService servicio = new TokenService(SECRETO, Duration.ofMinutes(5));
        String t = servicio.firmarTransaccion(java.util.Map.of("state", "abc", "destino", "web"), Duration.ofMinutes(10));
        assertEquals("abc", servicio.leerTransaccion(t).orElseThrow().get("state"));
    }

    @Test
    void unTipoDeTokenNoSirveComoElOtro() {
        TokenService servicio = new TokenService(SECRETO, Duration.ofMinutes(5));
        String acceso = servicio.emitir("admin", Set.of(Roles.ADMINISTRADOR), TokenService.ORIGEN_INTERNO, 0, false).token();
        String transaccion = servicio.firmarTransaccion(java.util.Map.of("state", "x"), Duration.ofMinutes(10));

        assertTrue(servicio.validar(transaccion).isEmpty());
        assertTrue(servicio.leerTransaccion(acceso).isEmpty());
        assertTrue(servicio.leerTransaccion(null).isEmpty());
    }

    @Test
    void conservaOrigenVersionYContrasenaTemporal() {
        TokenService servicio = new TokenService(SECRETO, Duration.ofMinutes(5));
        String token = servicio.emitir("ministra", Set.of(Roles.AUTORIDAD), TokenService.ORIGEN_INTERNO, 7, true).token();

        TokenService.DatosToken datos = servicio.validar(token).orElseThrow();
        assertEquals(TokenService.ORIGEN_INTERNO, datos.origen());
        assertEquals(7, datos.version());
        assertTrue(datos.debeCambiarPassword());
    }

    @Test
    void sinContrasenaTemporalElClaimNoAparece() {
        TokenService servicio = new TokenService(SECRETO, Duration.ofMinutes(5));
        String token = servicio.emitir("12345672", Set.of(Roles.CIUDADANO), TokenService.ORIGEN_GUBUY, 0, false).token();
        assertEquals(false, servicio.validar(token).orElseThrow().debeCambiarPassword());
    }

    @Test
    void exigeSecretoLargo() {
        assertThrows(IllegalStateException.class, () -> new TokenService("corto", Duration.ofMinutes(5)));
        assertThrows(IllegalStateException.class, () -> new TokenService(null, Duration.ofMinutes(5)));
    }
}
