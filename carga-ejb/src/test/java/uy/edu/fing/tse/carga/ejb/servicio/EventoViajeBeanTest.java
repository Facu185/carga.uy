package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.dto.comun.ViajeFinalizadoDTO;
import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;
import uy.edu.fing.tse.carga.api.dto.viaje.ResultadoEventoDTO;
import uy.edu.fing.tse.carga.api.exception.ConflictoException;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.RecursoAjenoException;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.EventoViaje;
import uy.edu.fing.tse.carga.ejb.modelo.Guia;
import uy.edu.fing.tse.carga.ejb.modelo.Rubro;
import uy.edu.fing.tse.carga.ejb.modelo.TipoEventoViaje;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.persistencia.EventoViajeDAO;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uy.edu.fing.tse.carga.ejb.servicio.Falsos.conId;

class EventoViajeBeanTest {

    private final List<EventoViaje> guardados = new ArrayList<>();
    private final List<ViajeFinalizadoDTO> avisos = new ArrayList<>();
    private final Map<Long, Guia> guias = new HashMap<>();
    private final List<String> bloqueos = new ArrayList<>();
    private Vehiculo vehiculo;
    private Runnable alRefrescarVehiculo = () -> {};
    private Runnable alRefrescarGuia = () -> {};
    private EventoViaje colisionAlGuardar;
    private Chofer chofer;
    private Chofer otroChofer;
    private Guia guia;
    private EventoViajeBean bean;

    @BeforeEach
    void preparar() {
        Empresa empresa = conId(new Empresa(), 1L);
        vehiculo = conId(new Vehiculo(), 1L);
        vehiculo.setMatricula("STZ1234");
        vehiculo.setHabilitado(true);
        chofer = conId(new Chofer(), 1L);
        otroChofer = conId(new Chofer(), 2L);
        guia = conId(new Guia(empresa, vehiculo, chofer, conId(new Rubro(), 1L), null, BigDecimal.TEN, BigDecimal.ONE,
                Instant.parse("2026-10-20T11:00:00Z"), Instant.parse("2026-10-20T17:00:00Z"),
                "Montevideo", null, null, "Minas", null, null), 100L);
        guias.put(100L, guia);
        bean = bean(chofer);
    }

