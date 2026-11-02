package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoDTO;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitorBeanMapaTest {

    @Test
    void agrupaPorVehiculoYLaUltimaFilaEsLaPosicionActual() {
        List<RecorridoDTO> flota = MonitorBean.armarMapa(List.of(
                fila("ABC1234", -34.90, -56.10, OffsetDateTime.parse("2026-11-02T10:00:00Z")),
                fila("ABC1234", -34.80, -56.00, OffsetDateTime.parse("2026-11-02T10:00:30Z")),
                fila("XYZ9876", -33.00, -55.00, Timestamp.from(Instant.parse("2026-11-02T10:01:00Z")))));

        assertEquals(2, flota.size());
        RecorridoDTO abc = flota.get(0);
        assertEquals("ABC1234", abc.getMatricula());
        assertEquals(2, abc.getPuntos().size());
        assertArrayEquals(new double[]{-34.90, -56.10}, abc.getPuntos().get(0));
        assertEquals(-34.80, abc.getLatitud());
        assertEquals(-56.00, abc.getLongitud());
        assertEquals("XYZ9876", flota.get(1).getMatricula());
        assertEquals(1, flota.get(1).getPuntos().size());
    }

    @Test
    void sinPosicionesRecientesElMapaQuedaVacio() {
        assertTrue(MonitorBean.armarMapa(List.of()).isEmpty());
    }

    private static Object[] fila(String matricula, double latitud, double longitud, Object instante) {
        return new Object[]{matricula, latitud, longitud, instante};
    }
}
