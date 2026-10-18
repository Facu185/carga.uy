package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.PosicionTracking;
import uy.edu.fing.tse.carga.ejb.persistencia.RecorridoDAO;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecorridoBeanTest {

    private final List<PosicionTracking> posiciones = new ArrayList<>();
    private String matriculaPedida;
    private OffsetDateTime desdePedido;
    private OffsetDateTime hastaPedido;
    private RecorridoBean servicio;

    @BeforeEach
    void preparar() {
        servicio = new RecorridoBean();
        servicio.dao = new RecorridoDAO() {
            @Override
            public List<PosicionTracking> posiciones(String matricula, OffsetDateTime desde, OffsetDateTime hasta, int maximo) {
                matriculaPedida = matricula;
                desdePedido = desde;
                hastaPedido = hasta;
                return posiciones.subList(0, Math.min(maximo, posiciones.size()));
            }

            @Override
            public double distanciaMetros(String matricula, OffsetDateTime desde, OffsetDateTime hasta) {
                return 1234.5;
            }

            @Override
            public Optional<PeriodoDeGuia> periodoDeGuia(Long guiaId) {
                return guiaId == 77L
                        ? Optional.of(new PeriodoDeGuia("STZ1234", Instant.parse("2026-10-18T11:00:00Z"), Instant.parse("2026-10-18T19:00:00Z")))
                        : Optional.empty();
            }
        };
    }

    @Test
    void devuelveLosPuntosYLaDistanciaDelPeriodo() throws NegocioException {
        agregarPosiciones(3);
        RecorridoVehiculoDTO r = servicio.recorrido(" stz1234 ", "2026-10-18T08:00:00-03:00", "2026-10-18T20:00:00-03:00");
        assertEquals("STZ1234", matriculaPedida);
        assertEquals(3, r.getCantidad());
        assertFalse(r.isTruncado());
        assertEquals(1234.5, r.getDistanciaMetros());
    }

    @Test
    void conMasPuntosQueElMaximoAvisaQueEstaTruncado() throws NegocioException {
        agregarPosiciones(RecorridoBean.PUNTOS_MAXIMOS + 5);
        RecorridoVehiculoDTO r = servicio.recorrido("STZ1234", "2026-10-18T00:00:00Z", "2026-10-19T00:00:00Z");
        assertTrue(r.isTruncado());
        assertEquals(RecorridoBean.PUNTOS_MAXIMOS, r.getCantidad());
    }

    @Test
    void sinPosicionesNoCalculaDistancia() throws NegocioException {
        RecorridoVehiculoDTO r = servicio.recorrido("STZ1234", "2026-10-18T00:00:00Z", "2026-10-19T00:00:00Z");
        assertEquals(0, r.getCantidad());
        assertEquals(0, r.getDistanciaMetros());
    }

    @Test
    void validaElPeriodo() {
        assertThrows(NegocioException.class, () -> servicio.recorrido(" ", "2026-10-18T00:00:00Z", "2026-10-19T00:00:00Z"));
        assertThrows(NegocioException.class, () -> servicio.recorrido("STZ1234", "18/10/2026", "2026-10-19T00:00:00Z"));
        assertThrows(NegocioException.class, () -> servicio.recorrido("STZ1234", "2026-10-18T00:00:00", "2026-10-19T00:00:00Z"));
        assertThrows(NegocioException.class, () -> servicio.recorrido("STZ1234", "2026-10-19T00:00:00Z", "2026-10-18T00:00:00Z"));
        assertThrows(NegocioException.class, () -> servicio.recorrido("STZ1234", "2026-10-01T00:00:00Z", "2026-10-09T00:00:00Z"));
    }

    @Test
    void elRecorridoDeUnaGuiaUsaSuVehiculoYSuPeriodo() throws NegocioException {
        agregarPosiciones(2);
        RecorridoVehiculoDTO r = servicio.recorridoDeGuia(77L);
        assertEquals(77L, r.getGuiaId());
        assertEquals("STZ1234", matriculaPedida);
        assertEquals(Instant.parse("2026-10-18T11:00:00Z"), desdePedido.toInstant());
        assertEquals(Instant.parse("2026-10-18T19:00:00Z"), hastaPedido.toInstant());
        assertThrows(NegocioException.class, () -> servicio.recorridoDeGuia(999L));
    }

    private void agregarPosiciones(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            PosicionDTO dto = new PosicionDTO();
            dto.setIdEvento("e" + i);
            dto.setMatricula("STZ1234");
            dto.setLatitud(-34.9 + i * 0.001);
            dto.setLongitud(-56.16);
            dto.setInstante(OffsetDateTime.parse("2026-10-18T09:00:00-03:00").plusSeconds(30L * i).toString());
            posiciones.add(PosicionTracking.desde(dto, OffsetDateTime.parse("2026-10-18T12:00:00Z")));
        }
    }
}
