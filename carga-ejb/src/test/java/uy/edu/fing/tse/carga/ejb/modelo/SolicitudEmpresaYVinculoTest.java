package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudEmpresaYVinculoTest {

    private final Usuario persona = new Usuario("12345672", "Ana Perez", "ana@example.com");
    private final Empresa empresa = Empresa.desdeDTO(new uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO(
            "Transporte del Sur", "Transporte del Sur SA", 12345L, java.time.LocalDate.now()));

    @Test
    void laSolicitudDeAltaSoloPuedeResolverseUnaVez() {
        SolicitudAltaEmpresa solicitud = new SolicitudAltaEmpresa(persona, "Transporte del Sur",
                "Transporte del Sur SA", 12345L);

        assertEquals(EstadoSolicitudEmpresa.PENDIENTE, solicitud.getEstado());
        solicitud.aprobar("funcionario");

        assertEquals(EstadoSolicitudEmpresa.APROBADA, solicitud.getEstado());
        assertEquals("funcionario", solicitud.getResueltaPor());
        assertThrows(IllegalStateException.class, () -> solicitud.rechazar("funcionario", "Duplicada"));
    }

    @Test
    void elVinculoSoloAutorizaMientrasEstaActivo() {
        VinculoEmpresaUsuario vinculo = new VinculoEmpresaUsuario(persona, empresa, TipoVinculoEmpresa.RESPONSABLE);

        assertFalse(vinculo.estaActivo());
        vinculo.aprobar("funcionario");
        assertTrue(vinculo.estaActivo());
        vinculo.revocar("funcionario", "Baja de empresa");
        assertFalse(vinculo.estaActivo());
        assertEquals(EstadoVinculoEmpresa.REVOCADO, vinculo.getEstado());
        assertThrows(IllegalStateException.class, () -> vinculo.aprobar("funcionario"));
    }

    @Test
    void elDtoDeVinculoConservaLaIdentidadDelSolicitanteParaSuRevision() {
        var dto = new uy.edu.fing.tse.carga.api.dto.empresa.VinculoEmpresaDTO(
                1L, 2L, "Transporte del Sur", "RESPONSABLE", "PENDIENTE", "2026-10-12T12:00:00Z",
                null, null, null, null, null, "12345672", "Ana Perez");

        assertEquals("12345672", dto.getCedulaSolicitante());
        assertEquals("Ana Perez", dto.getNombreSolicitante());
    }
}
