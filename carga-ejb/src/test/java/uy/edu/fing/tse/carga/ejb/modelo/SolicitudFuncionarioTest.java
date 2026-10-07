package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudFuncionarioTest {

    private SolicitudFuncionario solicitud() {
        return new SolicitudFuncionario(new Usuario("12345672", "Ana Perez", "ana@example.com"), "DNT");
    }

    @Test
    void nuevaQuedaPendiente() {
        assertTrue(solicitud().estaPendiente());
    }

    @Test
    void aprobarRegistraQuienYCuando() {
        SolicitudFuncionario s = solicitud();
        s.aprobar("admin");
        assertEquals(EstadoSolicitud.APROBADA, s.getEstado());
        assertEquals("admin", s.getResueltaPor());
        assertNotNull(s.getResueltaEn());
        assertFalse(s.estaPendiente());
    }

    @Test
    void rechazarGuardaElMotivo() {
        SolicitudFuncionario s = solicitud();
        s.rechazar("admin", "No pertenece al MTOP");
        assertEquals(EstadoSolicitud.RECHAZADA, s.getEstado());
        assertEquals("No pertenece al MTOP", s.getMotivoRechazo());
    }

    @Test
    void noSePuedeResolverDosVeces() {
        SolicitudFuncionario s = solicitud();
        s.aprobar("admin");
        assertThrows(IllegalStateException.class, () -> s.rechazar("otro", "x"));
    }
}
