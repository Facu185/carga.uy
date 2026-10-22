package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Dobles: comprueba decisiones y efectos; no ejecuta roles EJB, JPQL ni transacciones. */
class VehiculoServicioBeanAutorizacionTest {
    @Test void administradorListaGlobalSinConsultarVinculos() throws Exception {
        Escenario e = new Escenario(Roles.ADMINISTRADOR);
        assertEquals(1, e.bean.listar(null).size());
        assertTrue(e.empresasAutorizadas.isEmpty());
        assertFalse(e.jpql.getFirst().contains("where"));
    }

    @Test void ciudadanoYChoferResponsablesConsultanSoloSuEmpresa() throws Exception {
        for (String rol : List.of(Roles.CIUDADANO, Roles.CHOFER)) {
            Escenario e = new Escenario(rol);
            assertEquals(1, e.bean.listar(1L).size());
            assertEquals(List.of(1L), e.empresasAutorizadas);
            assertEquals("persona-prueba", e.cedulaConsultada);
            assertTrue(e.jpql.getFirst().contains("where v.empresa.id = :empresaId"));
            assertEquals(1L, e.parametros.get("empresaId"));
        }
    }

    @Test void noAdministradorNoPuedeConsultarGlobalNiEmpresaAjena() throws Exception {
        Escenario e = new Escenario(Roles.CIUDADANO);
        assertThrows(EJBAccessException.class, () -> e.bean.listar(null));
        assertTrue(e.empresasAutorizadas.isEmpty());
        assertThrows(EJBAccessException.class, () -> e.bean.listar(2L));
        assertTrue(e.jpql.isEmpty());
        assertEquals(0, e.persistidos);
    }

    @Test void ausenciaDeResponsableActivoDeniegaSinConsultarFlota() throws Exception {
        // El DAO real solo da true a RESPONSABLE/ACTIVO: estos escenarios devuelven false.
        for (String motivo : List.of("SIN_VINCULO", "CHOFER_ACTIVO", "RESPONSABLE_PENDIENTE", "RESPONSABLE_REVOCADO")) {
            Escenario e = new Escenario(Roles.CIUDADANO);
            e.permitidas.clear();
            assertThrows(EJBAccessException.class, () -> e.bean.listar(1L), motivo);
            assertTrue(e.jpql.isEmpty(), motivo);
        }
    }

    @Test void principalNuloOEnBlancoDeniegaAntesDeConsultarVinculo() throws Exception {
        for (String nombre : new String[]{null, " "}) {
            Escenario e = new Escenario(Roles.CIUDADANO);
            e.principal = nombre == null ? null : () -> nombre;
            assertThrows(EJBAccessException.class, () -> e.bean.listar(1L));
            assertTrue(e.empresasAutorizadas.isEmpty());
            assertTrue(e.jpql.isEmpty());
        }
    }

    @Test void altaPropiaPermitidaPeroAltaAjenaNoConsultaNiPersiste() throws Exception {
        Escenario e = new Escenario(Roles.CIUDADANO);
        assertEquals(1L, e.bean.agregar(datos(1L)).getEmpresaId());
        assertEquals(1, e.persistidos);
        Escenario ajeno = new Escenario(Roles.CHOFER);
        assertThrows(EJBAccessException.class, () -> ajeno.bean.agregar(datos(2L)));
        assertTrue(ajeno.jpql.isEmpty());
        assertEquals(0, ajeno.empresasLeidas);
        assertEquals(0, ajeno.persistidos);
    }

    @Test void administradorGestionaCualquierEmpresaSinVinculos() throws Exception {
        Escenario e = new Escenario(Roles.ADMINISTRADOR);
        e.bean.agregar(datos(2L));
        e.bean.modificar(10L, datos(2L));
        e.bean.darDeBaja(10L);
        assertEquals(2L, e.vehiculo.getEmpresa().getId());
        assertFalse(e.vehiculo.isHabilitado());
        assertTrue(e.empresasAutorizadas.isEmpty());
    }

    @Test void listadoAdministrativoConservaVehiculosDeEmpresaInactiva() throws Exception {
        Escenario e = new Escenario(Roles.ADMINISTRADOR);
        e.empresas.get(1L).setActiva(false);
        assertEquals(1, e.bean.listar(1L).size());
        assertTrue(e.empresasAutorizadas.isEmpty());
    }

    @Test void modificarExigeOrigenYDestinoAntesDeAlterarEntidad() throws Exception {
        Escenario e = new Escenario(Roles.CIUDADANO);
        assertThrows(EJBAccessException.class, () -> e.bean.modificar(10L, datos(2L)));
        assertEquals(List.of(1L, 2L), e.empresasAutorizadas);
        assertEquals(1L, e.vehiculo.getEmpresa().getId());
        assertEquals("ORIGINAL", e.vehiculo.getMatricula());
        assertEquals(0, e.empresasLeidas);
        assertTrue(e.jpql.isEmpty());
    }

