package uy.edu.fing.tse.carga.ejb.seguridad;

import jakarta.ejb.EJBAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
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
        assertEquals(List.of("bloqueo", "identidadChofer", "funcionario"), e.orden);
        assertSame(e.persona, e.bloqueada);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, e.modo);
        assertTrue(e.consultaFuncionario.contains("f.activo = true"));
        assertEquals("persona-sintetica", e.parametros.get("cedula"));
    }

    @Test void sinFuncionarioNiChoferActivoPuedeSerResponsable() throws Exception {
        Escenario e = new Escenario();
        assertDoesNotThrow(() -> e.regla.verificarPuedeSerResponsable(e.persona));
        assertEquals(List.of("bloqueo", "identidadChofer", "funcionario", "chofer"), e.orden);
    }

    @Test void choferActivoNoPuedeSerResponsableAunqueSuTokenNoLoIndique() throws Exception {
        Escenario e = new Escenario(); e.choferes = 1;
        assertThrows(NegocioException.class, () -> e.regla.verificarPuedeSerResponsable(e.persona));
        assertEquals(List.of("bloqueo", "identidadChofer", "funcionario", "chofer"), e.orden);
        assertTrue(e.consultaChofer.contains("c.activo = true"));
        assertEquals("persona-sintetica", e.parametros.get("cedula"));
    }

    @Test void responsableActivoNoPuedeHabilitarseComoChofer() throws Exception {
        Escenario e = new Escenario(); e.responsables = 1;
        assertThrows(NegocioException.class, () -> e.regla.verificarPuedeSerChofer("persona-sintetica"));
        assertEquals(List.of("identidadChofer", "responsable"), e.orden);
        assertFalse(e.consultaResponsable.contains("empresa.activa"));
    }

    @Test void sinResponsabilidadPuedeHabilitarseChoferInclusoAntesDelPrimerLogin() throws Exception {
        Escenario e = new Escenario();
        assertDoesNotThrow(() -> e.regla.verificarPuedeSerChofer("persona-sintetica"));
        assertEquals(List.of("identidadChofer", "responsable"), e.orden);
        assertNull(e.bloqueada);
        assertTrue(e.consultaBloqueo.contains("pg_advisory_xact_lock(?1, ?2)"));
        assertEquals(0x43485253, e.parametrosBloqueo.get(1));
        assertEquals("persona-sintetica".hashCode(), e.parametrosBloqueo.get(2));
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

    @Test void funcionarioYChoferInactivosNoImpidenEjercerResponsabilidad() throws Exception {
        Escenario e = new Escenario(); e.responsables = 1;
        assertDoesNotThrow(() -> e.regla.verificarEjercicioResponsabilidad("persona-sintetica"));
        assertEquals(List.of("funcionario", "chofer"), e.orden);
    }

    @Test void choferConResponsabilidadHeredadaNoPuedeEjercerlaSinRevocarDatos() throws Exception {
        Escenario e = new Escenario(); e.choferes = 1; e.responsables = 1;
        assertThrows(EJBAccessException.class, () -> e.regla.verificarEjercicioResponsabilidad("persona-sintetica"));
        assertEquals(List.of("funcionario", "chofer", "responsable"), e.orden);
        assertNull(e.bloqueada);
        assertEquals(1, e.choferes);
        assertEquals(1, e.responsables);
    }

    @Test void choferSinResponsabilidadConservaSusDerechosPropios() throws Exception {
        Escenario e = new Escenario(); e.choferes = 1;
        assertDoesNotThrow(() -> e.regla.verificarEjercicioResponsabilidad("persona-sintetica"));
        assertEquals(List.of("funcionario", "chofer", "responsable"), e.orden);
    }

    @Test void sinResponsabilidadNoAmpliaD004NiProhibeElRolChofer() throws Exception {
        Escenario e = new Escenario(); e.funcionarios = 1;
        assertDoesNotThrow(() -> e.regla.verificarEjercicioResponsabilidad("persona-sintetica"));
        assertEquals(List.of("funcionario", "responsable", "chofer"), e.orden);
    }

    private static final class Escenario {
        final CompatibilidadFuncionarioEmpresa regla = new CompatibilidadFuncionarioEmpresa();
        final Usuario persona = new Usuario("persona-sintetica", "Prueba", null);
        final List<String> orden = new ArrayList<>();
        final Map<String, Object> parametros = new HashMap<>();
        final Map<Integer, Object> parametrosBloqueo = new HashMap<>();
        long funcionarios, responsables, choferes;
        Usuario bloqueada;
        LockModeType modo;
        String consultaFuncionario, consultaResponsable, consultaChofer, consultaBloqueo;

        Escenario() throws Exception {
            var campo = CompatibilidadFuncionarioEmpresa.class.getDeclaredField("em");
            campo.setAccessible(true);
            campo.set(regla, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "lock" -> {
                            orden.add("bloqueo"); bloqueada = (Usuario) a[0]; modo = (LockModeType) a[1]; yield null;
                        }
                        case "createQuery" -> consulta((String) a[0]);
                        case "createNativeQuery" -> {
                            consultaBloqueo = (String) a[0];
                            yield Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Query.class},
                                    (q, metodo, args) -> switch (metodo.getName()) {
                                        case "setParameter" -> { parametrosBloqueo.put((Integer) args[0], args[1]); yield q; }
                                        case "getSingleResult" -> { orden.add("identidadChofer"); yield 1L; }
                                        default -> throw new UnsupportedOperationException(metodo.getName());
                                    });
                        }
                        default -> throw new UnsupportedOperationException(m.getName());
                    }));
        }

        Object consulta(String jpql) {
            boolean funcionario = jpql.contains("from Funcionario");
            boolean chofer = jpql.contains("from Chofer");
            if (funcionario) { consultaFuncionario = jpql; orden.add("funcionario"); }
            else if (chofer) { consultaChofer = jpql; orden.add("chofer"); }
            else { consultaResponsable = jpql; orden.add("responsable"); }
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "setParameter" -> { parametros.put((String) a[0], a[1]); yield p; }
                        case "getSingleResult" -> funcionario ? funcionarios : chofer ? choferes : responsables;
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
        }
    }
}
