package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJBAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CasoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ReporteFiscalizacionDTO;
import uy.edu.fing.tse.carga.api.exception.ConflictoException;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.RecursoAjenoException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.CasoFiscalizacion;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoCaso;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ciclo completo del caso desde los servicios: descargo de la empresa y resolucion del funcionario. */
class GestionCasosTest {

    private static final String RESPONSABLE = "12345672";
    private static final Instant EN_UNA_SEMANA = Instant.now().plusSeconds(7 * 86_400);
    private static final Instant AYER = Instant.now().minusSeconds(86_400);

    private CasosEnMemoria casos;
    private Falsos.AuditoriaFalsa auditoria;
    private FiscalizacionBean fiscalizacion;
    private InfraccionesEmpresaBean infracciones;

    @BeforeEach
    void preparar() {
        casos = new CasosEnMemoria();
        auditoria = new Falsos.AuditoriaFalsa();

        fiscalizacion = new FiscalizacionBean();
        fiscalizacion.dao = casos;
        fiscalizacion.auditoria = auditoria;
        fiscalizacion.contexto = Falsos.sesion("funcionario1", Roles.FUNCIONARIO);

        infracciones = new InfraccionesEmpresaBean();
        infracciones.dao = casos;
        infracciones.auditoria = auditoria;
        infracciones.contexto = Falsos.sesion(RESPONSABLE, Roles.CIUDADANO);
        infracciones.compatibilidad = new Falsos.CompatibilidadFalsa();
        infracciones.usuarios = new Falsos.UsuariosEnMemoria();
        infracciones.vinculos = new VinculoEmpresaUsuarioDAO() {
            @Override
            public boolean esResponsableActivo(String cedula, Long empresaId) {
                return RESPONSABLE.equals(cedula) && empresaId == 1L;
            }
        };
    }

    @Test
    void laEmpresaPresentaSuDescargoYElFuncionarioLoResuelve() throws Exception {
        CasoFiscalizacion c = casos.notificado(1, EN_UNA_SEMANA);

        CasoDTO conDescargo = infracciones.presentarDescargo(1L, c.getId(), "La balanza del peaje estaba descalibrada ese día.");
        assertEquals("EN_DESCARGO", conDescargo.getEstado());
        assertFalse(conDescargo.isAdmiteDescargo());

        CasoDTO resuelto = fiscalizacion.resolver(c.getId(), false, "Se verificó la calibración: la pesada no es válida.");
        assertEquals("DESESTIMADA", resuelto.getEstado());
        assertEquals("funcionario1", resuelto.getResueltaPor());
        assertEquals(List.of(AccionAuditada.CASO_CAMBIO_ESTADO, AccionAuditada.CASO_RESUELTO), auditoria.acciones());
        assertEquals("Se verificó la calibración: la pesada no es válida.", auditoria.ultimo().fundamento());
    }

    @Test
    void noSeConfirmaMientrasCorreElPlazoDeDescargoPeroSiSePuedeDesestimar() throws Exception {
        CasoFiscalizacion c = casos.notificado(1, EN_UNA_SEMANA);

        assertThrows(ConflictoException.class, () -> fiscalizacion.resolver(c.getId(), true, "Exceso comprobado en la pesada."));
        assertEquals(EstadoCaso.NOTIFICADA, c.getEstado());
        assertEquals("DESESTIMADA", fiscalizacion.resolver(c.getId(), false, "Error de carga de la capacidad del vehículo.").getEstado());
    }

    @Test
    void vencidoElPlazoSinDescargoElFuncionarioPuedeConfirmar() throws Exception {
        CasoFiscalizacion c = casos.notificado(1, AYER);

        assertTrue(fiscalizacion.caso(c.getId()).isPlazoVencido());
        assertEquals("CONFIRMADA", fiscalizacion.resolver(c.getId(), true, "Exceso comprobado y sin descargo.").getEstado());
    }

    @Test
    void unCasoResueltoNoSeVuelveAResolver() throws Exception {
        CasoFiscalizacion c = casos.notificado(1, AYER);
        fiscalizacion.resolver(c.getId(), true, "Exceso comprobado y sin descargo.");

        assertThrows(ConflictoException.class, () -> fiscalizacion.resolver(c.getId(), false, "Cambio de opinión del funcionario."));
    }

