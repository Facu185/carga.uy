package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.AccionAuditada;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.dto.PersonaDTO;
import uy.edu.fing.tse.carga.api.dto.SesionDTO;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.seguridad.IdentidadGubUy;
import uy.edu.fing.tse.carga.ejb.seguridad.TokenService;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngresoGubUyBeanTest {

    private static final String CEDULA = "12345672";

    private IngresoGubUyBean servicio;
    private Falsos.UsuariosEnMemoria usuarios;
    private Falsos.FuncionariosEnMemoria funcionarios;
    private Falsos.TokensFalsos tokens;
    private Falsos.AuditoriaFalsa auditoria;
    private AtomicInteger consultasPdi;

    @BeforeEach
    void preparar() {
        usuarios = new Falsos.UsuariosEnMemoria();
        funcionarios = new Falsos.FuncionariosEnMemoria();
        tokens = new Falsos.TokensFalsos();
        auditoria = new Falsos.AuditoriaFalsa();
        consultasPdi = new AtomicInteger();
        servicio = new IngresoGubUyBean();
        servicio.usuarios = usuarios;
        servicio.funcionarios = funcionarios;
        servicio.tokens = tokens;
        servicio.auditoria = auditoria;
        conPdi(new PersonaDTO(CEDULA, "Ana Perez", "1990-05-20"));
    }

    private void conPdi(PersonaDTO respuesta) {
        servicio.pdi = cedula -> {
            consultasPdi.incrementAndGet();
            return respuesta;
        };
    }

    private SesionDTO ingresar(String documento, String nombre, String email) throws NegocioException {
        return servicio.ingresar(new IdentidadGubUy("sub", documento, email, nombre));
    }

    @Test
    void elPrimerIngresoRegistraALaPersonaComoCiudadanaYLaVerificaConDnic() throws NegocioException {
        SesionDTO sesion = ingresar("1.234.567-2", "Ana Perez", "ana@example.com");

        Usuario ana = usuarios.buscarPorCedula(CEDULA).orElseThrow();
        assertEquals("ana@example.com", ana.getCorreo());
        assertEquals(LocalDate.parse("1990-05-20"), ana.getFechaNacimiento());
        assertTrue(ana.tieneRol(Rol.CIUDADANO));
        assertEquals("CIUDADANO", sesion.getRol());
        assertEquals(TokenService.ORIGEN_GUBUY, tokens.ultimoOrigen);
        assertEquals(AccionAuditada.INGRESO_EXITOSO, auditoria.ultimo().accion());
    }

    @Test
    void siLaPdiNoRespondeElIngresoSigueYQuedaPendiente() throws NegocioException {
        conPdi(null);

        ingresar(CEDULA, "Ana Perez", null);

        Usuario ana = usuarios.buscarPorCedula(CEDULA).orElseThrow();
        assertFalse(ana.estaVerificadoConDnic());
        assertNull(ana.esMayorDeEdad(LocalDate.now()));
    }

    @Test
    void siYaEstabaVerificadoNoVuelveAConsultar() throws NegocioException {
        ingresar(CEDULA, "Ana Perez", null);
        ingresar(CEDULA, "Ana Perez", null);
        assertEquals(1, consultasPdi.get());
    }

    @Test
    void unIngresoPosteriorNoDuplicaYActualizaElCorreo() throws NegocioException {
        ingresar(CEDULA, "Ana Perez", "vieja@example.com");
        ingresar(CEDULA, "Ana Perez", "nueva@example.com");

        assertEquals(1, usuarios.usuarios.size());
        assertEquals("nueva@example.com", usuarios.buscarPorCedula(CEDULA).orElseThrow().getCorreo());
    }

    @Test
    void unFuncionarioActivoRecibeElRolYUnoDadoDeBajaLoPierde() throws NegocioException {
        Usuario ana = usuarios.guardar(new Usuario(CEDULA, "Ana Perez", null));
        Funcionario f = funcionarios.guardar(new Funcionario(ana, "DNT"));

        assertEquals("FUNCIONARIO", ingresar(CEDULA, "Ana Perez", null).getRol());
        assertEquals(Set.of("CIUDADANO", "FUNCIONARIO"), tokens.ultimosRoles);

        f.setActivo(false);
        assertEquals("CIUDADANO", ingresar(CEDULA, "Ana Perez", null).getRol());
        assertFalse(ana.tieneRol(Rol.FUNCIONARIO));
    }

    @Test
    void sinNombreSeMuestraLaCedula() throws NegocioException {
        assertEquals(CEDULA, ingresar(CEDULA, null, null).getNombre());
    }

    @Test
    void rechazaUnaCedulaInvalida() {
        assertThrows(NegocioException.class, () -> ingresar("12345678", "X", null));
        assertThrows(NegocioException.class, () -> ingresar(null, "X", null));
    }
}
