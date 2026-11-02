package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Mapeo y delegacion; el doble NO ejecuta SQL ni acredita instantaneas reales. */
class ReporteActividadDAOTest {
    @Test void consultaUnaSolaSentenciaParametrizadaYMapeaGruposSinDetalle() throws Exception {
        var e = new Escenario();
        e.filas = List.of(new Object[]{"TOTAL", "", "", 3L, 2L},
                new Object[]{"DIA", "2026-11-01", "2026-11-01", 3L, 0L},
                new Object[]{"ESTADO", "CREADA", "CREADA", 3L, 0L},
                new Object[]{"RUBRO", "SIN_RUBRO", "Sin rubro", 1L, 0L},
                new Object[]{"RUBRO", "1", "Sin rubro", 2L, 0L});
        var desde = Instant.parse("2026-11-01T03:00:00Z");
        var hasta = Instant.parse("2026-11-02T03:00:00Z");
        var r = e.dao.consultar(desde, hasta);
        assertEquals(1, e.sentencias); assertEquals(1, e.lecturas);
        assertEquals(Map.of(1, desde.atOffset(ZoneOffset.UTC), 2, hasta.atOffset(ZoneOffset.UTC)), e.parametros);
        assertEquals(3, r.getTotalGuias()); assertEquals(2, r.getEmpresasConGuias());
        assertEquals(3, r.getPorDia().getFirst().getCantidad());
        assertEquals("CREADA", r.getPorEstado().getFirst().getClave());
        assertEquals(List.of("1", "SIN_RUBRO"), r.getPorRubro().stream().map(x -> x.getClave()).toList());
        assertTrue(e.sql.contains("g.creada_en >= ?1 AND g.creada_en < ?2"));
        assertTrue(e.sql.contains("AT TIME ZONE 'America/Montevideo'"));
        assertTrue(e.sql.contains("to_char(dia, 'YYYY-MM-DD')"));
        assertTrue(e.sql.contains("COUNT(DISTINCT empresa_id)"));
        assertFalse(e.sql.contains("g.inicio")); assertFalse(e.sql.contains("g.fecha"));
        assertFalse(e.sql.contains("JOIN empresa")); assertFalse(e.sql.contains("vehiculo"));
    }

    @Test void resultadoVacioExplicitoEsCeroConArreglosVacios() throws Exception {
        var e = new Escenario();
        e.filas = java.util.Collections.singletonList(new Object[]{"TOTAL", "", "", 0L, 0L});
        var r = e.dao.consultar(Instant.EPOCH, Instant.EPOCH.plusSeconds(86400));
        assertEquals(0, r.getTotalGuias()); assertEquals(0, r.getEmpresasConGuias());
        assertTrue(r.getPorDia().isEmpty()); assertTrue(r.getPorEstado().isEmpty()); assertTrue(r.getPorRubro().isEmpty());
    }

    @Test void rubrosConMismaCantidadTienenDesempateEstable() throws Exception {
        var e = new Escenario();
        e.filas = List.of(new Object[]{"TOTAL", "", "", 2L, 1L},
                new Object[]{"RUBRO", "2", "B", 1L, 0L}, new Object[]{"RUBRO", "1", "A", 1L, 0L});
        assertEquals(List.of("1", "2"), e.dao.consultar(Instant.EPOCH, Instant.EPOCH.plusSeconds(86400))
                .getPorRubro().stream().map(x -> x.getClave()).toList());
    }

    private static final class Escenario {
        final ReporteActividadDAO dao = new ReporteActividadDAO();
        final Map<Integer, Object> parametros = new HashMap<>();
        List<Object[]> filas = List.of();
        String sql;
        int sentencias, lecturas;

        Escenario() throws Exception {
            var em = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class}, (p, m, a) -> {
                if (!m.getName().equals("createNativeQuery")) throw new UnsupportedOperationException(m.getName());
                sentencias++; sql = (String) a[0];
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Query.class}, (q, metodo, args) ->
                        switch (metodo.getName()) {
                            case "setParameter" -> { parametros.put((int) args[0], args[1]); yield q; }
                            case "getResultList" -> { lecturas++; yield filas; }
                            default -> throw new UnsupportedOperationException(metodo.getName());
                        });
            });
            var c = ReporteActividadDAO.class.getDeclaredField("em"); c.setAccessible(true); c.set(dao, em);
        }
    }
}
