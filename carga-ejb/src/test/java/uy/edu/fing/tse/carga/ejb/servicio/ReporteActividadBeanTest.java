package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.reporte.ConteoActividadDTO;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
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

import static org.junit.jupiter.api.Assertions.*;

/** Prueba primero. Dobles no prueban roles EJB, SQL ni instantanea de PostgreSQL. */
class ReporteActividadBeanTest {
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
        int lecturas;
        Instant desde, hasta;
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
                @Override public ReporteActividadDTO consultar(Instant inicio, Instant fin) {
                    lecturas++; desde = inicio; hasta = fin;
                    if (fallo != null) throw fallo;
                    return resultado;
                }
            });
        }
    }
}
