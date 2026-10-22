package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.ApplicationException;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.EditarPerfilEmpresaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba primero: decisiones y efectos con dobles; no acredita roles, JPQL ni rollback JTA. */
class PerfilEmpresaServicioBeanTest {
    @Test void consultaPropiaDevuelveDtoSinMutarNiBloquearParaEscritura() throws Exception {
        Escenario e = new Escenario();
        EmpresaDTO dto = e.bean.consultar(1L);
        assertEquals(1L, dto.getId());
        assertEquals("ORIGINAL", dto.getNombre());
        assertEquals("persona-sintetica", e.cedula);
        assertEquals(List.of(1L), e.autorizaciones);
        assertTrue(e.bloqueos.isEmpty());
        assertEquals(0, e.flushes);
        sinCambios(e);
    }

    @Test void edicionSoloCambiaLosCuatroCamposYConservaDatosAdministrativos() throws Exception {
        Escenario e = new Escenario();
        EmpresaDTO dto = e.bean.modificar(1L, datos());
        assertEquals("NUEVA", dto.getNombre());
        assertEquals("RAZON NUEVA", dto.getRazonSocial());
        assertEquals("DIRECCION NUEVA", dto.getDireccion());
        assertEquals(200L, dto.getNumeroEmpresa());
        assertEquals(1L, dto.getId());
        assertTrue(dto.isActiva());
        assertEquals(LocalDate.of(2020, 1, 1), dto.getFechaAlta());
        assertEquals(List.of(LockModeType.PESSIMISTIC_WRITE), e.bloqueos);
        assertEquals(1, e.flushes);
    }

    @Test void fechaAltaHeredadaNulaNoSeCompletaAlEditarPerfil() throws Exception {
        Escenario e = new Escenario();
        e.empresa.setFechaAlta(null);
        assertNull(e.bean.modificar(1L, datos()).getFechaAlta());
    }

    @Test void razonSocialYDireccionOpcionalesSeConservanComoNull() throws Exception {
        Escenario e = new Escenario();
        EditarPerfilEmpresaDTO dto = datos();
        dto.setRazonSocial(null); dto.setDireccion(null);
        EmpresaDTO respuesta = e.bean.modificar(1L, dto);
        assertNull(respuesta.getRazonSocial()); assertNull(respuesta.getDireccion());
    }

    @Test void empresaAjenaNoSeLeeNiModifica() throws Exception {
        Escenario e = new Escenario();
        assertThrows(EJBAccessException.class, () -> e.bean.consultar(2L));
        assertThrows(EJBAccessException.class, () -> e.bean.modificar(2L, datos()));
        assertEquals(0, e.busquedas);
        sinCambios(e);
    }

