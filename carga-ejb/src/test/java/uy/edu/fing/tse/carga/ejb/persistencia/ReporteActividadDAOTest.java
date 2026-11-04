package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.ejb.modelo.TipoCarga;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Mapeo y delegacion; el doble NO ejecuta SQL ni acredita instantaneas reales. */
class ReporteActividadDAOTest {
    @Test void finalizadosAgregaUnaSentenciaPorInstanteFinYAplicaCargaAntesDeDistribuir() throws Exception {
        var e = new Escenario();
        e.filas = java.util.Collections.singletonList(new Object[]{"TOTAL", "", "", 0L, 0L});
        e.dao.consultarFinalizados(Instant.EPOCH, Instant.EPOCH.plusSeconds(86400), null);
        assertEquals(1, e.sentencias); assertEquals(1, e.lecturas);
        assertEquals(2, e.parametros.size());
        assertTrue(e.sql.contains("tipo = 'FIN'")); assertTrue(e.sql.contains("MIN(instante)"));
        assertTrue(e.sql.contains("GROUP BY guia_id"));
        assertTrue(e.sql.contains("f.fin_real >= ?1 AND f.fin_real < ?2"));
        assertTrue(e.sql.contains("AT TIME ZONE 'America/Montevideo'"));
        assertFalse(e.sql.contains("g.creada_en")); assertFalse(e.sql.contains("recibido_en"));
        assertFalse(e.sql.contains("g.fin")); assertFalse(e.sql.contains("g.estado ="));
        assertFalse(e.sql.contains("tipo_carga_id ="));
        e.dao.consultarFinalizados(Instant.EPOCH, Instant.EPOCH.plusSeconds(86400), 7L);
        assertEquals(7L, e.parametros.get(3));
        assertTrue(e.sql.indexOf("g.tipo_carga_id = ?3") < e.sql.indexOf("SELECT 'TOTAL'"));
    }

    @Test void catalogoMapeaDtoOrdenadoYTipoAusenteEsExplicito() throws Exception {
        var e = new Escenario();
        var tipo = new TipoCarga();
        var id = TipoCarga.class.getDeclaredField("id"); id.setAccessible(true); id.set(tipo, 7L);
        var nombre = TipoCarga.class.getDeclaredField("nombre"); nombre.setAccessible(true); nombre.set(tipo, "Granel");
        e.tipos = List.of(tipo);
        var catalogo = e.dao.tiposCarga();
        assertEquals(7L, catalogo.getFirst().getId()); assertEquals("Granel", catalogo.getFirst().getNombre());
        assertTrue(e.jpql.endsWith("order by t.nombre, t.id"));
        assertEquals("Granel", e.dao.tipoCarga(7L).orElseThrow().getNombre());
        assertTrue(e.dao.tipoCarga(99L).isEmpty());
        e.tipos = List.of(); assertTrue(e.dao.tiposCarga().isEmpty());
        assertEquals(0, e.sentencias); assertEquals(0, e.lecturas);
    }

    @Test void filtraPorCargaConParametroAntesDeTodasLasDistribuciones() throws Exception {
        var e = new Escenario();
        e.filas = java.util.Collections.singletonList(new Object[]{"TOTAL", "", "", 0L, 0L});
        e.dao.consultar(Instant.EPOCH, Instant.EPOCH.plusSeconds(86400), 7L);
        assertEquals(1, e.sentencias); assertEquals(1, e.lecturas);
        assertEquals(7L, e.parametros.get(3));
        assertTrue(e.sql.contains("g.tipo_carga_id = ?3"));
        assertTrue(e.sql.indexOf("g.tipo_carga_id = ?3") < e.sql.indexOf("SELECT 'TOTAL'"));
        assertFalse(e.sql.contains("JOIN vehiculo"));
    }

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
        assertFalse(e.sql.contains("tipo_carga_id ="));
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
        List<TipoCarga> tipos = List.of();
        String sql, jpql;
        int sentencias, lecturas;

        Escenario() throws Exception {
            var em = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class}, (p, m, a) -> {
                if (m.getName().equals("find")) {
                    assertEquals(TipoCarga.class, a[0]);
                    return tipos.stream().filter(t -> t.getId().equals(a[1])).findFirst().orElse(null);
                }
                if (m.getName().equals("createQuery")) {
                    jpql = (String) a[0]; assertEquals(TipoCarga.class, a[1]);
                    return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class}, (q, metodo, args) -> {
                        assertEquals("getResultList", metodo.getName()); return tipos;
                    });
                }
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
