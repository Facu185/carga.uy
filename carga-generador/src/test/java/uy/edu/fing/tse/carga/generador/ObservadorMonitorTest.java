package uy.edu.fing.tse.carga.generador;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservadorMonitorTest {
    @Test
    void sinContrasenaNoSeSimulaElFuncionario() {
        assertFalse(Configuracion.desdeEntorno(Map.of()).monitorActivo());
        assertTrue(Configuracion.desdeEntorno(Map.of("GEN_MONITOR_PASSWORD", "clave")).monitorActivo());
    }

    @Test
    void laDireccionDeLaApiSaleDeLaDeIngesta() {
        assertEquals("http://central:8080/carga/api", Configuracion.desdeEntorno(Map.of()).urlApi());
        assertEquals("http://otro:9090/carga/api", Configuracion.desdeEntorno(
            Map.of("CENTRAL_INGESTA_URL", "http://otro:9090/carga/api/tracking/posiciones")).urlApi());
    }

    @Test
    void elLoginEscapaLasComillasDeLaContrasena() {
        assertEquals("{\"usuario\":\"admin\",\"password\":\"a\\\"b\\\\c\"}",
            ObservadorMonitor.cuerpoLogin("admin", "a\"b\\c"));
    }

    @Test
    void leeElTokenDeLaRespuestaDelLogin() {
        String sesion = "{\"debeCambiarPassword\":false,\"expiraEn\":\"2026-10-27T20:00:00Z\","
            + "\"token\":\"eyJabc.def.ghi\",\"usuario\":\"admin\"}";
        assertEquals("eyJabc.def.ghi", ObservadorMonitor.tokenDe(sesion));
        assertFalse(ObservadorMonitor.debeCambiarPassword(sesion));
        assertNull(ObservadorMonitor.tokenDe("{\"error\":\"credenciales\"}"));
    }

    @Test
    void detectaUnaClaveTemporal() {
        assertTrue(ObservadorMonitor.debeCambiarPassword("{\"debeCambiarPassword\":true,\"token\":\"x\"}"));
    }

    @Test
    void cadaIntervaloSoloMuestraLoNuevoPeroElTotalAcumula() {
        ObservadorMonitor.Serie serie = new ObservadorMonitor.Serie();
        serie.registrar(30);
        serie.registrar(10);
        assertArrayEquals(new long[]{10, 30}, serie.tomarIntervalo());

        serie.registrar(20);
        assertArrayEquals(new long[]{20}, serie.tomarIntervalo());
        assertArrayEquals(new long[]{10, 20, 30}, serie.total());
    }
}