    @Test void sinResponsableActivoSeDenieganAmbasOperacionesAntesDeLeerEntidad() throws Exception {
        // El doble representa el false del DAO real; no demuestra su consulta ni sus estados.
        for (String caso : List.of("AUSENTE", "REVOCADO", "PENDIENTE", "OPERADOR_ACTIVO")) {
            Escenario e = new Escenario(); e.responsable = false;
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(1L), caso);
            assertThrows(EJBAccessException.class, () -> e.bean.modificar(1L, datos()), caso);
            assertEquals(0, e.busquedas, caso);
            sinCambios(e);
        }
    }

    @Test void sinPrincipalValidoNoConsultaVinculoNiEmpresa() throws Exception {
        for (String nombre : new String[]{null, " "}) {
            Escenario e = new Escenario(); e.principal = nombre == null ? null : () -> nombre;
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(1L));
            assertThrows(EJBAccessException.class, () -> e.bean.modificar(1L, datos()));
            assertTrue(e.autorizaciones.isEmpty()); assertEquals(0, e.busquedas);
            sinCambios(e);
        }
    }

    @Test void inactivaEInexistenteNoSeDistinguenNiSeModifican() throws Exception {
        for (boolean existe : List.of(true, false)) {
            Escenario e = new Escenario(); e.existe = existe; e.empresa.setActiva(false);
            String lectura = assertThrows(EJBAccessException.class, () -> e.bean.consultar(1L)).getMessage();
            String edicion = assertThrows(EJBAccessException.class, () -> e.bean.modificar(1L, datos())).getMessage();
            assertEquals(lectura, edicion);
            assertEquals("No tiene acceso al perfil de esta empresa.", lectura);
            assertEquals(0, e.flushes); assertEquals(0, e.consultas);
            assertFalse(e.empresa.isActiva()); assertEquals("ORIGINAL", e.empresa.getNombre());
        }
    }

    @Test void identificadorInvalidoEsNegocioSinAccesoAPersistencia() throws Exception {
        for (Long id : new Long[]{null, 0L, -1L}) {
            Escenario e = new Escenario();
            assertThrows(NegocioException.class, () -> e.bean.consultar(id));
            assertThrows(NegocioException.class, () -> e.bean.modificar(id, datos()));
            assertTrue(e.autorizaciones.isEmpty()); assertEquals(0, e.busquedas);
            sinCambios(e);
        }
    }

    @Test void datosInvalidosNoMutanNingunCampo() throws Exception {
        List<EditarPerfilEmpresaDTO> invalidos = new ArrayList<>();
        invalidos.add(null);
        for (String nombre : new String[]{null, " ", "n".repeat(201)}) {
            EditarPerfilEmpresaDTO dto = datos(); dto.setNombre(nombre); invalidos.add(dto);
        }
        for (long numero : new long[]{0L, -1L}) {
            EditarPerfilEmpresaDTO dto = datos(); dto.setNumeroEmpresa(numero); invalidos.add(dto);
        }
        EditarPerfilEmpresaDTO razon = datos(); razon.setRazonSocial("r".repeat(201)); invalidos.add(razon);
        EditarPerfilEmpresaDTO direccion = datos(); direccion.setDireccion("d".repeat(301)); invalidos.add(direccion);
        for (EditarPerfilEmpresaDTO dto : invalidos) {
            Escenario e = new Escenario();
            assertThrows(NegocioException.class, () -> e.bean.modificar(1L, dto));
            assertEquals(0, e.flushes); sinCambios(e);
        }
    }

    @Test void numeroDuplicadoSeRechazaAntesDeAsignarCampos() throws Exception {
        Escenario e = new Escenario(); e.duplicados = 1L;
        assertThrows(NegocioException.class, () -> e.bean.modificar(1L, datos()));
        assertEquals(200L, e.parametros.get("numero"));
        assertEquals(1L, e.parametros.get("id"));
        assertTrue(e.jpql.contains("e.id <> :id"));
        assertEquals(0, e.flushes); sinCambios(e);
    }

    @Test void mismoNumeroNoSeConsideraDuplicadoDeSiMismo() throws Exception {
        Escenario e = new Escenario(); EditarPerfilEmpresaDTO dto = datos(); dto.setNumeroEmpresa(100L);
        assertEquals(100L, e.bean.modificar(1L, dto).getNumeroEmpresa());
        assertEquals(1L, e.parametros.get("id"));
    }

    @Test void unicidadDetectadaAlFlushSeTraduceANegocioConRollbackDeclarado() throws Exception {
        Escenario e = new Escenario();
        e.errorFlush = new PersistenceException(new SQLException("duplicado sintetico", "23505"));
        assertThrows(NegocioException.class, () -> e.bean.modificar(1L, datos()));
        assertEquals(1, e.flushes);
        assertTrue(NegocioException.class.getAnnotation(ApplicationException.class).rollback());
        // El doble NO restaura la entidad ni acredita rollback real.
    }

    @Test void otrosErroresDePersistenciaNoSeOcultanComoDuplicado() throws Exception {
        Escenario e = new Escenario();
        e.errorFlush = new PersistenceException(new SQLException("fallo sintetico", "23502"));
        assertSame(e.errorFlush, assertThrows(PersistenceException.class, () -> e.bean.modificar(1L, datos())));
    }

    @Test void dtoDeEdicionNoExponeCamposAdministrativos() {
        Set<String> atributos = Arrays.stream(EditarPerfilEmpresaDTO.class.getDeclaredFields())
                .filter(campo -> !Modifier.isStatic(campo.getModifiers())).map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("nombre", "razonSocial", "numeroEmpresa", "direccion"), atributos);
        assertEquals(Object.class, EditarPerfilEmpresaDTO.class.getSuperclass());
    }

    @Test void rolesDePerfilYAdministracionPermanecenSeparados() throws Exception {
        assertEquals(Set.of(Roles.CIUDADANO, Roles.CHOFER),
                Set.of(PerfilEmpresaServicioBean.class.getAnnotation(RolesAllowed.class).value()));
        assertEquals(Set.of(Roles.FUNCIONARIO, Roles.ADMINISTRADOR), Set.of(EmpresaServicioBean.class
                .getMethod("modificar", Long.class, EmpresaDTO.class).getAnnotation(RolesAllowed.class).value()));
    }

    private static EditarPerfilEmpresaDTO datos() {
        EditarPerfilEmpresaDTO dto = new EditarPerfilEmpresaDTO();
        dto.setNombre(" NUEVA "); dto.setRazonSocial("RAZON NUEVA");
        dto.setNumeroEmpresa(200L); dto.setDireccion("DIRECCION NUEVA");
        return dto;
    }

    private static void sinCambios(Escenario e) {
        assertEquals(1L, e.empresa.getId()); assertEquals("ORIGINAL", e.empresa.getNombre());
        assertEquals("RAZON ORIGINAL", e.empresa.getRazonSocial());
        assertEquals("DIRECCION ORIGINAL", e.empresa.getDireccion());
        assertEquals(100L, e.empresa.getNumeroEmpresa()); assertTrue(e.empresa.isActiva());
        assertEquals(LocalDate.of(2020, 1, 1), e.empresa.getFechaAlta());
    }

    private static void campo(Object objeto, String nombre, Object valor) throws Exception {
        var campo = objeto.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(objeto, valor);
    }

    private static final class Escenario {
        final PerfilEmpresaServicioBean bean = new PerfilEmpresaServicioBean();
        final Empresa empresa = new Empresa();
        final List<Long> autorizaciones = new ArrayList<>();
        final List<LockModeType> bloqueos = new ArrayList<>();
        final Map<String, Object> parametros = new HashMap<>();
        Principal principal = () -> "persona-sintetica";
        boolean responsable = true, existe = true;
        int busquedas, consultas, flushes;
        long duplicados;
        String cedula, jpql;
        PersistenceException errorFlush;

        Escenario() throws Exception {
            campo(empresa, "id", 1L); empresa.setNombre("ORIGINAL"); empresa.setRazonSocial("RAZON ORIGINAL");
            empresa.setDireccion("DIRECCION ORIGINAL"); empresa.setNumeroEmpresa(100L);
            empresa.setFechaAlta(LocalDate.of(2020, 1, 1));
            campo(bean, "contexto", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                    (p, m, a) -> { if (m.getName().equals("getCallerPrincipal")) return principal;
                        throw new UnsupportedOperationException(m.getName()); }));
            campo(bean, "vinculos", new VinculoEmpresaUsuarioDAO() {
                @Override public boolean esResponsableActivo(String identidad, Long id) {
                    autorizaciones.add(id); cedula = identidad; return responsable && id.equals(1L);
                }
            });
            campo(bean, "em", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "find" -> {
                            busquedas++; assertEquals(Empresa.class, a[0]); assertEquals(1L, a[1]);
                            if (a.length == 3) bloqueos.add((LockModeType) a[2]);
                            yield existe ? empresa : null;
                        }
                        case "createQuery" -> {
                            consultas++; jpql = (String) a[0];
                            yield Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class},
                                    (q, metodo, args) -> switch (metodo.getName()) {
                                        case "setParameter" -> { parametros.put((String) args[0], args[1]); yield q; }
                                        case "getSingleResult" -> duplicados;
                                        default -> throw new UnsupportedOperationException(metodo.getName());
                                    });
                        }
                        case "flush" -> { flushes++; if (errorFlush != null) throw errorFlush; yield null; }
                        default -> throw new UnsupportedOperationException(m.getName());
                    }));
        }
    }
}
