package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.vehiculo.ConsultaVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Escritas antes de C13; dobles no ejecutan JPQL, roles del contenedor ni JSON. */
class ConsultaVehiculosBeanTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 22);

    @Test void autorizaFuncionarioYAdministradorSinVinculoInclusoEmpresaInactiva() throws Exception {
        for (String rol : List.of(Roles.FUNCIONARIO, Roles.ADMINISTRADOR)) {
            Escenario e = new Escenario(rol);
            e.empresa.setActiva(false); e.vehiculo.setHabilitado(false);
            var pagina = e.bean.consultar(1L, null, null, null);
            assertEquals(0, pagina.getPagina()); assertEquals(20, pagina.getTamano());
            assertEquals(1, pagina.getTotal()); assertEquals(1, pagina.getRegistros().size());
            var dto = pagina.getRegistros().getFirst();
            assertEquals(10L, dto.getId()); assertEquals(1L, dto.getEmpresaId());
            assertEquals("ABC1234", dto.getMatricula()); assertFalse(dto.isHabilitado());
            assertEquals("P-1", dto.getNumeroPermiso()); assertEquals("VIGENTE", dto.getEstadoPermiso());
            assertEquals("VIGENTE", dto.getEstadoItv());
            assertEquals("2026-10-22", pagina.getFechaReferencia());
            assertEquals("America/Montevideo", pagina.getZonaHoraria());
            assertEquals(0, e.desplazamiento); assertEquals(20, e.limite);
            assertTrue(e.consultas.stream().allMatch(q -> q.contains("v.empresa.id = :empresaId")));
            assertTrue(e.consultas.getLast().endsWith("order by v.matricula, v.id"));
            assertTrue(e.consultas.stream().noneMatch(q -> q.contains("habilitado") || q.contains("activa")));
        }
    }

    @Test void deniegaAntesDeConsultarExistenciaOFlotaInclusoConDatosInvalidos() throws Exception {
        for (String rol : List.of(Roles.CIUDADANO, Roles.CHOFER, Roles.AUTORIDAD)) {
            Escenario e = new Escenario(rol);
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(999L, null, null, null));
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(null, null, -1, 0));
            assertEquals(0, e.lecturasEmpresa); assertTrue(e.consultas.isEmpty());
        }
        Escenario sinPrincipal = new Escenario(Roles.FUNCIONARIO); sinPrincipal.principal = null;
        assertThrows(EJBAccessException.class, () -> sinPrincipal.bean.consultar(1L, null, null, null));
        assertEquals(0, sinPrincipal.lecturasEmpresa);
    }

    @Test void validaEmpresaYPaginacionSinConsultarFlota() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        for (Long id : new Long[]{null, 0L, -1L})
            assertThrows(NegocioException.class, () -> e.bean.consultar(id, null, null, null));
        for (int[] pag : new int[][]{{-1, 20}, {0, 0}, {0, 101}, {Integer.MAX_VALUE, 100}})
            assertThrows(NegocioException.class, () -> e.bean.consultar(1L, null, pag[0], pag[1]));
        e.existe = false;
        assertThrows(NegocioException.class, () -> e.bean.consultar(1L, null, null, null));
        assertTrue(e.consultas.isEmpty());
    }

    @Test void filtroLiteralUsaMismosParametrosEnConteoYPagina() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        e.bean.consultar(1L, " a_%! ", 2, 10);
        assertEquals(20, e.desplazamiento); assertEquals(10, e.limite);
        assertEquals(2, e.parametros.size());
        assertEquals(e.parametros.getFirst(), e.parametros.getLast());
        assertEquals(Map.of("empresaId", 1L, "matricula", "%A!_!%!!%"), e.parametros.getFirst());
        assertTrue(e.consultas.stream().allMatch(q -> q.contains("upper(v.matricula) like :matricula escape '!'")));
    }

    @Test void filtroBlancoNoAgregaCondicionYPaginaVaciaConservaTotal() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO); e.filas = List.of(); e.total = 41;
        var pagina = e.bean.consultar(1L, "  ", 8, 20);
        assertTrue(pagina.getRegistros().isEmpty()); assertEquals(41, pagina.getTotal());
        assertEquals(8, pagina.getPagina()); assertEquals(160, e.desplazamiento);
        assertTrue(e.consultas.stream().noneMatch(q -> q.contains("like")));
    }

    @Test void referenciaNoUsaDiaUtcNiZonaDelNavegador() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        campo(e.bean, "reloj", Clock.fixed(Instant.parse("2026-10-23T01:30:00Z"), ZoneOffset.UTC));
        assertEquals("2026-10-22", e.bean.consultar(1L, null, null, null).getFechaReferencia());
    }

    @Test void permisoIncluyeAmbosLimitesYNoConfundeVigenciaConBaja() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        e.vehiculo.setHabilitado(false);
        e.vehiculo.setPermisoDesde(HOY); e.vehiculo.setPermisoHasta(HOY);
        assertEquals("VIGENTE", consultar(e).getEstadoPermiso());
        assertFalse(consultar(e).isHabilitado());
        e.vehiculo.setPermisoDesde(HOY.plusDays(1)); e.vehiculo.setPermisoHasta(HOY.plusDays(2));
        assertEquals("FUTURO", consultar(e).getEstadoPermiso());
        e.vehiculo.setPermisoDesde(HOY.minusDays(2)); e.vehiculo.setPermisoHasta(HOY.minusDays(1));
        assertEquals("VENCIDO", consultar(e).getEstadoPermiso());
    }

    @Test void permisoIncompletoOInconsistenteEsSinDatosSinCambiarEntidad() throws Exception {
        for (String falta : List.of("numero", "desde", "hasta", "invertido")) {
            Escenario e = new Escenario(Roles.FUNCIONARIO);
            switch (falta) {
                case "numero" -> e.vehiculo.setNumeroPermiso(" ");
                case "desde" -> e.vehiculo.setPermisoDesde(null);
                case "hasta" -> e.vehiculo.setPermisoHasta(null);
                case "invertido" -> e.vehiculo.setPermisoDesde(HOY.plusDays(1));
            }
            assertEquals("SIN_DATOS", consultar(e).getEstadoPermiso(), falta);
            assertTrue(e.vehiculo.isHabilitado());
        }
    }

    @Test void itvIncluyeVencimientoYConservaAusencia() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        assertEquals("VIGENTE", consultar(e).getEstadoItv());
        e.vehiculo.setItvVencimiento(HOY.minusDays(1));
        assertEquals("VENCIDO", consultar(e).getEstadoItv());
        e.vehiculo.setItvVencimiento(null);
        assertEquals("SIN_DATOS", consultar(e).getEstadoItv());
        assertNull(consultar(e).getItvVencimiento());
    }

    @Test void contratosDeRolesSeparadosNoAmplianAbm() {
        assertEquals(Set.of(Roles.FUNCIONARIO, Roles.ADMINISTRADOR),
                Set.of(ConsultaVehiculosBean.class.getAnnotation(RolesAllowed.class).value()));
        assertEquals(Set.of(Roles.ADMINISTRADOR, Roles.CIUDADANO, Roles.CHOFER),
                Set.of(VehiculoServicioBean.class.getAnnotation(RolesAllowed.class).value()));
    }

    private static ConsultaVehiculoDTO consultar(Escenario e) throws NegocioException {
        return e.bean.consultar(1L, null, null, null).getRegistros().getFirst();
    }

    private static void campo(Object destino, String nombre, Object valor) throws Exception {
        var c = destino.getClass().getDeclaredField(nombre); c.setAccessible(true); c.set(destino, valor);
    }

    private static final class Escenario {
        final ConsultaVehiculosBean bean = new ConsultaVehiculosBean();
        final Empresa empresa = new Empresa();
        final Vehiculo vehiculo = new Vehiculo();
        final List<String> consultas = new ArrayList<>();
        final List<Map<String, Object>> parametros = new ArrayList<>();
        List<Vehiculo> filas = List.of(vehiculo);
        Principal principal = () -> "funcionario-sintetico";
        boolean existe = true;
        long total = 1;
        int lecturasEmpresa, desplazamiento, limite;

        Escenario(String rol) throws Exception {
            campo(empresa, "id", 1L); campo(vehiculo, "id", 10L);
            vehiculo.setEmpresa(empresa); vehiculo.setMatricula("ABC1234"); vehiculo.setNumeroPermiso("P-1");
            vehiculo.setPermisoDesde(HOY.minusDays(1)); vehiculo.setPermisoHasta(HOY);
            vehiculo.setItvVencimiento(HOY);
            campo(bean, "reloj", Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), ZoneOffset.UTC));
            campo(bean, "contexto", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "getCallerPrincipal" -> principal;
                        case "isCallerInRole" -> rol.equals(a[0]);
                        default -> throw new UnsupportedOperationException(m.getName());
                    }));
            campo(bean, "em", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "find" -> { assertEquals(Empresa.class, a[0]); lecturasEmpresa++; yield existe ? empresa : null; }
                        case "createQuery" -> consulta((String) a[0]);
                        default -> throw new UnsupportedOperationException(m.getName()); // falla si escribe o bloquea
                    }));
        }

        Object consulta(String jpql) {
            consultas.add(jpql); Map<String, Object> params = new HashMap<>(); parametros.add(params);
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class}, (p, m, a) -> switch (m.getName()) {
                case "setParameter" -> { params.put((String) a[0], a[1]); yield p; }
                case "setFirstResult" -> { desplazamiento = (int) a[0]; yield p; }
                case "setMaxResults" -> { limite = (int) a[0]; yield p; }
                case "getSingleResult" -> total;
                case "getResultList" -> filas;
                default -> throw new UnsupportedOperationException(m.getName());
            });
        }
    }
}
