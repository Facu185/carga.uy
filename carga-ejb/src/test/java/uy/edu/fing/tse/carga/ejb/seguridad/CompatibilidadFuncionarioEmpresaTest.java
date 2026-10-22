package uy.edu.fing.tse.carga.ejb.seguridad;

import jakarta.ejb.EJBAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.TipoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba primero: decisiones y llamadas JPA; no ejecuta SQL ni exclusion real. */
class CompatibilidadFuncionarioEmpresaTest {
    @Test void funcionarioActivoNoPuedeRecibirResponsabilidadYSeBloqueaAntesDeConsultar() throws Exception {
        Escenario e = new Escenario(); e.funcionarios = 1;
        assertThrows(NegocioException.class, () -> e.regla.verificarPuedeSerResponsable(e.persona));
        assertEquals(List.of("bloqueo", "funcionario"), e.orden);
        assertSame(e.persona, e.bloqueada);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, e.modo);
        assertTrue(e.consultaFuncionario.contains("f.activo = true"));
        assertEquals("persona-sintetica", e.parametros.get("cedula"));
    }

    @Test void sinFuncionarioActivoPuedeSerResponsableSinDependerDelRolCiudadanoOChofer() throws Exception {
        Escenario e = new Escenario();
        assertDoesNotThrow(() -> e.regla.verificarPuedeSerResponsable(e.persona));
        assertEquals(List.of("bloqueo", "funcionario"), e.orden);
    }

    @Test void responsableActivoNoPuedeSerFuncionarioSinRecortarListaNiExigirEmpresaActiva() throws Exception {
        Escenario e = new Escenario(); e.responsables = 1;
        assertThrows(NegocioException.class, () -> e.regla.verificarPuedeSerFuncionario(e.persona));
        assertEquals(List.of("bloqueo", "responsable"), e.orden);
        assertTrue(e.consultaResponsable.startsWith("select count(v)"));
        assertTrue(e.consultaResponsable.contains("v.tipo = :tipo"));
        assertTrue(e.consultaResponsable.contains("v.estado = :estado"));
        assertFalse(e.consultaResponsable.contains("empresa.activa"));
        assertEquals(TipoVinculoEmpresa.RESPONSABLE, e.parametros.get("tipo"));
        assertEquals(EstadoVinculoEmpresa.ACTIVO, e.parametros.get("estado"));
    }

    @Test void pendientesRevocadosYOtrosTiposNoImpidenHabilitarFuncionario() throws Exception {
        // El doble retorna el conteo de RESPONSABLE/ACTIVO; no prueba el filtro SQL.
        Escenario e = new Escenario();
        assertDoesNotThrow(() -> e.regla.verificarPuedeSerFuncionario(e.persona));
        assertEquals(List.of("bloqueo", "responsable"), e.orden);
    }

    @Test void combinacionHeredadaSeDeniegaSinMutarNiBloquearParaEscritura() throws Exception {
        Escenario e = new Escenario(); e.funcionarios = 1; e.responsables = 1;
        assertThrows(EJBAccessException.class, () -> e.regla.verificarEjercicioResponsabilidad("persona-sintetica"));
        assertEquals(List.of("funcionario", "responsable"), e.orden);
        assertNull(e.bloqueada);
    }

    @Test void funcionarioInactivoNoImpideEjercerResponsabilidad() throws Exception {
        Escenario e = new Escenario(); e.responsables = 1;
        assertDoesNotThrow(() -> e.regla.verificarEjercicioResponsabilidad("persona-sintetica"));
        assertEquals(List.of("funcionario"), e.orden);
    }

    @Test void sinResponsabilidadNoAmpliaD004NiProhibeElRolChofer() throws Exception {
        Escenario e = new Escenario(); e.funcionarios = 1;
        assertDoesNotThrow(() -> e.regla.verificarEjercicioResponsabilidad("persona-sintetica"));
        assertEquals(List.of("funcionario", "responsable"), e.orden);
    }

    private static final class Escenario {
        final CompatibilidadFuncionarioEmpresa regla = new CompatibilidadFuncionarioEmpresa();
        final Usuario persona = new Usuario("persona-sintetica", "Prueba", null);
        final List<String> orden = new ArrayList<>();
        final Map<String, Object> parametros = new HashMap<>();
        long funcionarios, responsables;
        Usuario bloqueada;
        LockModeType modo;
        String consultaFuncionario, consultaResponsable;

        Escenario() throws Exception {
            var campo = CompatibilidadFuncionarioEmpresa.class.getDeclaredField("em");
            campo.setAccessible(true);
            campo.set(regla, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "lock" -> {
                            orden.add("bloqueo"); bloqueada = (Usuario) a[0]; modo = (LockModeType) a[1]; yield null;
                        }
                        case "createQuery" -> consulta((String) a[0]);
                        default -> throw new UnsupportedOperationException(m.getName());
                    }));
        }

        Object consulta(String jpql) {
            boolean funcionario = jpql.contains("from Funcionario");
            if (funcionario) { consultaFuncionario = jpql; orden.add("funcionario"); }
            else { consultaResponsable = jpql; orden.add("responsable"); }
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "setParameter" -> { parametros.put((String) a[0], a[1]); yield p; }
                        case "getSingleResult" -> funcionario ? funcionarios : responsables;
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
        }
    }
}
