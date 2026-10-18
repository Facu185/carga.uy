package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.ViajeFinalizadoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvisoFinDeViajeBeanTest {

    private final List<ViajeFinalizadoDTO> publicados = new ArrayList<>();
    private AvisoFinDeViajeBean aviso;

    @BeforeEach
    void preparar() {
        aviso = new AvisoFinDeViajeBean() {
            @Override
            void publicar(ViajeFinalizadoDTO viaje) {
                publicados.add(viaje);
            }
        };
    }

    @Test
    void encolaElAvisoConLosDatosDelViaje() throws NegocioException {
        aviso.avisar(new ViajeFinalizadoDTO(5L, "STZ1234", "2026-10-07T22:00:00-03:00", "2026-10-08T03:00:00-03:00"));
        assertEquals(1, publicados.size());
        assertEquals("STZ1234", publicados.get(0).getMatricula());
        assertEquals(5L, publicados.get(0).getGuiaId());
    }

    @Test
    void rechazaAvisosIncompletosAntesDeEncolar() {
        assertThrows(NegocioException.class, () -> aviso.avisar(null));
        assertThrows(NegocioException.class, () -> aviso.avisar(new ViajeFinalizadoDTO(5L, " ", null, "2026-10-08T03:00:00Z")));
        assertThrows(NegocioException.class, () -> aviso.avisar(new ViajeFinalizadoDTO(5L, "STZ1234", null, null)));
        assertThrows(NegocioException.class, () -> aviso.avisar(new ViajeFinalizadoDTO(5L, "STZ1234", null, "ayer")));
        assertThrows(NegocioException.class,
                () -> aviso.avisar(new ViajeFinalizadoDTO(5L, "STZ1234", "2026-10-09T00:00:00Z", "2026-10-08T00:00:00Z")));
        assertTrue(publicados.isEmpty());
    }
}
