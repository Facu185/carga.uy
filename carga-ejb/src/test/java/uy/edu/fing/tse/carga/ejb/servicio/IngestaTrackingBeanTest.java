package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoLoteDTO;
import uy.edu.fing.tse.carga.api.exception.IngestaSaturadaException;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.jms.MonitorColaPosiciones;
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
    private boolean saturada;
    private IngestaTrackingBean ingesta;

    @BeforeEach
    void preparar() {
        encoladas.clear();
        metricas = new Metricas();
        saturada = false;
        ingesta = new IngestaTrackingBean() {
            @Override
            void publicar(PosicionDTO posicion, long recibidoEnMs) {
                encoladas.add(posicion);
            }
        };
        ingesta.metricas = metricas;
        ingesta.cola = new MonitorColaPosiciones() {
            @Override
            public boolean saturada() {
                return saturada;
            }

            @Override
            public int reintentarEnSegundos() {
                return 30;
            }
        };
    }

    @Test
    void unaPosicionValidaSeEncola() throws NegocioException {
        ingesta.encolarPosicion(posicion("evt-1"));

        assertEquals(1, encoladas.size());
        assertTrue(metrica("aceptada", 1));
    }

    @Test
    void unaPosicionInvalidaSeRechazaYNoLlegaALaCola() {
        PosicionDTO p = posicion("evt-1");
        p.setLatitud(200);

        NegocioException e = assertThrows(NegocioException.class, () -> ingesta.encolarPosicion(p));

        assertTrue(e.getMessage().contains("latitud"));
        assertTrue(encoladas.isEmpty());
        assertTrue(metrica("rechazada", 1));
    }

    @Test
    void sinCuerpoSeRechaza() {
        assertThrows(NegocioException.class, () -> ingesta.encolarPosicion(null));
        assertTrue(encoladas.isEmpty());
    }

    @Test
    void conLaColaSaturadaNoSeAceptaYSeIndicaCuandoReintentar() {
        saturada = true;

        IngestaSaturadaException e = assertThrows(IngestaSaturadaException.class,
            () -> ingesta.encolarPosicion(posicion("evt-1")));

        assertEquals(30, e.getReintentarEnSegundos());
        assertTrue(encoladas.isEmpty());
        assertTrue(metrica("saturada", 1));
    }

    @Test
    void unLoteValidoSeEncolaCompleto() throws NegocioException {
        ResultadoLoteDTO r = ingesta.encolarLote(List.of(posicion("a"), posicion("b"), posicion("c")));

        assertEquals(3, r.getRecibidas());
        assertEquals(3, r.getAceptadas());
        assertTrue(r.getRechazadas().isEmpty());
        assertEquals(3, encoladas.size());
        assertTrue(metrica("aceptada", 3));
    }

    @Test
    void enUnLoteSeRechazanSoloLasInvalidasYSeInformaCuales() throws NegocioException {
        PosicionDTO mala = posicion("mala");
        mala.setLongitud(-500);

        ResultadoLoteDTO r = ingesta.encolarLote(List.of(posicion("a"), mala, posicion("c")));

        assertEquals(2, r.getAceptadas());
        assertEquals(1, r.getRechazadas().size());
        assertEquals(1, r.getRechazadas().get(0).getIndice());
        assertEquals("mala", r.getRechazadas().get(0).getIdEvento());
        assertTrue(r.getRechazadas().get(0).getMotivo().contains("longitud"));
        assertEquals(List.of("a", "c"), encoladas.stream().map(PosicionDTO::getIdEvento).toList());
        assertTrue(metrica("rechazada", 1));
    }

    @Test
    void unLoteVacioOMuyGrandeSeRechazaEntero() {
        assertThrows(NegocioException.class, () -> ingesta.encolarLote(List.of()));
        assertThrows(NegocioException.class, () -> ingesta.encolarLote(null));

        List<PosicionDTO> grande = new ArrayList<>();
        for (int i = 0; i <= IngestaTrackingBean.LOTE_MAX; i++) {
            grande.add(posicion("p-" + i));
        }
        assertThrows(NegocioException.class, () -> ingesta.encolarLote(grande));
        assertTrue(encoladas.isEmpty());
    }

    @Test
    void conLaColaSaturadaNoSeAceptaNingunaDelLote() {
        saturada = true;

        assertThrows(IngestaSaturadaException.class,
            () -> ingesta.encolarLote(List.of(posicion("a"), posicion("b"))));

        assertTrue(encoladas.isEmpty());
        assertTrue(metrica("saturada", 2));
    }

    private boolean metrica(String resultado, long valor) {
        return metricas.exportarPrometheus()
            .contains("carga_tracking_posiciones_total{resultado=\"" + resultado + "\"} " + valor + "\n");
    }

    private static PosicionDTO posicion(String idEvento) {
        PosicionDTO p = new PosicionDTO();
        p.setIdEvento(idEvento);
        p.setMatricula("STZ1234");
        p.setLatitud(-34.9);
        p.setLongitud(-56.16);
        p.setInstante(OffsetDateTime.now().toString());
        return p;
    }
}