    @Test
    void resolverSinFundamentoNoSeAdmite() {
        CasoFiscalizacion c = casos.notificado(1, AYER);
        assertThrows(NegocioException.class, () -> fiscalizacion.resolver(c.getId(), true, "  "));
        assertThrows(NegocioException.class, () -> fiscalizacion.resolver(c.getId(), true, "corto"));
        assertTrue(auditoria.registros.isEmpty());
    }

    @Test
    void elDescargoFueraDePlazoOEnUnCasoAjenoSeRechaza() throws Exception {
        CasoFiscalizacion vencido = casos.notificado(1, AYER);
        assertThrows(ConflictoException.class,
                () -> infracciones.presentarDescargo(1L, vencido.getId(), "Descargo presentado tarde por la empresa."));

        CasoFiscalizacion deOtra = casos.notificado(2, EN_UNA_SEMANA);
        assertThrows(RecursoAjenoException.class,
                () -> infracciones.presentarDescargo(1L, deOtra.getId(), "Intento sobre un caso de otra empresa."));
        assertThrows(RecursoAjenoException.class, () -> infracciones.infracciones(2L, 0, 20));
    }

    @Test
    void unSegundoDescargoSeRechaza() throws Exception {
        CasoFiscalizacion c = casos.notificado(1, EN_UNA_SEMANA);
        infracciones.presentarDescargo(1L, c.getId(), "Primer descargo de la empresa con su fundamento.");

        assertThrows(ConflictoException.class,
                () -> infracciones.presentarDescargo(1L, c.getId(), "Segundo descargo que no corresponde."));
    }

    @Test
    void unFuncionarioActivoNoOperaComoResponsable() {
        CasoFiscalizacion c = casos.notificado(1, EN_UNA_SEMANA);
        Falsos.CompatibilidadFalsa compatibilidad = new Falsos.CompatibilidadFalsa();
        compatibilidad.funcionariosActivos.add(RESPONSABLE);
        compatibilidad.responsablesActivos.add(RESPONSABLE);
        infracciones.compatibilidad = compatibilidad;

        assertThrows(EJBAccessException.class, () -> infracciones.infraccion(1L, c.getId()));
    }

    @Test
    void laEmpresaSoloVeSusCasos() throws Exception {
        casos.notificado(1, EN_UNA_SEMANA);
        casos.notificado(1, EN_UNA_SEMANA);
        casos.notificado(2, EN_UNA_SEMANA);

        assertEquals(2, infracciones.infracciones(1L, 0, 20).getTotal());
        assertEquals(3, fiscalizacion.bandeja(null, null, 0, 20).getTotal());
        assertEquals(3, fiscalizacion.bandeja("NOTIFICADA", "EXCESO_CARGA", 0, 20).getTotal());
        assertThrows(NegocioException.class, () -> fiscalizacion.bandeja("ABIERTA", null, 0, 20));
    }

    @Test
    void elReporteCalculaLaTasaDeConfirmacion() throws Exception {
        CasosEnMemoria conConteo = new CasosEnMemoria() {
            @Override
            public List<Object[]> conteoPorTipoYEstado(Instant desde, Instant hasta) {
                return List.of(
                        new Object[]{uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion.EXCESO_CARGA, EstadoCaso.CONFIRMADA, 3L},
                        new Object[]{uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion.EXCESO_CARGA, EstadoCaso.DESESTIMADA, 1L},
                        new Object[]{uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion.DESVIO_RUTA, EstadoCaso.NOTIFICADA, 4L});
            }
        };
        fiscalizacion.dao = conConteo;

        ReporteFiscalizacionDTO r = fiscalizacion.reporte("2026-10-01", "2026-10-31");

        assertEquals(8, r.getDetectados());
        assertEquals(3, r.getConfirmados());
        assertEquals(4, r.getPendientes());
        assertEquals(0.375, r.getTasaConfirmacion());
        assertEquals(4, r.getPorTipo().size());
        assertThrows(NegocioException.class, () -> fiscalizacion.reporte("2026-10-31", "2026-10-01"));
    }
}