    @Test void origenAjenoDeniegaAunqueDestinoSeaPropio() throws Exception {
        Escenario e = new Escenario(Roles.CIUDADANO);
        e.vehiculo.setEmpresa(e.empresas.get(2L));
        assertThrows(EJBAccessException.class, () -> e.bean.modificar(10L, datos(1L)));
        assertEquals(List.of(2L), e.empresasAutorizadas);
        assertEquals(2L, e.vehiculo.getEmpresa().getId());
        assertEquals("ORIGINAL", e.vehiculo.getMatricula());
        assertTrue(e.jpql.isEmpty());
    }

    @Test void trasladoEntreDosEmpresasAutorizadasYModificacionPropiaPermitidos() throws Exception {
        Escenario e = new Escenario(Roles.CIUDADANO);
        e.bean.modificar(10L, datos(1L));
        e.permitidas.add(2L);
        assertEquals(2L, e.bean.modificar(10L, datos(2L)).getEmpresaId());
        assertEquals(2L, e.vehiculo.getEmpresa().getId());
        assertEquals("NUEVA", e.vehiculo.getMatricula());
    }

    @Test void bajaAjenaConservaHabilitacionYBajaPropiaLaCambia() throws Exception {
        Escenario e = new Escenario(Roles.CIUDADANO);
        e.permitidas.clear();
        assertThrows(EJBAccessException.class, () -> e.bean.darDeBaja(10L));
        assertTrue(e.vehiculo.isHabilitado());
        e.permitidas.add(1L);
        e.bean.darDeBaja(10L);
        assertFalse(e.vehiculo.isHabilitado());
    }

    @Test void datosInvalidosContinuanComoErrorDeNegocioSinPersistencia() throws Exception {
        Escenario e = new Escenario(Roles.CIUDADANO);
        assertThrows(NegocioException.class, () -> e.bean.agregar(null));
        VehiculoDTO dto = datos(1L); dto.setMatricula(" ");
        assertThrows(NegocioException.class, () -> e.bean.agregar(dto));
        assertEquals(0, e.persistidos);
    }

    private static VehiculoDTO datos(Long empresa) {
        VehiculoDTO dto = new VehiculoDTO(); dto.setEmpresaId(empresa); dto.setMatricula("nueva");
        dto.setPesoKg(1000); dto.setCapacidadCargaKg(2000); dto.setCapacidadVolumenM3(BigDecimal.TEN);
        dto.setNumeroPermiso("P-PRUEBA"); dto.setPermisoDesde("2026-01-01");
        dto.setPermisoHasta("2027-01-01"); dto.setItvVencimiento("2027-01-01");
        return dto;
    }

    private static void campo(Object destino, String nombre, Object valor) throws Exception {
        var campo = destino.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(destino, valor);
    }

    private static class Escenario {
        final VehiculoServicioBean bean = new VehiculoServicioBean();
        final Vehiculo vehiculo = new Vehiculo();
        final Map<Long, Empresa> empresas = new HashMap<>();
        final Set<Long> permitidas = new HashSet<>(Set.of(1L));
        final List<Long> empresasAutorizadas = new ArrayList<>();
        final List<String> jpql = new ArrayList<>();
        final Map<String, Object> parametros = new HashMap<>();
        Principal principal = () -> "persona-prueba";
        String cedulaConsultada;
        int persistidos, empresasLeidas;

        Escenario(String rol) throws Exception {
            for (Long id : List.of(1L, 2L)) {
                Empresa empresa = new Empresa(); campo(empresa, "id", id); empresas.put(id, empresa);
            }
            campo(vehiculo, "id", 10L); vehiculo.setEmpresa(empresas.get(1L)); vehiculo.setMatricula("ORIGINAL");
            campo(bean, "contexto", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class}, (p, m, a) -> {
                return switch (m.getName()) {
                    case "isCallerInRole" -> rol.equals(a[0]);
                    case "getCallerPrincipal" -> principal;
                    default -> throw new UnsupportedOperationException(m.getName());
                };
            }));
            campo(bean, "vinculos", new VinculoEmpresaUsuarioDAO() {
                @Override public boolean esResponsableActivo(String cedula, Long empresaId) {
                    empresasAutorizadas.add(empresaId); cedulaConsultada = cedula;
                    return permitidas.contains(empresaId);
                }
            });
            campo(bean, "em", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class}, (p, m, a) -> {
                return switch (m.getName()) {
                    case "find" -> {
                        if (a[0] == Empresa.class) { empresasLeidas++; yield empresas.get(a[1]); }
                        yield vehiculo;
                    }
                    case "getReference" -> empresas.get(a[1]);
                    case "createQuery" -> consulta((String) a[0]);
                    case "persist" -> { persistidos++; yield null; }
                    case "flush" -> null;
                    default -> throw new UnsupportedOperationException(m.getName());
                };
            }));
        }

        Object consulta(String consulta) {
            jpql.add(consulta);
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class}, (p, m, a) -> switch (m.getName()) {
                case "setParameter" -> { parametros.put((String) a[0], a[1]); yield p; }
                case "getSingleResult" -> 0L;
                case "getResultList" -> List.of(vehiculo);
                default -> throw new UnsupportedOperationException(m.getName());
            });
        }
    }
}
