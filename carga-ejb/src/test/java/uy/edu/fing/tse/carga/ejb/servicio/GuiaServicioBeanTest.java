package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.guia.CrearGuiaCommand;
import uy.edu.fing.tse.carga.api.dto.guia.GuiaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias del caso de uso: el proxy emula solo el subconjunto JPA que este bean consulta. */
class GuiaServicioBeanTest {
    @Test void rechazaResponsableSinEmpresa() {
        Escenario e = new Escenario();
        e.usuario = new Usuario("responsable", "Responsable", null);
        NegocioException ex = assertThrows(NegocioException.class, () -> e.bean().crear(e.comando()));
        assertEquals("El usuario autenticado no esta vinculado a una empresa.", ex.getMessage());
    }

    @Test void rechazaVehiculoDeOtraEmpresa() {
        Escenario e = new Escenario();
        e.vehiculo.setEmpresa(e.otraEmpresa);
        assertFalla(e, e.comando(), "deben pertenecer a la empresa responsable");
    }

    @Test void rechazaChoferDeOtraEmpresa() {
        Escenario e = new Escenario();
        e.chofer.setEmpresa(e.otraEmpresa);
        assertFalla(e, e.comando(), "deben pertenecer a la empresa responsable");
    }

    @Test void rechazaPermisoOItvNoVigentes() {
        Escenario e = new Escenario();
        e.vehiculo.setItvVencimiento(LocalDate.parse("2026-10-05"));
        assertFalla(e, e.comando(), "no estan vigentes");
    }

    @Test void evaluaElFinConPrecisionNanosegundoEnElBordeUtc() {
        Escenario e = new Escenario();
        e.vehiculo.setPermisoHasta(LocalDate.parse("2026-10-06"));
        e.vehiculo.setItvVencimiento(LocalDate.parse("2026-10-06"));
        CrearGuiaCommand comando = e.comando();
        comando.setFin("2026-10-07T00:00:00.000000001Z");
        assertFalla(e, comando, "no estan vigentes");
    }

    @Test void rechazaConflictoPorVehiculo() {
        Escenario e = new Escenario(); e.conflictosVehiculo = 1L;
        assertFalla(e, e.comando(), "ya tiene una guia asignada");
    }

    @Test void rechazaConflictoPorChofer() {
        Escenario e = new Escenario(); e.conflictosChofer = 1L;
        assertFalla(e, e.comando(), "ya tiene una guia asignada");
    }

    @Test void bloqueaVehiculoYChoferAntesDeConsultarConflictos() throws Exception {
        Escenario e = new Escenario();
        e.bean().crear(e.comando());
        assertEquals(List.of("Vehiculo:20", "Chofer:10"), e.bloqueos);
        assertTrue(e.indiceConsultaConflicto > e.indiceUltimoBloqueo);
    }

