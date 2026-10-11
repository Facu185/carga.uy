package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoPesajeDTO;
import uy.edu.fing.tse.carga.ejb.integracion.CircuitBreaker;
import uy.edu.fing.tse.carga.ejb.observabilidad.Metricas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PesajeBeanTest {

    /** Que hace el nodo simulado en cada llamada. */
    private enum Nodo { RESPONDE, CAIDO, RECHAZA, NO_AUTORIZA }

    private Nodo nodo;
    private int llamadas;
    private PesajeBean pesaje;
    private Metricas metricas;
    private RelojManual reloj;

    @BeforeEach
    void preparar() {
        nodo = Nodo.RESPONDE;
        pesaje = new PesajeBean() {
            @Override
            List<PesadaDTO> invocar(String matricula, String fecha) throws Exception {
                llamadas++;
                return switch (nodo) {
                    case RESPONDE -> List.of(pesada("STZ1234-1"));
                    case CAIDO -> throw new TimeoutException("el nodo no contesto");
                    case RECHAZA -> throw new PedidoRechazadoException(400);
                    case NO_AUTORIZA -> throw new PedidoRechazadoException(401);
                };
            }
        };
        metricas = new Metricas();
        reloj = new RelojManual();
        pesaje.metricas = metricas;
        pesaje.configurar("http://balanzas/api/pesadas", Duration.ofMillis(100), 1,
                new CircuitBreaker("Balanzas", 2, Duration.ofSeconds(30), reloj));
    }

    @Test
    void devuelveLasPesadasYAvisaQueElNodoRespondio() {
        ResultadoPesajeDTO r = pesaje.consultarPesadas("STZ1234", "2026-10-07");
        assertTrue(r.isNodoDisponible());
        assertEquals(1, r.getPesadas().size());
        assertEquals(1, llamadas);
    }

    @Test
    void anteUnaFallaTecnicaReintentaUnaVezYAvisaQueElNodoNoRespondio() {
        nodo = Nodo.CAIDO;
        ResultadoPesajeDTO r = pesaje.consultarPesadas("STZ1234", "2026-10-07");
        assertFalse(r.isNodoDisponible());
        assertTrue(r.getPesadas().isEmpty());
        assertEquals(2, llamadas); // el intento y un reintento
    }

    @Test
    void unRechazoDelNodoNoSeReintentaNiAbreElCircuito() {
        nodo = Nodo.RECHAZA;
        for (int i = 0; i < 5; i++) {
            assertFalse(pesaje.consultarPesadas("STZ1234", "2026-10-07").isNodoDisponible());
        }
        assertEquals(5, llamadas); // una por consulta, sin reintentos
        assertEquals(CircuitBreaker.Estado.CERRADO, pesaje.estadoCircuito());
    }

    @Test
    void siElNodoNoAceptaLasCredencialesLoDiceSinReintentarNiAbrirElCircuito() {
        nodo = Nodo.NO_AUTORIZA;
        ResultadoPesajeDTO r = pesaje.consultarPesadas("STZ1234", "2026-10-07");
        assertFalse(r.isNodoDisponible());
        assertTrue(r.getMensaje().contains("NODO_BALANZAS_TOKEN"));
        assertEquals(1, llamadas);
        assertEquals(CircuitBreaker.Estado.CERRADO, pesaje.estadoCircuito());
    }

    @Test
    void conFallasSostenidasElCircuitoSeAbreYDejaDeLlamarAlNodo() {
        nodo = Nodo.CAIDO;
        pesaje.consultarPesadas("STZ1234", "2026-10-07");
        pesaje.consultarPesadas("STZ1234", "2026-10-07");
        assertEquals(CircuitBreaker.Estado.ABIERTO, pesaje.estadoCircuito());

        int antes = llamadas;
        ResultadoPesajeDTO r = pesaje.consultarPesadas("STZ1234", "2026-10-07");
        assertFalse(r.isNodoDisponible());
        assertEquals(antes, llamadas); // responde al instante, sin tocar el nodo

        String exportado = metricas.exportarPrometheus();
        assertTrue(exportado.contains("carga_balanzas_consultas_total{resultado=\"falla\"} 2"));
        assertTrue(exportado.contains("carga_balanzas_consultas_total{resultado=\"circuito_abierto\"} 1"));
    }

    @Test
    void cuandoElNodoVuelveElCircuitoSeCierraSolo() {
        nodo = Nodo.CAIDO;
        pesaje.consultarPesadas("STZ1234", "2026-10-07");
        pesaje.consultarPesadas("STZ1234", "2026-10-07");
        assertEquals(CircuitBreaker.Estado.ABIERTO, pesaje.estadoCircuito());

        nodo = Nodo.RESPONDE;
        reloj.avanzar(Duration.ofSeconds(31)); // pasa el tiempo de espera: una llamada de prueba
        assertTrue(pesaje.consultarPesadas("STZ1234", "2026-10-07").isNodoDisponible());
        assertEquals(CircuitBreaker.Estado.CERRADO, pesaje.estadoCircuito());
    }

    @Test
    void obtenerPesadasDevuelveListaVaciaConElNodoCaido() {
        nodo = Nodo.CAIDO;
        assertTrue(pesaje.obtenerPesadas("STZ1234", "2026-10-07").isEmpty());
    }

    static PesadaDTO pesada(String id) {
        PesadaDTO p = new PesadaDTO();
        p.setIdPesada(id);
        p.setMatricula("STZ1234");
        p.setPeaje("Peaje Pando");
        p.setPesoKg(28000);
        p.setInstante("2026-10-07T10:15-03:00");
        return p;
    }

    /** Reloj que avanza solo cuando la prueba lo pide. */
    static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-10-07T12:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return this;
        }
    }
}
