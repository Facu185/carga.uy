package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Guia;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Escritas antes de C14. Los dobles no ejecutan JPQL, JSON ni roles del contenedor. */
class ConsultaGuiasBeanTest {
    @Test void consultaSinVinculoConEmpresaInactivaYAsignacionesDadasDeBaja() throws Exception {
        for (String rol : List.of(Roles.FUNCIONARIO, Roles.ADMINISTRADOR)) {
            Escenario e = new Escenario(rol);
            e.empresa.setActiva(false); e.vehiculo.setHabilitado(false); e.chofer.setActivo(false);
            var pagina = e.bean.consultar(1L, null, null, null, null, null);
            assertEquals(0, pagina.getPagina()); assertEquals(20, pagina.getTamano());
            assertEquals(1, pagina.getTotal()); assertEquals(1, pagina.getRegistros().size());
            var dto = pagina.getRegistros().getFirst();
            assertEquals("Empresa sintetica", dto.getNombreEmpresa());
            assertEquals(10L, dto.getGuia().getId()); assertEquals(1L, dto.getGuia().getEmpresaId());
            assertEquals("ABC1234", dto.getMatricula()); assertEquals("Chofer sintetico", dto.getNombreChofer());
            assertEquals(2L, dto.getGuia().getVehiculoId()); assertEquals(3L, dto.getGuia().getChoferId());
            assertEquals("Origen", dto.getGuia().getOrigen()); assertEquals("Destino", dto.getGuia().getDestino());
            assertEquals(new BigDecimal("100.00"), dto.getGuia().getPesoKg());
            assertEquals(new BigDecimal("2.00"), dto.getGuia().getVolumenM3());
            assertEquals(0, e.desplazamiento); assertEquals(20, e.limite);
            assertTrue(e.consultas.stream().noneMatch(q -> q.contains("Vinculo") || q.contains("activo") || q.contains("habilitado")));
        }
    }

