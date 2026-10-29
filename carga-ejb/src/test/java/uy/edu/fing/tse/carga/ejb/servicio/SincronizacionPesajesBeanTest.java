package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.PesajeLocal;
import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.PesajesDeGuiaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoPesajeDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoSincronizacionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.Pesada;
import uy.edu.fing.tse.carga.ejb.observabilidad.Metricas;
import uy.edu.fing.tse.carga.ejb.persistencia.PesadaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.RecorridoDAO;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SincronizacionPesajesBeanTest {

    private final List<PesadaDTO> delNodo = new ArrayList<>();
    private boolean nodoDisponible;
    private boolean circuitoCerrado;
    private final List<PesadaRegistradaDTO> avisos = new ArrayList<>();
    private PesadasEnMemoria pesadas;
    private SincronizacionPesajesBean sincronizacion;

    @BeforeEach
    void preparar() {
        nodoDisponible = true;
        circuitoCerrado = true;
        pesadas = new PesadasEnMemoria();
        sincronizacion = new SincronizacionPesajesBean() {
            @Override
            void publicar(PesadaRegistradaDTO pesada) {
                avisos.add(pesada);
            }

            @Override
            boolean nodoBalanzasDisponible() {
                return circuitoCerrado;
            }
        };
        sincronizacion.pesadas = pesadas;
        sincronizacion.guias = new RecorridoDAO() {
            @Override
            public Optional<PeriodoDeGuia> periodoDeGuia(Long guiaId) {
                return guiaId == 77L
                        ? Optional.of(new PeriodoDeGuia("STZ1234", Instant.parse("2026-10-07T12:00:00Z"),
                                Instant.parse("2026-10-09T12:00:00Z")))
                        : Optional.empty();
            }
        };
        sincronizacion.metricas = new Metricas();
        sincronizacion.pesaje = new PesajeLocal() {
            @Override
            public List<PesadaDTO> obtenerPesadas(String matricula, String fecha) {
                return consultarPesadas(matricula, fecha).getPesadas();
            }

            @Override
            public ResultadoPesajeDTO consultarPesadas(String matricula, String fecha) {
                return nodoDisponible
                        ? ResultadoPesajeDTO.disponible(List.copyOf(delNodo), 10)
                        : ResultadoPesajeDTO.noDisponible("El nodo Balanzas no respondio.", 3000);
            }
        };
    }

    @Test
    void guardaLasPesadasNuevasYAvisaCadaUna() throws NegocioException {
        delNodo.add(pesada("P-1", "2026-10-07T10:15-03:00"));
        delNodo.add(pesada("P-2", "2026-10-07T14:40-03:00"));

        ResultadoSincronizacionDTO r = sincronizacion.sincronizar("STZ1234", "2026-10-07");

        assertTrue(r.isNodoDisponible());
        assertEquals(2, r.getNuevas());
        assertEquals(2, pesadas.guardadas.size());
        assertEquals(List.of("P-1", "P-2"), avisos.stream().map(PesadaRegistradaDTO::getIdPesada).toList());
    }

    @Test
    void sincronizarDosVecesNoDuplicaPesadasNiAvisos() throws NegocioException {
        delNodo.add(pesada("P-1", "2026-10-07T10:15-03:00"));
        sincronizacion.sincronizar("STZ1234", "2026-10-07");

        ResultadoSincronizacionDTO segunda = sincronizacion.sincronizar("STZ1234", "2026-10-07");

        assertEquals(0, segunda.getNuevas());
        assertEquals(1, segunda.getDuplicadas());
        assertEquals(1, pesadas.guardadas.size());
        assertEquals(1, avisos.size());
    }

    @Test
    void vinculaLaPesadaConLaGuiaVigenteEnElInstanteDelPesaje() throws NegocioException {
        pesadas.guiaEntre(Instant.parse("2026-10-07T12:00:00Z"), Instant.parse("2026-10-07T18:00:00Z"), 77L);
        delNodo.add(pesada("DENTRO", "2026-10-07T10:15-03:00"));  // 13:15 UTC
        delNodo.add(pesada("FUERA", "2026-10-07T08:00-03:00"));   // 11:00 UTC, antes de la guia

        ResultadoSincronizacionDTO r = sincronizacion.sincronizar("STZ1234", "2026-10-07");

        assertEquals(1, r.getVinculadasAGuia());
        assertEquals(77L, pesadas.guardadas.get("DENTRO").getGuiaId());
        // Sin guia vigente: es el dato que usa la regla "viaje sin guia" (P5)
        assertNull(pesadas.guardadas.get("FUERA").getGuiaId());
    }

    @Test
    void descartaLoQueElNodoMandaIncompletoYGuardaElResto() throws NegocioException {
        delNodo.add(pesada("BIEN", "2026-10-07T10:15-03:00"));
        delNodo.add(pesada("SIN-INSTANTE", null));
        delNodo.add(pesada("INSTANTE-ROTO", "ayer a la tarde"));

        ResultadoSincronizacionDTO r = sincronizacion.sincronizar("STZ1234", "2026-10-07");

        assertEquals(3, r.getRecibidas());
        assertEquals(1, r.getNuevas());
        assertEquals(2, r.getDescartadas());
        assertEquals(1, avisos.size());
    }

    @Test
    void conElNodoCaidoNoGuardaNadaYLoInforma() throws NegocioException {
        nodoDisponible = false;

        ResultadoSincronizacionDTO r = sincronizacion.sincronizar("STZ1234", "2026-10-07");

        assertFalse(r.isNodoDisponible());
        assertTrue(r.getMensaje().contains("no respondio"));
        assertTrue(pesadas.guardadas.isEmpty());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void rechazaMatriculaVaciaYFechaMalFormada() {
        assertThrows(NegocioException.class, () -> sincronizacion.sincronizar(" ", "2026-10-07"));
        assertThrows(NegocioException.class, () -> sincronizacion.sincronizar("STZ1234", "07/10/2026"));
    }

    @Test
    void lasPesadasDeUnaGuiaSonLasVinculadasDeTodosLosDiasDelViaje() throws NegocioException {
        pesadas.guiaEntre(Instant.parse("2026-10-07T12:00:00Z"), Instant.parse("2026-10-09T12:00:00Z"), 77L);
        delNodo.add(pesada("DIA-1", "2026-10-07T10:15-03:00"));
        delNodo.add(pesada("ANTES", "2026-10-07T08:00-03:00"));   // sin guia vigente: no es del viaje
        sincronizacion.sincronizar("STZ1234", "2026-10-07");
        delNodo.clear();
        delNodo.add(pesada("DIA-2", "2026-10-08T09:30-03:00"));
        sincronizacion.sincronizar("STZ1234", "2026-10-08");

        PesajesDeGuiaDTO r = sincronizacion.registradasDeGuia(77L);

        assertEquals(List.of("DIA-1", "DIA-2"), r.getPesadas().stream().map(PesadaRegistradaDTO::getIdPesada).toList());
        assertEquals(2, r.getCantidad());
        assertEquals("STZ1234", r.getMatricula());
        assertEquals("2026-10-07T12:00Z", r.getDesde());
        assertFalse(r.isTruncado());
        assertTrue(r.isNodoDisponible());
    }

    @Test
    void conElCircuitoAbiertoAvisaQuePuedenFaltarPesadasDeLaGuia() throws NegocioException {
        circuitoCerrado = false;

        PesajesDeGuiaDTO r = sincronizacion.registradasDeGuia(77L);

        assertTrue(r.getPesadas().isEmpty());
        assertFalse(r.isNodoDisponible());
    }

    @Test
    void truncaLasPesadasDeUnaGuiaAlMaximo() throws NegocioException {
        for (int i = 0; i <= SincronizacionPesajesBean.PESADAS_POR_GUIA_MAXIMAS; i++) {
            Pesada p = Pesada.desde(pesada("P-" + i, "2026-10-07T10:15-03:00"), 77L, java.time.OffsetDateTime.now());
            pesadas.guardarSiEsNueva(p);
        }

        PesajesDeGuiaDTO r = sincronizacion.registradasDeGuia(77L);

        assertTrue(r.isTruncado());
        assertEquals(SincronizacionPesajesBean.PESADAS_POR_GUIA_MAXIMAS, r.getCantidad());
    }

    @Test
    void rechazaGuiaInexistenteOInvalida() {
        assertThrows(NegocioException.class, () -> sincronizacion.registradasDeGuia(999L));
        assertThrows(NegocioException.class, () -> sincronizacion.registradasDeGuia(null));
        assertThrows(NegocioException.class, () -> sincronizacion.registradasDeGuia(0L));
    }

    static PesadaDTO pesada(String id, String instante) {
        PesadaDTO p = new PesadaDTO();
        p.setIdPesada(id);
        p.setMatricula("STZ1234");
        p.setPeaje("Peaje Pando");
        p.setPesoKg(28000);
        p.setInstante(instante);
        return p;
    }

    /** Repositorio en memoria con la misma semantica que la base: id_pesada unico. */
    static final class PesadasEnMemoria extends PesadaDAO {
        final Map<String, Pesada> guardadas = new LinkedHashMap<>();
        private Instant guiaDesde;
        private Instant guiaHasta;
        private Long guiaId;

        void guiaEntre(Instant desde, Instant hasta, Long id) {
            guiaDesde = desde;
            guiaHasta = hasta;
            guiaId = id;
        }

        @Override
        public boolean guardarSiEsNueva(Pesada p) {
            return guardadas.putIfAbsent(p.getIdPesada(), p) == null;
        }

        @Override
        public Optional<Pesada> buscarPorIdPesada(String idPesada) {
            return Optional.ofNullable(guardadas.get(idPesada));
        }

        @Override
        public List<Pesada> buscar(String matricula, LocalDate dia) {
            return new ArrayList<>(guardadas.values());
        }

        @Override
        public List<Pesada> buscarDeGuia(Long guiaId, int maximo) {
            return guardadas.values().stream()
                    .filter(p -> guiaId.equals(p.getGuiaId()))
                    .sorted(java.util.Comparator.comparing(Pesada::getInstante))
                    .limit(maximo)
                    .toList();
        }

        @Override
        public Optional<Long> guiaVigente(String matricula, Instant instante) {
            boolean dentro = guiaId != null && !instante.isBefore(guiaDesde) && instante.isBefore(guiaHasta);
            return dentro ? Optional.of(guiaId) : Optional.empty();
        }
    }
}
