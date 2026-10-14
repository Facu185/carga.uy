package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.MonitorLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.ActivacionNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.DatosNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.EstadoNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.MetricasDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.NodoPerifericoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.NodoPeriferico;
import uy.edu.fing.tse.carga.ejb.modelo.TipoNodo;
import uy.edu.fing.tse.carga.ejb.persistencia.NodoPerifericoDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodosPerifericosBeanTest {

    private NodosEnMemoria dao;
    private Falsos.AuditoriaFalsa auditoria;
    private NodosPerifericosBean servicio;
    private Long principal;

    @BeforeEach
    void preparar() throws NegocioException {
        dao = new NodosEnMemoria();
        auditoria = new Falsos.AuditoriaFalsa();
        servicio = new NodosPerifericosBean();
        servicio.dao = dao;
        servicio.auditoria = auditoria;
        servicio.monitor = new MonitorSinNodos();
        NodoPeriferico n = NodoPeriferico.nuevoBalanzas("Balanzas peajes", "http://balanzas:8080/balanzas/api/pesadas");
        n.setActivo(true);
        principal = dao.guardar(n).getId();
    }

    @Test
    void elAltaNaceDesactivadaYQuedaAuditada() throws NegocioException {
        NodoPerifericoDTO nuevo = servicio.crear(new DatosNodoDTO("Balanzas respaldo", "BALANZAS", "http://respaldo/api"));
        assertFalse(nuevo.isActivo());
        assertEquals(AccionAuditada.NODO_PERIFERICO_CREADO, auditoria.ultimo().accion());
    }

    @Test
    void noSeRepiteElNombreNiSeDaDeAltaUnNodoTracking() {
        assertThrows(NegocioException.class,
                () -> servicio.crear(new DatosNodoDTO("balanzas PEAJES", "BALANZAS", "http://otro/api")));
        assertThrows(NegocioException.class,
                () -> servicio.crear(new DatosNodoDTO("Otro tracking", "TRACKING", null)));
    }

    @Test
    void cambiarLaUrlQuedaAuditadoConElValorAnteriorYElNuevo() throws NegocioException {
        servicio.modificar(principal, new DatosNodoDTO(null, null, "https://balanzas.mtop.uy/api/pesadas"));
        String detalle = auditoria.ultimo().detalle();
        assertTrue(detalle.contains("antes=http://balanzas:8080/balanzas/api/pesadas"), detalle);
        assertTrue(detalle.contains("ahora=https://balanzas.mtop.uy/api/pesadas"), detalle);
    }

    @Test
    void sinCambiosNoSeAudita() throws NegocioException {
        int antes = auditoria.registros.size();
        servicio.modificar(principal, new DatosNodoDTO("Balanzas peajes", null, "http://balanzas:8080/balanzas/api/pesadas"));
        assertEquals(antes, auditoria.registros.size());
    }

    @Test
    void desactivarExigeFundamentoYQuedaRegistrado() throws NegocioException {
        assertThrows(NegocioException.class, () -> servicio.cambiarActivacion(principal, new ActivacionNodoDTO(false, " ")));

        servicio.cambiarActivacion(principal, new ActivacionNodoDTO(false, "Mantenimiento del nodo"));

        assertFalse(dao.buscar(principal).orElseThrow().isActivo());
        assertEquals(AccionAuditada.NODO_PERIFERICO_DESACTIVADO, auditoria.ultimo().accion());
        assertEquals("Mantenimiento del nodo", auditoria.ultimo().fundamento());
    }

    @Test
    void noSePuedenTenerDosNodosBalanzasActivos() throws NegocioException {
        Long respaldo = servicio.crear(new DatosNodoDTO("Balanzas respaldo", "BALANZAS", "http://respaldo/api")).getId();

        assertThrows(NegocioException.class, () -> servicio.cambiarActivacion(respaldo, new ActivacionNodoDTO(true, null)));

        servicio.cambiarActivacion(principal, new ActivacionNodoDTO(false, "Pasaje al nodo de respaldo"));
        servicio.cambiarActivacion(respaldo, new ActivacionNodoDTO(true, null));
        assertTrue(dao.buscar(respaldo).orElseThrow().isActivo());
    }

    /** Repositorio en memoria con las mismas reglas que la base. */
    static final class NodosEnMemoria extends NodoPerifericoDAO {
        final List<NodoPeriferico> nodos = new ArrayList<>();
        private long siguienteId = 1;

        @Override
        public List<NodoPeriferico> listar() {
            return List.copyOf(nodos);
        }

        @Override
        public Optional<NodoPeriferico> buscar(Long id) {
            return nodos.stream().filter(n -> n.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<NodoPeriferico> buscarActivo(TipoNodo tipo) {
            return nodos.stream().filter(n -> n.getTipo() == tipo && n.isActivo()).findFirst();
        }

        @Override
        public boolean hayDeTipo(TipoNodo tipo) {
            return nodos.stream().anyMatch(n -> n.getTipo() == tipo);
        }

        @Override
        public boolean existeNombre(String nombre, Long excepto) {
            return nodos.stream().anyMatch(n -> n.getNombre().equalsIgnoreCase(nombre) && !n.getId().equals(excepto));
        }

        @Override
        public NodoPeriferico guardar(NodoPeriferico nodo) {
            nodos.add(Falsos.conId(nodo, siguienteId++));
            return nodo;
        }
    }

    static final class MonitorSinNodos implements MonitorLocal {
        @Override
        public MetricasDTO metricas() {
            return new MetricasDTO();
        }

        @Override
        public List<EstadoNodoDTO> estadoNodos() {
            return List.of();
        }

        @Override
        public List<RecorridoDTO> mapaFlota(int puntosPorVehiculo) {
            return List.of();
        }
    }
}