    @Test void deniegaAmbasOperacionesAntesDeExistenciaInclusoConParametrosInvalidos() throws Exception {
        for (String rol : List.of(Roles.CIUDADANO, Roles.CHOFER, Roles.AUTORIDAD)) {
            Escenario e = new Escenario(rol);
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(999L, null, null, null, null, null));
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(null, "invalido", null, "X", -1, 0));
            assertThrows(EJBAccessException.class, () -> e.bean.detalle(999L, 999L));
            assertThrows(EJBAccessException.class, () -> e.bean.detalle(null, null));
            assertEquals(0, e.lecturasEmpresa); assertTrue(e.consultas.isEmpty());
        }
    }

    @Test void sinPrincipalONombreVacioNoConsultaDatos() throws Exception {
        for (Principal principal : new Principal[]{null, () -> null, () -> "  "}) {
            Escenario e = new Escenario(Roles.FUNCIONARIO); e.principal = principal;
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(1L, null, null, null, null, null));
            assertThrows(EJBAccessException.class, () -> e.bean.detalle(1L, 10L));
            assertEquals(0, e.lecturasEmpresa); assertTrue(e.consultas.isEmpty());
        }
    }

    @Test void validaEmpresaYPaginacionAntesDeConsultarExistencia() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        for (Long id : new Long[]{null, 0L, -1L})
            assertThrows(NegocioException.class, () -> e.bean.consultar(id, null, null, null, null, null));
        for (int[] pag : new int[][]{{-1, 20}, {0, 0}, {0, 101}, {Integer.MAX_VALUE, 100}})
            assertThrows(NegocioException.class, () -> e.bean.consultar(1L, null, null, null, pag[0], pag[1]));
        assertEquals(0, e.lecturasEmpresa); assertTrue(e.consultas.isEmpty());
    }

    @Test void empresaInexistenteEsNegocioSinConsultarGuias() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO); e.existe = false;
        assertThrows(NegocioException.class, () -> e.bean.consultar(1L, null, null, null, null, null));
        assertThrows(NegocioException.class, () -> e.bean.detalle(1L, 10L));
        assertTrue(e.consultas.isEmpty());
    }

    @Test void limitesDePaginacionValidosYVacioConservanTotal() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO); e.filas = List.of(); e.total = 41;
        var pagina = e.bean.consultar(1L, null, null, null, 8, 20);
        assertTrue(pagina.getRegistros().isEmpty()); assertEquals(41, pagina.getTotal());
        assertEquals(8, pagina.getPagina()); assertEquals(160, e.desplazamiento);
        e.bean.consultar(1L, null, null, null, Integer.MAX_VALUE, 1);
        assertEquals(Integer.MAX_VALUE, e.desplazamiento);
        e.bean.consultar(1L, null, null, null, 0, 100); assertEquals(100, e.limite);
    }

    @Test void periodoExigeParConZonaYOrdenEstricto() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        String valido = "2026-10-27T12:00:00Z";
        for (String[] periodo : new String[][]{{valido, null}, {null, valido}, {"", valido}, {valido, ""},
                {"2026-10-27T10:00:00", valido}, {"2026-02-30T10:00:00Z", valido},
                {valido, valido}, {valido, "2026-10-27T09:00:00-03:00"},
                {valido, "2026-10-26T12:00:00Z"}})
            assertThrows(NegocioException.class, () -> e.bean.consultar(1L, periodo[0], periodo[1], null, null, null));
        assertEquals(0, e.lecturasEmpresa); assertTrue(e.consultas.isEmpty());
    }

    @Test void filtrosParametrizadosIgualesConSolapamientoSemiabiertoYOrdenEstable() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        e.bean.consultar(1L, "2026-10-27T09:00:00-03:00", "2026-10-28T12:00:00Z", " EN_CURSO ", 2, 10);
        assertEquals(20, e.desplazamiento); assertEquals(10, e.limite);
        assertEquals(2, e.parametros.size());
        assertEquals(e.parametros.getFirst(), e.parametros.getLast());
        assertEquals(Map.of("empresaId", 1L, "desde", Instant.parse("2026-10-27T12:00:00Z"),
                "hasta", Instant.parse("2026-10-28T12:00:00Z"), "estado", "EN_CURSO"), e.parametros.getFirst());
        assertTrue(e.consultas.stream().allMatch(q -> q.contains("g.empresa.id = :empresaId")
                && q.contains("g.inicio < :hasta and g.fin > :desde") && q.contains("g.estado = :estado")));
        assertTrue(e.consultas.getLast().contains("left join fetch g.chofer left join fetch g.vehiculo"));
        assertTrue(e.consultas.getLast().endsWith("order by g.inicio desc, g.id desc"));
        assertTrue(e.consultas.stream().noneMatch(q -> q.contains("g.fecha")));
    }

    @Test void filtroEstadoSoloAdmiteValoresPersistidosYBlancoNoFiltra() throws Exception {
        for (String estado : List.of("CREADA", "ASIGNADA", "EN_CURSO", "FINALIZADA")) {
            Escenario e = new Escenario(Roles.FUNCIONARIO);
            e.bean.consultar(1L, null, null, estado, null, null);
            assertEquals(estado, e.parametros.getFirst().get("estado"));
            assertFalse(e.parametros.getFirst().containsKey("desde"));
        }
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        assertThrows(NegocioException.class, () -> e.bean.consultar(1L, null, null, "FINALIZADA' OR 1=1", null, null));
        assertThrows(NegocioException.class, () -> e.bean.consultar(1L, null, null, "en_curso", null, null));
        assertEquals(0, e.lecturasEmpresa);
        e.bean.consultar(1L, null, null, "  ", null, null);
        assertEquals(Map.of("empresaId", 1L), e.parametros.getFirst());
        assertTrue(e.consultas.stream().noneMatch(q -> q.contains("g.estado") || q.contains(":desde")));
    }

    @Test void historicoSinAsignacionesOVolumenConservaEstadoYCeroSinMutar() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        campo(e.guia, "vehiculo", null); campo(e.guia, "chofer", null); campo(e.guia, "volumenM3", null);
        campo(e.guia, "pesoKg", BigDecimal.ZERO); campo(e.guia, "estado", "CREADA");
        var listado = e.bean.consultar(1L, null, null, null, null, null).getRegistros().getFirst();
        var detalle = e.bean.detalle(1L, 10L);
        for (var dto : List.of(listado, detalle)) {
            assertNull(dto.getMatricula()); assertNull(dto.getNombreChofer());
            assertNull(dto.getGuia().getVehiculoId()); assertNull(dto.getGuia().getChoferId());
            assertNull(dto.getGuia().getVolumenM3()); assertEquals(BigDecimal.ZERO, dto.getGuia().getPesoKg());
            assertEquals("CREADA", dto.getGuia().getEstado());
        }
        assertEquals("CREADA", e.guia.getEstado());
    }

    @Test void fechasPasadasNoInventanFinalizacionNiHistoriaReal() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        assertEquals("ASIGNADA", e.bean.detalle(1L, 10L).getGuia().getEstado());
        assertEquals("2020-01-01T15:00:00Z", e.bean.detalle(1L, 10L).getGuia().getFin());
        assertEquals("ASIGNADA", e.guia.getEstado());
    }

    @Test void detalleSeRestringePorEmpresaYGuiaSinNecesitarListadoPrevio() throws Exception {
        for (String rol : List.of(Roles.FUNCIONARIO, Roles.ADMINISTRADOR)) {
            Escenario e = new Escenario(rol);
            var dto = e.bean.detalle(1L, 10L);
            assertEquals(10L, dto.getGuia().getId()); assertEquals("Empresa sintetica", dto.getNombreEmpresa());
            assertEquals(Map.of("empresaId", 1L, "guiaId", 10L), e.parametros.getFirst());
            assertTrue(e.consultas.getFirst().contains("g.empresa.id = :empresaId and g.id = :guiaId"));
            assertTrue(e.consultas.getFirst().contains("left join fetch g.chofer left join fetch g.vehiculo"));
            assertEquals(1, e.limite);
        }
    }

    @Test void detalleInexistenteODeOtraEmpresaEsNegocioSinEntregarDatos() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO); e.filas = List.of();
        assertThrows(NegocioException.class, () -> e.bean.detalle(1L, 999L));
        assertEquals(Map.of("empresaId", 1L, "guiaId", 999L), e.parametros.getFirst());
    }

    @Test void detalleValidaAmbosIdsAntesDeExistencia() throws Exception {
        Escenario e = new Escenario(Roles.FUNCIONARIO);
        for (Long id : new Long[]{null, 0L, -1L}) {
            assertThrows(NegocioException.class, () -> e.bean.detalle(id, 10L));
            assertThrows(NegocioException.class, () -> e.bean.detalle(1L, id));
        }
        assertEquals(0, e.lecturasEmpresa); assertTrue(e.consultas.isEmpty());
    }

    @Test void rolesDeNuevaLecturaNoAmplianCreacionNiListadoEmpresarial() throws Exception {
        assertEquals(Set.of(Roles.FUNCIONARIO, Roles.ADMINISTRADOR),
                Set.of(ConsultaGuiasBean.class.getAnnotation(RolesAllowed.class).value()));
        assertEquals(Set.of(Roles.CIUDADANO), Set.of(GuiaServicioBean.class.getAnnotation(RolesAllowed.class).value()));
        assertEquals(Set.of(Roles.CIUDADANO, Roles.CHOFER), Set.of(GuiaServicioBean.class
                .getMethod("guiasDeLaEmpresaResponsable", Long.class, int.class, int.class).getAnnotation(RolesAllowed.class).value()));
    }

    private static void campo(Object destino, String nombre, Object valor) throws Exception {
        var c = destino.getClass().getDeclaredField(nombre); c.setAccessible(true); c.set(destino, valor);
    }

    private static final class Escenario {
        final ConsultaGuiasBean bean = new ConsultaGuiasBean();
        final Empresa empresa = new Empresa();
        final Vehiculo vehiculo = new Vehiculo();
        final Chofer chofer = new Chofer();
        final Guia guia;
        final List<String> consultas = new ArrayList<>();
        final List<Map<String, Object>> parametros = new ArrayList<>();
        List<Guia> filas;
        Principal principal = () -> "funcionario-sintetico";
        boolean existe = true;
        long total = 1;
        int lecturasEmpresa, desplazamiento, limite;

        Escenario(String rol) throws Exception {
            campo(empresa, "id", 1L); empresa.setNombre("Empresa sintetica");
            campo(vehiculo, "id", 2L); vehiculo.setEmpresa(empresa); vehiculo.setMatricula("ABC1234");
            campo(chofer, "id", 3L); chofer.setEmpresa(empresa); chofer.setNombre("Chofer sintetico");
            guia = new Guia(empresa, vehiculo, chofer, null, null, new BigDecimal("100.00"), new BigDecimal("2.00"),
                    Instant.parse("2020-01-01T12:00:00Z"), Instant.parse("2020-01-01T15:00:00Z"),
                    "Origen", null, null, "Destino", null, null);
            campo(guia, "id", 10L); filas = List.of(guia);
            campo(bean, "contexto", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "getCallerPrincipal" -> principal;
                        case "isCallerInRole" -> rol.equals(a[0]);
                        default -> throw new UnsupportedOperationException(m.getName());
                    }));
            campo(bean, "em", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "find" -> { assertEquals(Empresa.class, a[0]); assertEquals(1L, a[1]); lecturasEmpresa++; yield existe ? empresa : null; }
                        case "createQuery" -> consulta((String) a[0]);
                        default -> throw new UnsupportedOperationException(m.getName()); // falla ante escrituras o locks
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
