package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.SincronizacionPesajesLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.PesajesDeGuiaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoSincronizacionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.persistencia.PesadaDAO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgendaPesajesBeanTest {

    @Test
    void sincronizaCadaVehiculoConGuiaYUnoConProblemasNoFrenaALosDemas() {
        List<String> sincronizadas = new ArrayList<>();
        AgendaPesajesBean agenda = new AgendaPesajesBean();
        agenda.pesadas = new PesadaDAO() {
            @Override
            public List<String> matriculasConGuiaEntre(Instant desde, Instant hasta) {
                return List.of("STZ1234", "FALLA01", "CUY9012");
            }
        };
        agenda.sincronizacion = new SincronizacionPesajesLocal() {
            @Override
            public ResultadoSincronizacionDTO sincronizar(String matricula, String fecha) {
                if (matricula.equals("FALLA01")) {
                    throw new IllegalStateException("se corto la base");
                }
                sincronizadas.add(matricula + "@" + fecha);
                return ResultadoSincronizacionDTO.completada(0, 0, 0, 0, 0);
            }

            @Override
            public List<PesadaRegistradaDTO> registradas(String matricula, String fecha) throws NegocioException {
                throw new UnsupportedOperationException();
            }

            @Override
            public PesajesDeGuiaDTO registradasDeGuia(Long guiaId) {
                throw new UnsupportedOperationException();
            }
        };

        // 02:30 UTC del 8 de octubre todavia es 7 de octubre en Uruguay (UTC-3)
        int ok = agenda.sincronizarVehiculosConGuia(Instant.parse("2026-10-08T02:30:00Z"));

        assertEquals(2, ok);
        assertEquals(List.of("STZ1234@2026-10-07", "CUY9012@2026-10-07"), sincronizadas);
    }
}
