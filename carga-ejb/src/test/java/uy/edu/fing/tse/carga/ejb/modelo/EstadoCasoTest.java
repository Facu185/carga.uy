package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El ciclo de vida del caso (letra, punto b): solo las transiciones previstas, ninguna otra. */
class EstadoCasoTest {

    private static final Map<EstadoCaso, Set<EstadoCaso>> PREVISTAS = Map.of(
            EstadoCaso.DETECTADA, EnumSet.of(EstadoCaso.NOTIFICADA),
            EstadoCaso.NOTIFICADA, EnumSet.of(EstadoCaso.EN_DESCARGO, EstadoCaso.CONFIRMADA, EstadoCaso.DESESTIMADA),
            EstadoCaso.EN_DESCARGO, EnumSet.of(EstadoCaso.CONFIRMADA, EstadoCaso.DESESTIMADA),
            EstadoCaso.CONFIRMADA, EnumSet.noneOf(EstadoCaso.class),
            EstadoCaso.DESESTIMADA, EnumSet.noneOf(EstadoCaso.class));

    @Test
    void todasLasTransicionesNoPrevistasSeRechazan() {
        for (EstadoCaso origen : EstadoCaso.values()) {
            for (EstadoCaso destino : EstadoCaso.values()) {
                assertEquals(PREVISTAS.get(origen).contains(destino), origen.puedePasarA(destino),
                        origen + " -> " + destino);
            }
        }
    }

    @Test
    void unCasoNoPuedeSaltearseLaNotificacionNiReabrirse() {
        CasoFiscalizacion detectado = new CasoFiscalizacion(1L, TipoInfraccion.EXCESO_CARGA, EstadoCaso.DETECTADA, null, "STZ1234");
        assertThrows(IllegalStateException.class, () -> detectado.pasarA(EstadoCaso.CONFIRMADA));
        assertThrows(IllegalStateException.class, () -> detectado.pasarA(EstadoCaso.EN_DESCARGO));

        CasoFiscalizacion confirmado = new CasoFiscalizacion(2L, TipoInfraccion.EXCESO_CARGA, EstadoCaso.CONFIRMADA, null, "STZ1234");
        assertThrows(IllegalStateException.class, () -> confirmado.pasarA(EstadoCaso.DESESTIMADA));
        assertThrows(IllegalStateException.class, () -> confirmado.pasarA(EstadoCaso.NOTIFICADA));
    }

    @Test
    void soloConfirmadaYDesestimadaSonFinales() {
        for (EstadoCaso e : EstadoCaso.values()) {
            assertEquals(e == EstadoCaso.CONFIRMADA || e == EstadoCaso.DESESTIMADA, e.resuelto());
        }
        assertTrue(EstadoCaso.CONFIRMADA.siguientes().isEmpty());
        assertFalse(EstadoCaso.NOTIFICADA.siguientes().isEmpty());
    }
}