    private EventoViajeBean bean(Chofer autenticado) {
        EventoViajeBean b = new EventoViajeBean() {
            @Override
            Chofer choferAutenticado() {
                return autenticado;
            }
        };
        // Interacciones JPA, no bloqueos ni carreras reales del contenedor.
        b.em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class}, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("find")) {
                        if (args[0] == Guia.class) {
                            if (args.length == 3) {
                                assertEquals(LockModeType.PESSIMISTIC_WRITE, args[2]);
                                bloqueos.add("guia");
                            }
                            return guias.get(args[1]);
                        }
                        if (args[0] == Vehiculo.class) {
                            assertEquals(LockModeType.PESSIMISTIC_WRITE, args[2]);
                            bloqueos.add("vehiculo");
                            return vehiculo.getId().equals(args[1]) ? vehiculo : null;
                        }
                    }
                    if (metodo.getName().equals("refresh")) {
                        assertEquals(LockModeType.PESSIMISTIC_WRITE, args[1]);
                        if (args[0] instanceof Vehiculo) alRefrescarVehiculo.run();
                        else if (args[0] instanceof Guia) alRefrescarGuia.run();
                        return null;
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });
        b.eventos = new EventoViajeDAO() {
            @Override
            public Optional<EventoViaje> buscarPorIdEvento(String idEvento) {
                return guardados.stream().filter(e -> e.getIdEvento().equals(idEvento)).findFirst();
            }

            @Override
            public boolean guardarSiEsNuevo(EventoViaje evento) {
                if (colisionAlGuardar != null) {
                    guardados.add(colisionAlGuardar);
                    return false;
                }
                if (guardados.stream().anyMatch(e -> e.getIdEvento().equals(evento.getIdEvento()))) return false;
                guardados.add(evento);
                return true;
            }

            @Override
            public Optional<Instant> instanteDeInicio(Long guiaId) {
                return guardados.stream().filter(e -> e.getGuia().getId().equals(guiaId)
                        && e.getTipo().equals("INICIO")).map(EventoViaje::getInstante).findFirst();
            }
        };
        b.avisoFinDeViaje = avisos::add;
        // Las metricas no son parte de lo que se prueba: un proxy que ignora cualquier metodo
        b.metricas = (MetricasLocal) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{MetricasLocal.class}, (proxy, metodo, args) -> metodo.getReturnType() == String.class ? "" : null);
        return b;
    }

    private static EventoViajeDTO evento(String id, String tipo, String instante) {
        EventoViajeDTO e = new EventoViajeDTO();
        e.setIdEvento(id);
        e.setGuiaId(100L);
        e.setTipo(tipo);
        e.setInstante(instante);
        return e;
    }

    private static final String INICIO = "evento-inicio-0001";

    private ResultadoEventoDTO iniciar() throws NegocioException {
        return bean.registrar(evento(INICIO, "INICIO", "2026-10-20T08:05:00-03:00"));
    }

    @Test
    void registraElInicioYPoneElViajeEnCurso() throws NegocioException {
        ResultadoEventoDTO r = iniciar();
        assertTrue(r.isNuevo());
        assertEquals("EN_CURSO", r.getEstadoGuia());
        assertEquals("EN_CURSO", guia.getEstado());
        assertEquals(1, guardados.size());
        assertEquals(INICIO, r.getEvento().getIdEvento());
    }

    @Test
    void unReenvioDelMismoEventoNoLoDuplica() throws NegocioException {
        iniciar();
        ResultadoEventoDTO reenvio = iniciar();
        assertFalse(reenvio.isNuevo());
        assertEquals("EN_CURSO", reenvio.getEstadoGuia());
        assertEquals(1, guardados.size());
    }

    @Test
    void unIdEventoYaUsadoParaOtroEventoEsUnConflicto() throws NegocioException {
        iniciar();
        assertThrows(ConflictoException.class,
                () -> bean.registrar(evento(INICIO, "CARGA_PARCIAL", "2026-10-20T09:00:00-03:00")));
        assertEquals(1, guardados.size());
    }

    @Test
    void elFinCierraElViajeYAvisaParaObtenerLosPesajes() throws NegocioException {
        iniciar();
        ResultadoEventoDTO fin = bean.registrar(evento("evento-fin-0001", "FIN", "2026-10-20T13:50:00-03:00"));
        assertEquals("FINALIZADA", fin.getEstadoGuia());
        assertEquals(1, avisos.size());
        ViajeFinalizadoDTO aviso = avisos.get(0);
        assertEquals(100L, aviso.getGuiaId());
        assertEquals("STZ1234", aviso.getMatricula());
        assertEquals("2026-10-20T11:05:00Z", aviso.getInicio());
        assertEquals("2026-10-20T16:50:00Z", aviso.getFin());
    }

    @Test
    void reenviarElFinDeUnViajeYaCerradoNoEsUnError() throws NegocioException {
        iniciar();
        bean.registrar(evento("evento-fin-0001", "FIN", "2026-10-20T13:50:00-03:00"));
        ResultadoEventoDTO reenvio = bean.registrar(evento("evento-fin-0001", "FIN", "2026-10-20T13:50:00-03:00"));
        assertFalse(reenvio.isNuevo());
        assertEquals(1, avisos.size(), "Un reenvio no tiene que volver a disparar los pesajes");
    }

    @Test
    void rechazaEventosQueElEstadoDelViajeNoAdmite() throws NegocioException {
        assertThrows(ConflictoException.class,
                () -> bean.registrar(evento("evento-fin-sin-inicio", "FIN", "2026-10-20T13:50:00-03:00")));
        iniciar();
        bean.registrar(evento("evento-fin-0001", "FIN", "2026-10-20T13:50:00-03:00"));
        assertThrows(ConflictoException.class,
                () -> bean.registrar(evento("evento-tarde-0001", "CARGA_PARCIAL", "2026-10-20T14:00:00-03:00")));
        assertEquals(2, guardados.size());
        assertEquals(1, avisos.size());
    }

    @Test
    void noPermiteRegistrarEnLaGuiaDeOtroChofer() {
        EventoViajeBean ajeno = bean(otroChofer);
        assertThrows(RecursoAjenoException.class,
                () -> ajeno.registrar(evento("evento-ajeno-0001", "INICIO", "2026-10-20T08:00:00-03:00")));
        assertEquals("ASIGNADA", guia.getEstado());
        assertTrue(guardados.isEmpty());
    }

    @Test
    void unaGuiaInexistenteSeRespondeIgualQueUnaAjena() {
        EventoViajeDTO e = evento("evento-inexistente", "INICIO", "2026-10-20T08:00:00-03:00");
        e.setGuiaId(999L);
        assertThrows(RecursoAjenoException.class, () -> bean.registrar(e));
    }

    @Test
    void rechazaEventosMalFormadosSinGuardarNada() {
        NegocioException e = assertThrows(NegocioException.class,
                () -> bean.registrar(evento("evento-0001", "LLEGADA", "2026-10-20T08:00:00-03:00")));
        assertEquals(NegocioException.class, e.getClass());
        assertTrue(guardados.isEmpty());
    }

    @Test
    void guardaLaFotoJpegYRechazaLaQueNoLoEs() throws NegocioException {
        EventoViajeDTO conFoto = evento(INICIO, "INICIO", "2026-10-20T08:05:00-03:00");
        conFoto.setFotoBase64(Base64.getEncoder().encodeToString(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1}));
        assertTrue(bean.registrar(conFoto).getEvento().isTieneFoto());

        EventoViajeDTO noJpeg = evento("evento-incidente-01", "INCIDENTE", "2026-10-20T09:00:00-03:00");
        noJpeg.setDetalle("Pinchadura");
        noJpeg.setFotoBase64(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4}));
        assertThrows(NegocioException.class, () -> bean.registrar(noJpeg));
        assertEquals(1, guardados.size());
    }

    @Test
    void elTipoDeLosEventosGuardadosEsElDelEnum() throws NegocioException {
        iniciar();
        assertEquals(TipoEventoViaje.INICIO.name(), guardados.get(0).getTipo());
    }

    @Test
    void elInicioBloqueaVehiculoAntesQueGuia() throws NegocioException {
        iniciar();
        assertEquals(List.of("vehiculo", "guia"), bloqueos);
    }

    @Test
    void rechazaInicioOfflineSiElVehiculoEstaDeBajaAlProcesarlo() {
        vehiculo.setHabilitado(false);
        assertThrows(ConflictoException.class, this::iniciar);
        assertEquals("ASIGNADA", guia.getEstado());
        assertTrue(guardados.isEmpty());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void revalidaLaBajaDespuesDeObtenerElBloqueo() {
        alRefrescarVehiculo = () -> vehiculo.setHabilitado(false);
        assertThrows(ConflictoException.class, this::iniciar);
        assertEquals(List.of("vehiculo", "guia"), bloqueos);
        assertEquals("ASIGNADA", guia.getEstado());
        assertTrue(guardados.isEmpty());
    }

    @Test
    void rechazaSiLaAsignacionCambioAlReleerLaGuia() {
        alRefrescarGuia = () -> cambiarVehiculo(conId(new Vehiculo(), 2L));
        assertThrows(ConflictoException.class, this::iniciar);
        assertEquals(List.of("vehiculo", "guia"), bloqueos);
        assertEquals("ASIGNADA", guia.getEstado());
        assertTrue(guardados.isEmpty());
    }

    @Test
    void sinVehiculoNoSePuedeRegistrarUnInicioNuevo() {
        cambiarVehiculo(null);
        assertThrows(ConflictoException.class, this::iniciar);
        assertTrue(guardados.isEmpty());
    }

    @Test
    void asignacionNuevaTrasLeerUnHistoricoNoOmiteElLockDelVehiculo() {
        cambiarVehiculo(null);
        alRefrescarGuia = () -> cambiarVehiculo(vehiculo);
        assertThrows(ConflictoException.class, this::iniciar);
        assertEquals(List.of("guia"), bloqueos);
        assertEquals("ASIGNADA", guia.getEstado());
        assertTrue(guardados.isEmpty());
    }

    @Test
    void reenvioIdenticoNoRevalidaBajaNiEstadoComoInicioNuevo() throws NegocioException {
        iniciar();
        bean.registrar(evento("evento-fin-0001", "FIN", "2026-10-20T13:50:00-03:00"));
        vehiculo.setHabilitado(false);
        ResultadoEventoDTO r = iniciar();
        assertFalse(r.isNuevo());
        assertEquals("FINALIZADA", r.getEstadoGuia());
        assertEquals(2, guardados.size());
        assertEquals(1, avisos.size());
    }

    @Test
    void unMismoIdConOtroInstanteEsConflicto() throws NegocioException {
        iniciar();
        assertThrows(ConflictoException.class,
                () -> bean.registrar(evento(INICIO, "INICIO", "2026-10-20T08:06:00-03:00")));
        assertEquals(1, guardados.size());
    }

    @Test
    void unMismoIdConOtroDetalleEsConflicto() throws NegocioException {
        iniciar();
        EventoViajeDTO cambio = evento(INICIO, "INICIO", "2026-10-20T08:05:00-03:00");
        cambio.setDetalle("Contenido cambiado");
        assertThrows(ConflictoException.class, () -> bean.registrar(cambio));
        assertEquals(1, guardados.size());
    }

    @Test
    void unMismoIdConOtraUbicacionEsConflicto() throws NegocioException {
        iniciar();
        EventoViajeDTO cambio = evento(INICIO, "INICIO", "2026-10-20T08:05:00-03:00");
        cambio.setLatitud(-34.0);
        cambio.setLongitud(-56.0);
        assertThrows(ConflictoException.class, () -> bean.registrar(cambio));
        assertEquals(1, guardados.size());
    }

    @Test
    void unMismoIdConOtrosBytesDeFotoEsConflicto() throws NegocioException {
        EventoViajeDTO e = evento(INICIO, "INICIO", "2026-10-20T08:05:00-03:00");
        e.setFotoBase64(Base64.getEncoder().encodeToString(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1}));
        bean.registrar(e);
        e.setFotoBase64(Base64.getEncoder().encodeToString(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 2}));
        assertThrows(ConflictoException.class, () -> bean.registrar(e));
        assertEquals(1, guardados.size());
    }

    @Test
    void normalizaOffsetDetalleYPrecisionDeBaseParaElReenvio() throws NegocioException {
        EventoViajeDTO e = evento(INICIO, "INICIO", "2026-10-20T08:05:00.123456789-03:00");
        e.setDetalle("  Salida  ");
        bean.registrar(e);
        assertEquals(Instant.parse("2026-10-20T11:05:00.123456Z"), guardados.get(0).getInstante());
        e.setInstante("2026-10-20T11:05:00.123456Z");
        e.setDetalle("Salida");
        assertFalse(bean.registrar(e).isNuevo());
        assertEquals(1, guardados.size());
    }

    @Test
    void colisionAtomicaConOtroContenidoNoCambiaEstado() {
        colisionAlGuardar = new EventoViaje(guia, INICIO, TipoEventoViaje.INICIO,
                Instant.parse("2026-10-20T11:06:00Z"), null, null, null, null);
        assertThrows(ConflictoException.class, this::iniciar);
        assertEquals("ASIGNADA", guia.getEstado());
        assertEquals(1, guardados.size());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void colisionAtomicaDeIdEnOtraGuiaEsConflictoSinExponerSuEvento() {
        Guia otra = conId(new Guia(new Empresa(), vehiculo, chofer, new Rubro(), null,
                BigDecimal.TEN, BigDecimal.ONE, guia.getInicio(), guia.getFin(),
                "Origen", null, null, "Destino", null, null), 101L);
        colisionAlGuardar = new EventoViaje(otra, INICIO, TipoEventoViaje.INICIO,
                Instant.parse("2026-10-20T11:05:00Z"), null, null, null, null);
        assertThrows(ConflictoException.class, this::iniciar);
        assertEquals("ASIGNADA", guia.getEstado());
        assertEquals(1, guardados.size());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void colisionAtomicaIdenticaDevuelveGanadorSinRepetirEfectos() throws NegocioException {
        colisionAlGuardar = new EventoViaje(guia, INICIO, TipoEventoViaje.INICIO,
                Instant.parse("2026-10-20T11:05:00Z"), null, null, null, null);
        assertFalse(iniciar().isNuevo());
        assertEquals(1, guardados.size());
        assertEquals("ASIGNADA", guia.getEstado(), "El doble no simula la transaccion ganadora; no debe aplicar otra transicion");
        assertTrue(avisos.isEmpty());
    }

    @Test
    void falloDeInsercionNoAplicaTransicionNiAviso() throws NegocioException {
        iniciar();
        bean.eventos = new EventoViajeDAO() {
            @Override public Optional<EventoViaje> buscarPorIdEvento(String id) { return Optional.empty(); }
            @Override public boolean guardarSiEsNuevo(EventoViaje e) { throw new IllegalStateException("fallo de persistencia"); }
        };
        assertThrows(IllegalStateException.class,
                () -> bean.registrar(evento("evento-fin-0001", "FIN", "2026-10-20T13:50:00-03:00")));
        assertEquals("EN_CURSO", guia.getEstado());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void falloDeAvisoSePropagaParaQueElContenedorRevierta() throws NegocioException {
        iniciar();
        bean.avisoFinDeViaje = aviso -> { throw new NegocioException("fallo de aviso"); };
        assertThrows(NegocioException.class,
                () -> bean.registrar(evento("evento-fin-0001", "FIN", "2026-10-20T13:50:00-03:00")));
        // Un bean creado directamente NO verifica rollback de la entidad ni de la insercion.
        assertTrue(avisos.isEmpty());
    }

    private void cambiarVehiculo(Vehiculo nuevo) {
        try {
            var campo = Guia.class.getDeclaredField("vehiculo");
            campo.setAccessible(true);
            campo.set(guia, nuevo);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
