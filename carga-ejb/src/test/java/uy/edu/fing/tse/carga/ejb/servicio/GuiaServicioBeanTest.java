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
        e.empresaVinculada = null;
        assertFalla(e, e.comando(), "El usuario autenticado no esta vinculado a una empresa.");
    }

    @Test void rechazaVehiculoDeOtraEmpresa() {
        Escenario e = new Escenario();
        e.vehiculo.setEmpresa(e.otraEmpresa);
        assertFalla(e, e.comando(), "no esta vinculado a una empresa");
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
        assertTrue(e.consultaChofer.contains("c.cedula = :cedula"));
        assertEquals("chofer-cedula", e.parametros.get("cedula"));
    }

    @Test void creaUnaGuiaAsignadaConDatosDeclarados() throws Exception {
        Escenario e = new Escenario();
        CrearGuiaCommand c = e.comando();
        c.setOrigen("  Origen  "); c.setDestino("  Destino  ");
        c.setOrigenLat(-34.9); c.setOrigenLon(-56.2);
        GuiaDTO dto = e.bean().crear(c);
        assertEquals(1, e.persistidas.size());
        assertEquals(1, e.flushes);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getEmpresaId());
        assertEquals(20L, dto.getVehiculoId());
        assertEquals(10L, dto.getChoferId());
        assertEquals(30L, dto.getRubroId());
        assertNull(dto.getTipoCargaId());
        assertEquals(BigDecimal.ONE, dto.getPesoKg());
        assertEquals(BigDecimal.ONE, dto.getVolumenM3());
        assertEquals(c.getInicio(), dto.getInicio()); assertEquals(c.getFin(), dto.getFin());
        assertEquals("Origen", dto.getOrigen()); assertEquals("Destino", dto.getDestino());
        assertEquals(-34.9, dto.getOrigenLat()); assertEquals(-56.2, dto.getOrigenLon());
        assertEquals("ASIGNADA", dto.getEstado()); assertNotNull(dto.getCreadaEn());
    }

    @Test void aceptaTipoCargaExistenteOpcional() throws Exception {
        Escenario e = new Escenario(); CrearGuiaCommand c = e.comando(); c.setTipoCargaId(40L);
        assertEquals(40L, e.bean().crear(c).getTipoCargaId());
        assertEquals(1, e.persistidas.size());
    }

    @Test void autorizacionUsaPrincipalEmpresaYVinculoActivo() throws Exception {
        Escenario e = new Escenario(); e.bean().crear(e.comando());
        assertTrue(e.consultaVinculo.contains("x.usuario.cedula = :cedula"));
        assertTrue(e.consultaVinculo.contains("x.empresa.id = :empresaId"));
        assertTrue(e.consultaVinculo.contains("x.estado = :estado"));
        assertEquals("responsable", e.parametros.get("cedula"));
        assertEquals(1L, e.parametros.get("empresaId"));
        assertEquals(EstadoVinculoEmpresa.ACTIVO, e.parametros.get("estado"));
    }

    @Test void rechazaEmpresaInactivaSinPersistir() {
        Escenario e = new Escenario(); e.empresa.setActiva(false);
        assertFalla(e, e.comando(), "deben pertenecer");
    }

    @Test void rechazaVehiculoDeshabilitadoSinPersistir() {
        Escenario e = new Escenario(); e.vehiculo.setHabilitado(false);
        assertFalla(e, e.comando(), "no estan vigentes");
    }

    @Test void rechazaChoferInactivoSinPersistir() {
        Escenario e = new Escenario(); e.chofer.setActivo(false);
        assertFalla(e, e.comando(), "esta inactivo");
    }

    @Test void rechazaPermisoQueEmpiezaDespuesDelInicio() {
        Escenario e = new Escenario(); e.vehiculo.setPermisoDesde(LocalDate.parse("2026-10-07"));
        assertFalla(e, e.comando(), "no estan vigentes");
    }

    @Test void rechazaPermisoQueVenceAntesDelFin() {
        Escenario e = new Escenario(); e.vehiculo.setPermisoHasta(LocalDate.parse("2026-10-06"));
        CrearGuiaCommand c = e.comando(); c.setFin("2026-10-07T00:00:00.000000001Z");
        assertFalla(e, c, "no estan vigentes");
    }

    @Test void aceptaFinExclusivoALaMedianochePosteriorAlVencimiento() throws Exception {
        Escenario e = new Escenario();
        e.vehiculo.setPermisoDesde(LocalDate.parse("2026-10-06"));
        e.vehiculo.setPermisoHasta(LocalDate.parse("2026-10-06"));
        e.vehiculo.setItvVencimiento(LocalDate.parse("2026-10-06"));
        CrearGuiaCommand c = e.comando(); c.setFin("2026-10-07T00:00:00Z");
        assertEquals(c.getFin(), e.bean().crear(c).getFin());
        assertEquals(1, e.persistidas.size());
    }

    @Test void rechazaFaltaDePermisoOItv() {
        for (int caso = 0; caso < 3; caso++) {
            Escenario e = new Escenario();
            if (caso == 0) e.vehiculo.setPermisoDesde(null);
            if (caso == 1) e.vehiculo.setPermisoHasta(null);
            if (caso == 2) e.vehiculo.setItvVencimiento(null);
            assertFalla(e, e.comando(), "no estan vigentes");
        }
    }

    @Test void rechazaExcesoDePesoAunqueVolumenSeaValido() {
        Escenario e = new Escenario(); CrearGuiaCommand c = e.comando();
        c.setPesoKg(new BigDecimal("100.01")); assertFalla(e, c, "peso de la carga supera");
    }

    @Test void rechazaExcesoDeVolumenAunquePesoSeaValido() {
        Escenario e = new Escenario(); CrearGuiaCommand c = e.comando();
        c.setVolumenM3(new BigDecimal("10.01")); assertFalla(e, c, "volumen de la carga supera");
    }

    @Test void rechazaCapacidadVolumetricaSinConfigurar() {
        Escenario e = new Escenario(); e.vehiculo.setCapacidadVolumenM3(null);
        assertFalla(e, e.comando(), "no esta configurada");
    }

    @Test void aceptaCargaExactamenteEnAmbasCapacidades() throws Exception {
        Escenario e = new Escenario(); CrearGuiaCommand c = e.comando();
        c.setPesoKg(BigDecimal.valueOf(100)); c.setVolumenM3(BigDecimal.TEN);
        GuiaDTO dto = e.bean().crear(c);
        assertEquals(c.getPesoKg(), dto.getPesoKg()); assertEquals(c.getVolumenM3(), dto.getVolumenM3());
    }

    @Test void rechazaCargaAusenteCeroONegativa() {
        for (BigDecimal valor : Arrays.asList(null, BigDecimal.ZERO, BigDecimal.ONE.negate())) {
            Escenario peso = new Escenario(); CrearGuiaCommand p = peso.comando(); p.setPesoKg(valor);
            assertFalla(peso, p, "peso en kg debe ser positivo");
            Escenario volumen = new Escenario(); CrearGuiaCommand v = volumen.comando(); v.setVolumenM3(valor);
            assertFalla(volumen, v, "volumen en m3 debe ser positivo");
        }
    }

    @Test void rechazaComandoNulo() {
        Escenario e = new Escenario(); assertFalla(e, null, "datos de la guia son obligatorios");
    }

    @Test void rechazaFechasAusentesSinZonaOInvalidas() {
        for (String valor : Arrays.asList(null, " ", "no-es-fecha", "2026-10-06T10:00:00")) {
            Escenario inicio = new Escenario(); CrearGuiaCommand a = inicio.comando(); a.setInicio(valor);
            assertFalla(inicio, a, "inicio");
            Escenario fin = new Escenario(); CrearGuiaCommand b = fin.comando(); b.setFin(valor);
            assertFalla(fin, b, "fin");
        }
    }

    @Test void rechazaPeriodoInvertidoODeDuracionCero() {
        for (String fin : List.of("2026-10-06T10:00:00Z", "2026-10-06T09:00:00Z")) {
            Escenario e = new Escenario(); CrearGuiaCommand c = e.comando(); c.setFin(fin);
            assertFalla(e, c, "inicio debe ser anterior");
        }
    }

    @Test void rechazaOrigenDestinoAusentesOIgualesNormalizados() {
        for (String[] valores : List.of(new String[]{null, "Destino"}, new String[]{"Origen", " "},
                new String[]{"  Origen ", "orIGen"})) {
            Escenario e = new Escenario(); CrearGuiaCommand c = e.comando();
            c.setOrigen(valores[0]); c.setDestino(valores[1]); assertFalla(e, c, "origen y el destino");
        }
    }

    @Test void rechazaReferenciasObligatoriasAusentes() {
        for (int caso = 0; caso < 3; caso++) {
            Escenario e = new Escenario(); CrearGuiaCommand c = e.comando();
            if (caso == 0) c.setVehiculoId(null);
            if (caso == 1) c.setChoferId(null);
            if (caso == 2) c.setRubroId(null);
            assertFalla(e, c, "Vehiculo, chofer y rubro son obligatorios");
        }
    }

    @Test void rechazaReferenciasInexistentes() {
        for (int caso = 0; caso < 4; caso++) {
            Escenario e = new Escenario(); CrearGuiaCommand c = e.comando();
            if (caso == 0) c.setVehiculoId(999L);
            if (caso == 1) c.setChoferId(999L);
            if (caso == 2) c.setRubroId(999L);
            if (caso == 3) c.setTipoCargaId(999L);
            assertFalla(e, c, "no existe");
        }
    }

    @Test void consultaConflictosConIntervalosSemiabiertosYRecursosCorrectos() throws Exception {
        Escenario e = new Escenario(); CrearGuiaCommand c = e.comando(); e.bean().crear(c);
        assertTrue(e.consultaConflicto.contains("g.inicio < :fin and g.fin > :inicio"));
        assertTrue(e.consultaConflicto.contains("g.vehiculo.id = :vehiculoId or g.chofer.id = :choferId"));
        assertTrue(e.consultaConflicto.contains("g.estado <> 'FINALIZADA'"));
        assertEquals(java.time.Instant.parse(c.getInicio()), e.parametros.get("inicio"));
        assertEquals(java.time.Instant.parse(c.getFin()), e.parametros.get("fin"));
        assertEquals(20L, e.parametros.get("vehiculoId")); assertEquals(10L, e.parametros.get("choferId"));
    }

    @Test void rechazaCreacionSinPrincipalSinPersistir() {
        for (String principal : Arrays.asList(null, " ")) {
            Escenario e = new Escenario(); e.sesion = Falsos.sesion(principal);
            assertFalla(e, e.comando(), "No hay un usuario autenticado");
        }
    }

    @Test void consultaChoferRechazaPrincipalAusenteONoRegistrado() {
        Escenario sinPrincipal = new Escenario(); sinPrincipal.sesion = Falsos.sesion(null);
        assertThrows(NegocioException.class, () -> sinPrincipal.bean().guiasDelChoferAutenticado());
        Escenario noChofer = new Escenario(); noChofer.choferEncontrado = false;
        NegocioException ex = assertThrows(NegocioException.class, () -> noChofer.bean().guiasDelChoferAutenticado());
        assertTrue(ex.getMessage().contains("no esta registrado como chofer"));
        assertTrue(noChofer.persistidas.isEmpty());
    }

    private static void assertFalla(Escenario e, CrearGuiaCommand comando, String esperado) {
        NegocioException ex = assertThrows(NegocioException.class, () -> e.bean().crear(comando));
        assertTrue(ex.getMessage().contains(esperado), ex::getMessage);
        assertTrue(e.persistidas.isEmpty(), "El rechazo no debe solicitar persistencia");
        assertEquals(0, e.flushes);
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
        Empresa empresaVinculada = empresa;
        SessionContext sesion = Falsos.sesion("responsable");
        long conflictosVehiculo;
        long conflictosChofer;
        List<Guia> guias = List.of();
        String ultimaConsulta, consultaVinculo, consultaConflicto, consultaChofer;
        boolean choferEncontrado = true;
        List<Guia> persistidas = new ArrayList<>();
        int flushes;
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
                    if (tipo == TipoCarga.class && id.equals(40L)) return conIdSeguro(new TipoCarga(), 40L);
                    return null;
                }
                if (m.getName().equals("createQuery")) return consulta((String) a[0]);
                if (m.getName().equals("persist")) {
                    Guia guia = (Guia) a[0]; conId(guia, 100L); persistidas.add(guia); return null;
                }
                if (m.getName().equals("flush")) { flushes++; return null; }
                throw new UnsupportedOperationException(m.getName());
            });
        }
        private TypedQuery<?> consulta(String jpql) {
            ultimaConsulta = jpql;
            if (jpql.startsWith("select count(g)")) { indiceConsultaConflicto = ++secuencia; consultaConflicto = jpql; }
            if (jpql.contains("from VinculoEmpresaUsuario")) consultaVinculo = jpql;
            if (jpql.contains("from Chofer c where")) consultaChofer = jpql;
            List<?> resultados = jpql.contains("from VinculoEmpresaUsuario") ? (empresaVinculada == null ? List.of() : List.of(empresaVinculada))
                    : jpql.contains("from Chofer c where") ? (choferEncontrado ? List.of(chofer) : List.of()) : jpql.contains("from Guia g") ? guias : List.of();
            return (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class}, (p, m, a) -> switch (m.getName()) {
                case "setParameter" -> { parametros.put((String) a[0], a[1]); yield p; }
                case "setMaxResults" -> p;
                case "getResultStream" -> jpql.contains("from VinculoEmpresaUsuario")
                        ? (empresaVinculada != null && Objects.equals(empresaVinculada.getId(), parametros.get("empresaId"))
                            && EstadoVinculoEmpresa.ACTIVO == parametros.get("estado")
                            ? List.of(empresaVinculada) : List.of()).stream()
                        : resultados.stream();
                case "getResultList" -> resultados;
                case "getSingleResult" -> conflictosVehiculo + conflictosChofer;
                default -> throw new UnsupportedOperationException(m.getName());
            });
        }
        private Empresa empresa(long id) { Empresa e = new Empresa(); e.setNumeroEmpresa(id); return e; }
        private Vehiculo vehiculo() { Vehiculo v = conIdSeguro(new Vehiculo(), 20L); v.setEmpresa(empresa); v.setCapacidadCargaKg(100); v.setCapacidadVolumenM3(BigDecimal.TEN); v.setPermisoDesde(LocalDate.parse("2026-10-01")); v.setPermisoHasta(LocalDate.parse("2026-10-10")); v.setItvVencimiento(LocalDate.parse("2026-10-10")); return v; }
        private Chofer chofer() { Chofer c = conIdSeguro(new Chofer(), 10L); c.setEmpresa(empresa); c.setCedula("otro"); return c; }
        private static <T> T conIdSeguro(T e, Long id) { try { return conId(e, id); } catch (Exception x) { throw new AssertionError(x); } }
    }
}
