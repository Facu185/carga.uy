package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.funcionario.CambioDependenciaDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.FuncionarioDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.NuevoFuncionarioDTO;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuncionariosBeanTest {

    private FuncionariosBean servicio;
    private Falsos.UsuariosEnMemoria usuarios;
    private Falsos.CompatibilidadFalsa compatibilidad;

    @BeforeEach
    void preparar() {
        usuarios = new Falsos.UsuariosEnMemoria();
        servicio = new FuncionariosBean();
        compatibilidad = new Falsos.CompatibilidadFalsa();
        servicio.compatibilidad = compatibilidad;
        servicio.dao = new Falsos.FuncionariosEnMemoria();
        servicio.usuarios = usuarios;
        servicio.auditoria = new Falsos.AuditoriaFalsa();
    }

    @Test
    void responsableActivoNoPuedeSerDadoDeAltaYNoSeLeAsignaRolNiSeAuditaExito() {
        Usuario persona = usuarios.guardar(new Usuario("12345672", "Prueba", null));
        persona.asignarRol(Falsos.rol(Rol.CIUDADANO));
        compatibilidad.responsablesActivos.add(persona.getCedula());
        assertThrows(NegocioException.class, () -> servicio.crear(new NuevoFuncionarioDTO("12345672", "Prueba", "DNT")));
        assertTrue(servicio.dao.listar().isEmpty());
        assertFalse(persona.tieneRol(Rol.FUNCIONARIO));
        assertTrue(persona.tieneRol(Rol.CIUDADANO));
        assertTrue(((Falsos.AuditoriaFalsa) servicio.auditoria).registros.isEmpty());
        assertEquals(java.util.List.of("12345672"), compatibilidad.bloqueadas);
    }

    @Test
    void responsableHeredadoNoSeReactivaPeroSePuedeDeshabilitarFuncionario() throws NegocioException {
        FuncionarioDTO f = servicio.crear(new NuevoFuncionarioDTO("12345672", "Prueba", "DNT"));
        servicio.cambiarEstado(f.getId(), false);
        compatibilidad.responsablesActivos.add(f.getCedula());
        int auditorias = ((Falsos.AuditoriaFalsa) servicio.auditoria).registros.size();
        assertThrows(NegocioException.class, () -> servicio.cambiarEstado(f.getId(), true));
        assertFalse(servicio.dao.buscarPorId(f.getId()).orElseThrow().isActivo());
        assertFalse(usuarios.buscarPorCedula(f.getCedula()).orElseThrow().tieneRol(Rol.FUNCIONARIO));
        assertEquals(auditorias, ((Falsos.AuditoriaFalsa) servicio.auditoria).registros.size());
        assertFalse(servicio.cambiarEstado(f.getId(), false).isActivo());
        assertTrue(compatibilidad.responsablesActivos.contains(f.getCedula()));
    }

    @Test
    void bajaDeCombinacionHeredadaResuelveConflictoSinRevocarResponsabilidad() throws NegocioException {
        FuncionarioDTO f = servicio.crear(new NuevoFuncionarioDTO("12345672", "Prueba", "DNT"));
        compatibilidad.responsablesActivos.add(f.getCedula());
        assertFalse(servicio.cambiarEstado(f.getId(), false).isActivo());
        assertFalse(usuarios.buscarPorCedula(f.getCedula()).orElseThrow().tieneRol(Rol.FUNCIONARIO));
        assertTrue(compatibilidad.responsablesActivos.contains(f.getCedula()));
    }

    @Test
    void elAltaManualNormalizaLaCedulaYDaElRol() throws NegocioException {
        FuncionarioDTO f = servicio.crear(new NuevoFuncionarioDTO("1.234.567-2", " Ana Perez ", "DNT"));

        assertEquals("12345672", f.getCedula());
        assertEquals("Ana Perez", f.getNombre());
        assertTrue(usuarios.buscarPorCedula("12345672").orElseThrow().tieneRol(Rol.FUNCIONARIO));
    }

    @Test
    void siLaPersonaYaIngresoUsaSuRegistroYMuestraSuCorreo() throws NegocioException {
        usuarios.guardar(new Usuario("12345672", "Ana Perez", "ana@example.com"));

        FuncionarioDTO f = servicio.crear(new NuevoFuncionarioDTO("12345672", "Ana", null));

        assertEquals(1, usuarios.usuarios.size());
        assertEquals("ana@example.com", f.getEmail());
    }

    @Test
    void validaLosDatosYNoPermiteDuplicados() throws NegocioException {
        assertThrows(NegocioException.class, () -> servicio.crear(new NuevoFuncionarioDTO("12345678", "X", null)));
        assertThrows(NegocioException.class, () -> servicio.crear(new NuevoFuncionarioDTO("12345672", " ", null)));
        servicio.crear(new NuevoFuncionarioDTO("12345672", "Ana", null));
        assertThrows(NegocioException.class, () -> servicio.crear(new NuevoFuncionarioDTO("12345672", "Ana", null)));
    }

    @Test
    void laBajaLeSacaElRolYLaDependenciaSeEdita() throws NegocioException {
        FuncionarioDTO f = servicio.crear(new NuevoFuncionarioDTO("12345672", "Ana", null));

        assertFalse(servicio.cambiarEstado(f.getId(), false).isActivo());
        assertFalse(usuarios.buscarPorCedula("12345672").orElseThrow().tieneRol(Rol.FUNCIONARIO));

        assertEquals("DNV", servicio.cambiarDependencia(f.getId(), new CambioDependenciaDTO(" DNV ")).getDependencia());
        assertThrows(NegocioException.class, () -> servicio.cambiarDependencia(f.getId(), new CambioDependenciaDTO("")));
        assertThrows(NegocioException.class, () -> servicio.cambiarEstado(99L, true));
    }
}
