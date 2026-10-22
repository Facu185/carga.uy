package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.funcionario.NuevaSolicitudDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.RechazoDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.SolicitudFuncionarioDTO;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudesFuncionarioBeanTest {

    private static final String CEDULA = "12345672";

    private SolicitudesFuncionarioBean servicio;
    private Falsos.SolicitudesEnMemoria solicitudes;
    private Falsos.FuncionariosEnMemoria funcionarios;
    private Falsos.AuditoriaFalsa auditoria;
    private Usuario ana;
    private Falsos.CompatibilidadFalsa compatibilidad;

    @BeforeEach
    void preparar() {
        Falsos.UsuariosEnMemoria usuarios = new Falsos.UsuariosEnMemoria();
        ana = usuarios.guardar(new Usuario(CEDULA, "Ana Perez", "ana@example.com"));
        solicitudes = new Falsos.SolicitudesEnMemoria();
        funcionarios = new Falsos.FuncionariosEnMemoria();
        auditoria = new Falsos.AuditoriaFalsa();
        servicio = new SolicitudesFuncionarioBean();
        compatibilidad = new Falsos.CompatibilidadFalsa();
        servicio.compatibilidad = compatibilidad;
        servicio.usuarios = usuarios;
        servicio.solicitudes = solicitudes;
        servicio.funcionarios = funcionarios;
        servicio.auditoria = auditoria;
        servicio.contexto = Falsos.sesion(CEDULA, "CIUDADANO");
    }

    private SolicitudFuncionarioDTO solicitar() throws NegocioException {
        return servicio.solicitar(new NuevaSolicitudDTO(" DNT "));
    }

    private void comoAdministrador() {
        servicio.contexto = Falsos.sesion("admin", "ADMINISTRADOR");
    }

    @Test
    void responsabilidadActivaImpideAprobacionSinResolverLaSolicitudNiDarRol() throws NegocioException {
        SolicitudFuncionarioDTO s = solicitar();
        compatibilidad.responsablesActivos.add(CEDULA);
        comoAdministrador();
        assertThrows(NegocioException.class, () -> servicio.aprobar(s.getId()));
        assertTrue(solicitudes.buscarPorId(s.getId()).orElseThrow().estaPendiente());
        assertTrue(funcionarios.funcionarios.isEmpty());
        assertFalse(ana.tieneRol(Rol.FUNCIONARIO));
        assertEquals(1, auditoria.registros.size());
        assertEquals(java.util.List.of(CEDULA), compatibilidad.bloqueadas);
    }

    @Test
    void responsabilidadActivaImpideReactivacionPorSolicitudYConservaDependencia() throws NegocioException {
        Funcionario anterior = funcionarios.guardar(new Funcionario(ana, "Otra"));
        anterior.setActivo(false);
        SolicitudFuncionarioDTO s = solicitar();
        compatibilidad.responsablesActivos.add(CEDULA);
        comoAdministrador();
        assertThrows(NegocioException.class, () -> servicio.aprobar(s.getId()));
        assertFalse(anterior.isActivo());
        assertEquals("Otra", anterior.getDependencia());
        assertTrue(solicitudes.buscarPorId(s.getId()).orElseThrow().estaPendiente());
    }

    @Test
    void solicitaConLosDatosVerificadosDeLaPersona() throws NegocioException {
        SolicitudFuncionarioDTO s = solicitar();

        assertEquals(CEDULA, s.getCedula());
        assertEquals("Ana Perez", s.getNombre());
        assertEquals("ana@example.com", s.getEmail());
        assertEquals("DNT", s.getDependencia());
        assertEquals("PENDIENTE", s.getEstado());
        assertEquals(AccionAuditada.SOLICITUD_FUNCIONARIO_CREADA, auditoria.ultimo().accion());
    }

    @Test
    void exigeDependencia() {
        assertThrows(NegocioException.class, () -> servicio.solicitar(new NuevaSolicitudDTO(" ")));
    }

    @Test
    void noPermiteDosPendientesPeroSiVolverAPedirTrasUnRechazo() throws NegocioException {
        SolicitudFuncionarioDTO primera = solicitar();
        assertThrows(NegocioException.class, this::solicitar);

        comoAdministrador();
        servicio.rechazar(primera.getId(), new RechazoDTO("No figura en la nomina"));
        servicio.contexto = Falsos.sesion(CEDULA, "CIUDADANO");

        assertEquals("PENDIENTE", solicitar().getEstado());
    }

    @Test
    void siYaEsFuncionarioActivoNoPuedeSolicitar() {
        funcionarios.guardar(new Funcionario(ana, "DNT"));
        assertThrows(NegocioException.class, this::solicitar);
    }

    @Test
    void aprobarCreaElFuncionarioYLeDaElRol() throws NegocioException {
        SolicitudFuncionarioDTO s = solicitar();
        comoAdministrador();

        SolicitudFuncionarioDTO aprobada = servicio.aprobar(s.getId());

        assertEquals("APROBADA", aprobada.getEstado());
        assertEquals("admin", aprobada.getResueltaPor());
        Funcionario f = funcionarios.buscarPorCedula(CEDULA).orElseThrow();
        assertTrue(f.isActivo());
        assertEquals("DNT", f.getDependencia());
        assertTrue(ana.tieneRol(Rol.FUNCIONARIO));
    }

    @Test
    void aprobarReactivaAUnFuncionarioDadoDeBaja() throws NegocioException {
        Funcionario anterior = funcionarios.guardar(new Funcionario(ana, "Otra"));
        anterior.setActivo(false);
        SolicitudFuncionarioDTO s = solicitar();
        comoAdministrador();

        servicio.aprobar(s.getId());

        assertEquals(1, funcionarios.funcionarios.size());
        assertTrue(anterior.isActivo());
        assertEquals("DNT", anterior.getDependencia());
    }

    @Test
    void noSeResuelveDosVecesYElRechazoExigeMotivo() throws NegocioException {
        SolicitudFuncionarioDTO s = solicitar();
        comoAdministrador();

        assertThrows(NegocioException.class, () -> servicio.rechazar(s.getId(), new RechazoDTO(" ")));
        servicio.rechazar(s.getId(), new RechazoDTO("No figura en la nomina"));
        assertEquals("No figura en la nomina", auditoria.ultimo().fundamento());
        assertThrows(NegocioException.class, () -> servicio.aprobar(s.getId()));
        assertThrows(NegocioException.class, () -> servicio.aprobar(99L));
    }

    @Test
    void miSolicitudEsNullSiNuncaPidioYListarValidaElEstado() throws NegocioException {
        assertNull(servicio.miSolicitud());
        solicitar();
        assertFalse(servicio.miSolicitud() == null);

        comoAdministrador();
        assertEquals(1, servicio.listar("pendiente").size());
        assertEquals(0, servicio.listar("APROBADA").size());
        assertThrows(NegocioException.class, () -> servicio.listar("cualquiera"));
    }
}
