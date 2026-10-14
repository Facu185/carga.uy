package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.observabilidad.Metricas;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestaTrackingBeanTest {

    private final List<PosicionDTO> encoladas = new ArrayList<>();
    private Metricas metricas;
    private IngestaTrackingBean ingesta;

    @BeforeEach
    void preparar() {
        encoladas.clear();
        metricas = new Metricas();
        ingesta = new IngestaTrackingBean() {
            @Override
            void publicar(PosicionDTO posicion, long recibidoEnMs) {
                encoladas.add(posicion);
            }
        };
        ingesta.metricas = metricas;
    }

    @Test
    void unaPosicionValidaSeEncola() throws NegocioException {
        ingesta.encolarPosicion(posicion());

        assertEquals(1, encoladas.size());
        assertTrue(metricas.exportarPrometheus()
            .contains("carga_tracking_posiciones_total{resultado=\"aceptada\"} 1\n"));
    }

    @Test
    void unaPosicionInvalidaSeRechazaYNoLlegaALaCola() {
        PosicionDTO p = posicion();
        p.setLatitud(200);

        NegocioException e = assertThrows(NegocioException.class, () -> ingesta.encolarPosicion(p));

        assertTrue(e.getMessage().contains("latitud"));
        assertTrue(encoladas.isEmpty());
        assertTrue(metricas.exportarPrometheus()
            .contains("carga_tracking_posiciones_total{resultado=\"rechazada\"} 1\n"));
    }

    @Test
    void sinCuerpoSeRechaza() {
        assertThrows(NegocioException.class, () -> ingesta.encolarPosicion(null));
        assertTrue(encoladas.isEmpty());
    }

    private static PosicionDTO posicion() {
        PosicionDTO p = new PosicionDTO();
        p.setIdEvento("evt-1");
        p.setMatricula("STZ1234");
        p.setLatitud(-34.9);
        p.setLongitud(-56.16);
        p.setInstante(OffsetDateTime.now().toString());
        return p;
    }
}
