package uy.edu.fing.tse.carga.ejb.jms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.SincronizacionPesajesLocal;
import uy.edu.fing.tse.carga.api.dto.comun.ViajeFinalizadoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.PesajesDeGuiaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoSincronizacionDTO;
import uy.edu.fing.tse.carga.ejb.observabilidad.Metricas;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinDeViajeMDBTest {

    private final List<String> sincronizados = new ArrayList<>();
    private RuntimeException falla;
    private boolean deshecho;
    private FinDeViajeMDB mdb;

    @BeforeEach
    void preparar() {
        mdb = new FinDeViajeMDB() {
            @Override
            void deshacer() {
                deshecho = true;
            }
        };
        mdb.metricas = new Metricas();
        mdb.sincronizacion = new SincronizacionPesajesLocal() {
            @Override
            public ResultadoSincronizacionDTO sincronizar(String matricula, String fecha) {
                if (falla != null) {
                    throw falla;
                }
                sincronizados.add(matricula + "@" + fecha);
                return ResultadoSincronizacionDTO.completada(0, 0, 0, 0, 0);
            }

            @Override
            public List<PesadaRegistradaDTO> registradas(String matricula, String fecha) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PesajesDeGuiaDTO registradasDeGuia(Long guiaId) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    void sincronizaCadaDiaDeUruguayQueTocaElViaje() {
        // 22:00 del 7 al 03:00 del 8 en Uruguay: dos dias, aunque en UTC ya sean el 8 y el 8
        mdb.procesar(new ViajeFinalizadoDTO(5L, "STZ1234", "2026-10-07T22:00:00-03:00", "2026-10-08T03:00:00-03:00"));
        assertEquals(List.of("STZ1234@2026-10-07", "STZ1234@2026-10-08"), sincronizados);
    }

    @Test
    void sinInicioSoloSincronizaElDiaDelFin() {
        mdb.procesar(new ViajeFinalizadoDTO(5L, "STZ1234", null, "2026-10-08T03:00:00Z"));
        assertEquals(List.of("STZ1234@2026-10-08"), sincronizados);
    }

    @Test
    void unAvisoMalFormadoSeDescartaSinReintentar() {
        mdb.procesar(new ViajeFinalizadoDTO(5L, "STZ1234", null, "ayer"));
        mdb.procesar(new ViajeFinalizadoDTO(5L, " ", null, "2026-10-08T03:00:00Z"));
        mdb.procesar(new ViajeFinalizadoDTO(5L, "STZ1234", "2026-09-01T00:00:00Z", "2026-10-08T03:00:00Z"));
        assertTrue(sincronizados.isEmpty());
        assertFalse(deshecho);
    }

    @Test
    void unaFallaTecnicaSeDeshaceParaQueElBrokerLoReentregue() {
        falla = new IllegalStateException("se corto la base");
        mdb.procesar(new ViajeFinalizadoDTO(5L, "STZ1234", null, "2026-10-08T03:00:00Z"));
        assertTrue(deshecho);
    }
}