    @Test void misGuiasSoloDevuelveLasDelChoferDelPrincipal() throws Exception {
        Escenario e = new Escenario();
        e.sesion = Falsos.sesion("chofer-cedula");
        e.chofer.setCedula("chofer-cedula");
        Guia propia = guia(e.empresa, e.vehiculo, e.chofer);
        e.guias = List.of(propia);
        List<GuiaDTO> resultado = e.bean().guiasDelChoferAutenticado();
        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.getFirst().getChoferId());
        assertTrue(e.ultimaConsulta.contains("g.chofer.id = :choferId"));
        assertEquals(10L, e.parametros.get("choferId"));
    }

    private static void assertFalla(Escenario e, CrearGuiaCommand comando, String esperado) {
        NegocioException ex = assertThrows(NegocioException.class, () -> e.bean().crear(comando));
        assertTrue(ex.getMessage().contains(esperado), ex::getMessage);
    }

    private static Guia guia(Empresa empresa, Vehiculo vehiculo, Chofer chofer) throws Exception {
        Guia guia = new Guia(empresa, vehiculo, chofer, conId(new Rubro(), 30L), null,
                BigDecimal.ONE, BigDecimal.ONE, java.time.Instant.parse("2026-10-06T10:00:00Z"),
                java.time.Instant.parse("2026-10-06T11:00:00Z"), "A", null, null, "B", null, null);
        return conId(guia, 100L);
    }

    private static <T> T conId(T entidad, Long id) throws Exception { campo(entidad, "id").set(entidad, id); return entidad; }
    private static Field campo(Object entidad, String nombre) throws Exception { Field f = entidad.getClass().getDeclaredField(nombre); f.setAccessible(true); return f; }

    private static final class Escenario {
        final Empresa empresa = conIdSeguro(empresa(1L), 1L);
        final Empresa otraEmpresa = conIdSeguro(empresa(2L), 2L);
        final Vehiculo vehiculo = vehiculo();
        final Chofer chofer = chofer();
        Usuario usuario = usuarioConEmpresa();
        SessionContext sesion = Falsos.sesion("responsable");
        long conflictosVehiculo;
        long conflictosChofer;
        List<Guia> guias = List.of();
        String ultimaConsulta;
        Map<String, Object> parametros = new HashMap<>();
        List<String> bloqueos = new ArrayList<>();
        int indiceUltimoBloqueo;
        int indiceConsultaConflicto;
        int secuencia;

        GuiaServicioBean bean() { GuiaServicioBean b = new GuiaServicioBean(); b.contexto = sesion; b.em = em(); return b; }
        CrearGuiaCommand comando() {
            CrearGuiaCommand c = new CrearGuiaCommand(); c.setVehiculoId(20L); c.setChoferId(10L); c.setRubroId(30L);
            c.setPesoKg(BigDecimal.ONE); c.setVolumenM3(BigDecimal.ONE); c.setInicio("2026-10-06T10:00:00Z"); c.setFin("2026-10-06T11:00:00Z");
            c.setOrigen("Origen"); c.setDestino("Destino"); return c;
        }
        private EntityManager em() {
            return (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class}, (p, m, a) -> {
                if (m.getName().equals("find")) {
                    Class<?> tipo = (Class<?>) a[0]; Long id = (Long) a[1];
                    if (a.length == 3 && a[2] == LockModeType.PESSIMISTIC_WRITE) {
                        bloqueos.add(tipo.getSimpleName() + ":" + id);
                        indiceUltimoBloqueo = ++secuencia;
                    }
                    if (tipo == Vehiculo.class && id.equals(20L)) return vehiculo;
                    if (tipo == Chofer.class && id.equals(10L)) return chofer;
                    if (tipo == Rubro.class && id.equals(30L)) return conIdSeguro(new Rubro(), 30L);
                    return null;
                }
                if (m.getName().equals("createQuery")) return consulta((String) a[0]);
                if (m.getName().equals("persist") || m.getName().equals("flush")) return null;
                throw new UnsupportedOperationException(m.getName());
            });
        }
        private TypedQuery<?> consulta(String jpql) {
            ultimaConsulta = jpql;
            if (jpql.startsWith("select count(g)")) indiceConsultaConflicto = ++secuencia;
            List<?> resultados = jpql.contains("from Usuario") ? (usuario == null ? List.of() : List.of(usuario))
                    : jpql.contains("from Chofer c where") ? List.of(chofer) : jpql.contains("from Guia g") ? guias : List.of();
            return (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class}, (p, m, a) -> switch (m.getName()) {
                case "setParameter" -> { parametros.put((String) a[0], a[1]); yield p; }
                case "getResultStream" -> resultados.stream();
                case "getResultList" -> resultados;
                case "getSingleResult" -> conflictosVehiculo + conflictosChofer;
                default -> throw new UnsupportedOperationException(m.getName());
            });
        }
        private Empresa empresa(long id) { Empresa e = new Empresa(); e.setNumeroEmpresa(id); return e; }
        private Usuario usuarioConEmpresa() { Usuario u = new Usuario("responsable", "Responsable", null); try { campo(u, "empresa").set(u, empresa); } catch (Exception x) { throw new AssertionError(x); } return u; }
        private Vehiculo vehiculo() { Vehiculo v = conIdSeguro(new Vehiculo(), 20L); v.setEmpresa(empresa); v.setCapacidadCargaKg(100); v.setCapacidadVolumenM3(BigDecimal.TEN); v.setPermisoDesde(LocalDate.parse("2026-10-01")); v.setPermisoHasta(LocalDate.parse("2026-10-10")); v.setItvVencimiento(LocalDate.parse("2026-10-10")); return v; }
        private Chofer chofer() { Chofer c = conIdSeguro(new Chofer(), 10L); c.setEmpresa(empresa); c.setCedula("otro"); return c; }
        private static <T> T conIdSeguro(T e, Long id) { try { return conId(e, id); } catch (Exception x) { throw new AssertionError(x); } }
    }
}
