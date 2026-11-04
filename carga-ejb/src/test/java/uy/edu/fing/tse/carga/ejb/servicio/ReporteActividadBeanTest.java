package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.reporte.ConteoActividadDTO;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
import uy.edu.fing.tse.carga.api.dto.guia.TipoCargaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.persistencia.ReporteActividadDAO;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba primero. Dobles no prueban roles EJB, SQL ni instantanea de PostgreSQL. */
class ReporteActividadBeanTest {
    @Test void finalizadosUsaConsultaSeparadaYMetadatosSinCambiarCreacion() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        var r = e.bean.viajesFinalizados("2026-11-04", "2026-11-05", "7");
        assertEquals("FIN_VIAJE", r.getCriterioTemporal()); assertEquals(7L, r.getTipoCargaId());
        assertEquals("Granel", r.getTipoCargaNombre());
        assertEquals(Instant.parse("2026-11-04T03:00:00Z"), e.desde);
        assertEquals(Instant.parse("2026-11-06T03:00:00Z"), e.hasta);
        assertEquals(1, e.lecturasFinalizados); assertEquals(0, e.lecturas);
        assertEquals(List.of(0L, 0L), r.getPorDia().stream().map(ConteoActividadDTO::getCantidad).toList());
        assertEquals(0, r.getTotalGuias());
        assertEquals("CREACION_GUIA", e.bean.consultar(null, null, null).getCriterioTemporal());
        assertEquals(1, e.lecturas); assertEquals(1, e.lecturasFinalizados);
    }

    @Test void finalizadosMantieneCalendarioDefectoLimitesYVacio() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        var r = e.bean.viajesFinalizados(null, null, null);
        assertEquals("2026-10-03", r.getDesde()); assertEquals("2026-11-01", r.getHasta());
        assertEquals(30, r.getPorDia().size()); assertEquals(4, r.getPorEstado().size());
        assertEquals("FIN_VIAJE", r.getCriterioTemporal()); assertNull(r.getTipoCargaId());
        e.bean.viajesFinalizados("2010-10-03", "2010-10-03", null);
        assertEquals(Instant.parse("2010-10-03T03:00:00Z"), e.desde);
        assertEquals(Instant.parse("2010-10-04T02:00:00Z"), e.hasta);
        assertEquals(366, e.bean.viajesFinalizados("2024-01-01", "2024-12-31", null).getPorDia().size());
    }

    @Test void finalizadosValidaAntesDeConsultarYNoOcultaFallos() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        assertThrows(NegocioException.class, () -> e.bean.viajesFinalizados("2026-11-04", null, null));
        assertThrows(NegocioException.class, () -> e.bean.viajesFinalizados("2026-11-05", "2026-11-04", null));
        assertThrows(NegocioException.class, () -> e.bean.viajesFinalizados("2026-01-01", "2027-01-02", null));
        assertThrows(NegocioException.class, () -> e.bean.viajesFinalizados(null, null, "0"));
        e.tipoExiste = false;
        assertThrows(NegocioException.class, () -> e.bean.viajesFinalizados(null, null, "7"));
        assertEquals(0, e.lecturasFinalizados); assertEquals(0, e.lecturas);
        e.fallo = new IllegalStateException("fallo sintetico");
        assertSame(e.fallo, assertThrows(IllegalStateException.class, () -> e.bean.viajesFinalizados(null, null, null)));
    }

    @Test void finalizadosNoAbreHistorialNiLecturaAOtrosRoles() throws Exception {
        for (String rol : List.of(Roles.ADMINISTRADOR, Roles.FUNCIONARIO, Roles.CIUDADANO, Roles.CHOFER)) {
            var e = new Escenario(rol);
            assertThrows(EJBAccessException.class, () -> e.bean.viajesFinalizados(null, null, "7"));
            assertEquals(0, e.busquedasTipo); assertEquals(0, e.lecturasFinalizados);
        }
        var e = new Escenario(Roles.AUTORIDAD); e.principal = null;
        assertThrows(EJBAccessException.class, () -> e.bean.viajesFinalizados(null, null, null));
        assertEquals(Set.of(Roles.CHOFER),
                Set.of(EventoViajeBean.class.getAnnotation(RolesAllowed.class).value()));
    }

    @Test void filtroDeCargaSePropagaAntesDeAgregarYDevuelveSuNombreInclusoSinGuias() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        var r = e.bean.consultar("2026-11-01", "2026-11-02", "7");
        assertEquals(7L, e.tipoConsultado); assertEquals(7L, e.tipoFiltrado);
        assertEquals(7L, r.getTipoCargaId()); assertEquals("Granel", r.getTipoCargaNombre());
        assertEquals(0, r.getTotalGuias()); assertEquals(2, r.getPorDia().size());
        assertEquals(1, e.lecturas); assertEquals(1, e.busquedasTipo);
    }

    @Test void firmaAnteriorYFiltroAusenteConservanTodosSinConsultarCatalogo() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        var anterior = e.bean.consultar(null, null);
        assertNull(anterior.getTipoCargaId()); assertNull(anterior.getTipoCargaNombre());
        var actual = e.bean.consultar(null, null, null);
        assertNull(actual.getTipoCargaId()); assertNull(e.tipoFiltrado);
        assertEquals(2, e.lecturas); assertEquals(0, e.busquedasTipo);
    }

    @Test void filtroInvalidoNoConsultaCatalogoNiAgregados() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        for (String id : List.of("", " ", "0", "-1", "+1", "01", "1.0", "7 ", "texto", "9223372036854775808"))
            assertThrows(NegocioException.class, () -> e.bean.consultar(null, null, id));
        assertEquals(0, e.lecturas); assertEquals(0, e.busquedasTipo);
    }

    @Test void filtroInexistenteNoSeIgnoraNiDevuelveTodasLasGuias() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD); e.tipoExiste = false;
        assertThrows(NegocioException.class, () -> e.bean.consultar(null, null, "7"));
        assertEquals(1, e.busquedasTipo); assertEquals(0, e.lecturas);
    }

    @Test void consultaCatalogoCompletoSinExigirGuiasYPropagaFallo() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        assertEquals("Granel", e.bean.tiposCarga().getFirst().getNombre());
        assertEquals(0, e.lecturas); assertEquals(1, e.listadosTipo);
        e.fallo = new IllegalStateException("catalogo sintetico");
        assertSame(e.fallo, assertThrows(IllegalStateException.class, () -> e.bean.tiposCarga()));
    }

    @Test void filtroYCatalogoNoAmplianPermisosNiRevelanExistencia() throws Exception {
        for (var rol : List.of(Roles.ADMINISTRADOR, Roles.FUNCIONARIO, Roles.CIUDADANO, Roles.CHOFER)) {
            var e = new Escenario(rol);
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(null, null, "7"));
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(null, null, "invalido"));
            assertThrows(EJBAccessException.class, () -> e.bean.tiposCarga());
            assertEquals(0, e.busquedasTipo); assertEquals(0, e.listadosTipo); assertEquals(0, e.lecturas);
        }
        var e = new Escenario(Roles.AUTORIDAD); e.principal = null;
        assertThrows(EJBAccessException.class, () -> e.bean.consultar(null, null, "7"));
        assertThrows(EJBAccessException.class, () -> e.bean.tiposCarga());
    }

    @Test void autoridadSinVinculoObtieneAgregadosYMetadatosDelPeriodo() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        e.resultado.setTotalGuias(3); e.resultado.setEmpresasConGuias(2);
        e.resultado.setPorDia(List.of(new ConteoActividadDTO("2026-11-01", "2026-11-01", 3)));
        e.resultado.setPorEstado(List.of(new ConteoActividadDTO("ASIGNADA", "ASIGNADA", 3)));
        e.resultado.setPorRubro(List.of(new ConteoActividadDTO("SIN_RUBRO", "Sin rubro", 3)));
        var r = e.bean.consultar("2026-11-01", "2026-11-02");
        assertEquals(3, r.getTotalGuias()); assertEquals(2, r.getEmpresasConGuias());
        assertEquals("2026-11-01", r.getDesde()); assertEquals("2026-11-02", r.getHasta());
        assertEquals("America/Montevideo", r.getZonaHoraria()); assertEquals("CREACION_GUIA", r.getCriterioTemporal());
        assertEquals(List.of(3L, 0L), r.getPorDia().stream().map(ConteoActividadDTO::getCantidad).toList());
        assertEquals(List.of("CREADA", "ASIGNADA", "EN_CURSO", "FINALIZADA"),
                r.getPorEstado().stream().map(ConteoActividadDTO::getClave).toList());
        assertEquals(List.of(0L, 3L, 0L, 0L), r.getPorEstado().stream().map(ConteoActividadDTO::getCantidad).toList());
        assertEquals("SIN_RUBRO", r.getPorRubro().getFirst().getClave()); assertEquals(1, e.lecturas);
    }

    @Test void medianochesLocalesSeConviertenAInstantesSinUsarInicioPrevisto() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        e.bean.consultar("2026-11-01", "2026-11-01");
        assertEquals(Instant.parse("2026-11-01T03:00:00Z"), e.desde);
        assertEquals(Instant.parse("2026-11-02T03:00:00Z"), e.hasta);
    }

    @Test void diasHistoricosUsanReglasDeZonaYNoOffsetFijo() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        e.bean.consultar("2010-01-15", "2010-01-15");
        assertEquals(Instant.parse("2010-01-15T02:00:00Z"), e.desde);
        assertEquals(Instant.parse("2010-01-16T02:00:00Z"), e.hasta);
    }

    @Test void diaConCambioHorarioNoSeConvierteEnVeinticuatroHorasFijas() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        e.bean.consultar("2010-10-03", "2010-10-03");
        assertEquals(Instant.parse("2010-10-03T03:00:00Z"), e.desde);
        assertEquals(Instant.parse("2010-10-04T02:00:00Z"), e.hasta);
    }

    @Test void defectoSonTreintaDiasIncluidoHoyLocalDelServidor() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        var r = e.bean.consultar(null, null);
        assertEquals("2026-10-03", r.getDesde()); assertEquals("2026-11-01", r.getHasta());
        assertEquals(30, r.getPorDia().size());
        assertEquals(Instant.parse("2026-10-03T03:00:00Z"), e.desde);
        assertEquals(Instant.parse("2026-11-02T03:00:00Z"), e.hasta);
    }

    @Test void vacioTieneCerosRealesYNoInventosDeFiscalizacion() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        var r = e.bean.consultar("2026-11-01", "2026-11-01");
        assertEquals(0, r.getTotalGuias()); assertEquals(0, r.getEmpresasConGuias());
        assertEquals(1, r.getPorDia().size()); assertEquals(0, r.getPorDia().getFirst().getCantidad());
        assertEquals(4, r.getPorEstado().size()); assertTrue(r.getPorEstado().stream().allMatch(x -> x.getCantidad() == 0));
        assertTrue(r.getPorRubro().isEmpty());
    }

    @Test void otrosRolesNoLeenNiValidanExistencia() throws Exception {
        for (var rol : List.of(Roles.ADMINISTRADOR, Roles.FUNCIONARIO, Roles.CIUDADANO, Roles.CHOFER)) {
            var e = new Escenario(rol);
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(null, null));
            assertThrows(EJBAccessException.class, () -> e.bean.consultar("incorrecto", null));
            assertEquals(0, e.lecturas);
        }
    }

    @Test void principalAusenteONombreVacioFallaCerrado() throws Exception {
        for (Principal principal : new Principal[]{null, () -> null, () -> " "}) {
            var e = new Escenario(Roles.AUTORIDAD); e.principal = principal;
            assertThrows(EJBAccessException.class, () -> e.bean.consultar(null, null));
            assertEquals(0, e.lecturas);
        }
        var e = new Escenario(Roles.AUTORIDAD); campo(e.bean, "contexto", null);
        assertThrows(EJBAccessException.class, () -> e.bean.consultar(null, null));
    }

    @Test void fechasEstrictasParCompletoYRangoValidoAntesDeLeer() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        for (String[] par : new String[][]{{null, "2026-11-01"}, {"2026-11-01", null}, {"", ""},
                {"2026-02-30", "2026-03-01"}, {"2026-11-01 ", "2026-11-02"},
                {"2026-11-02", "2026-11-01"}, {"2026-1-01", "2026-01-02"},
                {"0000-01-01", "0000-01-02"}, {"+10000-01-01", "+10000-01-02"},
                {"2026-11-01T00:00:00Z", "2026-11-02"}, {"2026-01-01", "2027-01-02"}})
            assertThrows(NegocioException.class, () -> e.bean.consultar(par[0], par[1]));
        assertEquals(0, e.lecturas);
    }

    @Test void acepta366DiasSinTruncarYCompletaLosBordes() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD);
        var r = e.bean.consultar("2024-01-01", "2024-12-31");
        assertEquals(366, r.getPorDia().size());
        assertEquals("2024-01-01", r.getPorDia().getFirst().getClave());
        assertEquals("2024-12-31", r.getPorDia().getLast().getClave());
        assertEquals(1, e.lecturas);
    }

    @Test void falloDePersistenciaSePropagaSinConvertirloEnCero() throws Exception {
        var e = new Escenario(Roles.AUTORIDAD); e.fallo = new IllegalStateException("fallo sintetico");
        assertSame(e.fallo, assertThrows(IllegalStateException.class, () -> e.bean.consultar(null, null)));
    }

    @Test void permisoDeclarativoNoAmpliaConsultasOperativas() {
        assertEquals(Set.of(Roles.AUTORIDAD), Set.of(ReporteActividadBean.class.getAnnotation(RolesAllowed.class).value()));
        assertEquals(Set.of(Roles.FUNCIONARIO, Roles.ADMINISTRADOR),
                Set.of(ConsultaGuiasBean.class.getAnnotation(RolesAllowed.class).value()));
    }

    private static void campo(Object objeto, String nombre, Object valor) throws Exception {
        var c = objeto.getClass().getDeclaredField(nombre); c.setAccessible(true); c.set(objeto, valor);
    }

    private static final class Escenario {
        final ReporteActividadBean bean = new ReporteActividadBean();
        final ReporteActividadDTO resultado = new ReporteActividadDTO();
        Principal principal = () -> "autoridad-sintetica";
        int lecturas, lecturasFinalizados;
        Instant desde, hasta;
        Long tipoConsultado, tipoFiltrado;
        boolean tipoExiste = true;
        int busquedasTipo, listadosTipo;
        RuntimeException fallo;

        Escenario(String rol) throws Exception {
            campo(bean, "reloj", Clock.fixed(Instant.parse("2026-11-02T01:00:00Z"), ZoneOffset.UTC));
            campo(bean, "contexto", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "getCallerPrincipal" -> principal;
                        case "isCallerInRole" -> rol.equals(a[0]);
                        default -> throw new UnsupportedOperationException(m.getName());
                    }));
            campo(bean, "dao", new ReporteActividadDAO() {
                @Override public ReporteActividadDTO consultar(Instant inicio, Instant fin, Long tipoCargaId) {
                    lecturas++; desde = inicio; hasta = fin; tipoFiltrado = tipoCargaId;
                    if (fallo != null) throw fallo;
                    return resultado;
                }
                @Override public ReporteActividadDTO consultarFinalizados(Instant inicio, Instant fin, Long tipoCargaId) {
                    lecturasFinalizados++; desde = inicio; hasta = fin; tipoFiltrado = tipoCargaId;
                    if (fallo != null) throw fallo;
                    return resultado;
                }
                @Override public Optional<TipoCargaDTO> tipoCarga(Long id) {
                    busquedasTipo++; tipoConsultado = id;
                    return tipoExiste ? Optional.of(tipo()) : Optional.empty();
                }
                @Override public List<TipoCargaDTO> tiposCarga() {
                    listadosTipo++;
                    if (fallo != null) throw fallo;
                    return List.of(tipo());
                }
                private TipoCargaDTO tipo() {
                    var t = new TipoCargaDTO(); t.setId(7L); t.setNombre("Granel"); return t;
                }
            });
        }
    }
}
