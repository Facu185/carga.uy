package uy.edu.fing.tse.carga.ejb.jms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.PosicionTracking;
import uy.edu.fing.tse.carga.ejb.observabilidad.Metricas;
import uy.edu.fing.tse.carga.ejb.persistencia.PosicionDAO;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PosicionMDBTest {

    private PosicionesEnMemoria posiciones;
    private Metricas metricas;
    private int deshechas;
    private PosicionMDB mdb;

    @BeforeEach
    void preparar() {
        posiciones = new PosicionesEnMemoria();
        metricas = new Metricas();
        deshechas = 0;
        mdb = new PosicionMDB() {
            @Override
            void deshacer() {
                deshechas++;
            }
        };
        mdb.posiciones = posiciones;
        mdb.metricas = metricas;
    }

    @Test
    void guardaUnaPosicionNueva() {
        mdb.procesar(posicion("evt-1", "2026-10-14T10:00:00-03:00"), null);

        assertEquals(1, posiciones.guardadas.size());
        assertTrue(metrica("nueva", 1));
    }

    @Test
    void elMismoEventoDosVecesQuedaUnaSolaVez() {
        mdb.procesar(posicion("evt-1", "2026-10-14T10:00:00-03:00"), null);
        mdb.procesar(posicion("evt-1", "2026-10-14T10:00:00-03:00"), null);

        assertEquals(1, posiciones.guardadas.size());
        assertTrue(metrica("nueva", 1));
        assertTrue(metrica("duplicada", 1));
        assertEquals(0, deshechas);
    }

    @Test
    void guardaElInstanteDelOrigenAunqueLleguenFueraDeOrden() {
        mdb.procesar(posicion("evt-2", "2026-10-14T10:05:00-03:00"), null);
        mdb.procesar(posicion("evt-1", "2026-10-14T10:00:00-03:00"), null);

        assertEquals(OffsetDateTime.parse("2026-10-14T10:00:00-03:00"),
            posiciones.guardadas.get("evt-1").getInstante());
        assertEquals(2, posiciones.guardadas.size());
    }

    @Test
    void unaPosicionInvalidaSeDescartaSinReintentar() {
        mdb.procesar(posicion("evt-1", "esto no es una fecha"), null);

        assertTrue(posiciones.guardadas.isEmpty());
        assertTrue(metrica("descartada", 1));
        assertEquals(0, deshechas);
    }

    @Test
    void siLaBaseFallaSeDeshaceParaQueElBrokerReintente() {
        posiciones.fallar = true;

        mdb.procesar(posicion("evt-1", "2026-10-14T10:00:00-03:00"), null);

        assertEquals(1, deshechas);
        assertTrue(metrica("error", 1));
        assertFalse(metrica("nueva", 1));
    }

    @Test
    void laReentregaDespuesDeUnaFallaGuardaLaPosicion() {
        posiciones.fallar = true;
        mdb.procesar(posicion("evt-1", "2026-10-14T10:00:00-03:00"), null);
        posiciones.fallar = false;

        mdb.procesar(posicion("evt-1", "2026-10-14T10:00:00-03:00"), null);

        assertEquals(1, posiciones.guardadas.size());
        assertTrue(metrica("nueva", 1));
    }

    @Test
    void mideElTiempoDesdeLaRecepcion() {
        mdb.procesar(posicion("evt-1", "2026-10-14T10:00:00-03:00"), System.currentTimeMillis() - 40);

        assertTrue(metricas.exportarPrometheus().contains("carga_tracking_procesamiento_ms_count 1\n"));
    }

    private boolean metrica(String resultado, long valor) {
        return metricas.exportarPrometheus()
            .contains("carga_tracking_posiciones_total{resultado=\"" + resultado + "\"} " + valor + "\n");
    }

    static PosicionDTO posicion(String idEvento, String instante) {
        PosicionDTO p = new PosicionDTO();
        p.setIdEvento(idEvento);
        p.setMatricula("STZ1234");
        p.setLatitud(-34.9);
        p.setLongitud(-56.16);
        p.setInstante(instante);
        return p;
    }

    static final class PosicionesEnMemoria extends PosicionDAO {
        final Map<String, PosicionTracking> guardadas = new LinkedHashMap<>();
        boolean fallar;

        @Override
        public boolean guardarSiEsNueva(PosicionTracking p) {
            if (fallar) {
                throw new IllegalStateException("la base no responde (simulado)");
            }
            return guardadas.putIfAbsent(p.getIdEvento(), p) == null;
        }
    }
}
