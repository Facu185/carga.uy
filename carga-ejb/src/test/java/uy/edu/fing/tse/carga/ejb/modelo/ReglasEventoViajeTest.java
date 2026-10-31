package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;

import java.time.Instant;
import java.util.Base64;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uy.edu.fing.tse.carga.ejb.modelo.ReglasEventoViaje.ASIGNADA;
import static uy.edu.fing.tse.carga.ejb.modelo.ReglasEventoViaje.EN_CURSO;
import static uy.edu.fing.tse.carga.ejb.modelo.ReglasEventoViaje.FINALIZADA;

class ReglasEventoViajeTest {

    private static final Instant AHORA = Instant.parse("2026-10-20T12:00:00Z");
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    private static EventoViajeDTO evento(Consumer<EventoViajeDTO> ajuste) {
        EventoViajeDTO e = new EventoViajeDTO();
        e.setIdEvento("3f2a9c1e-7b4d-4e8a-9c1b-2d3e4f5a6b7c");
        e.setGuiaId(1L);
        e.setTipo("INICIO");
        e.setInstante("2026-10-20T08:00:00-03:00");
        ajuste.accept(e);
        return e;
    }

    private static void assertProblema(EventoViajeDTO e) {
        assertTrue(ReglasEventoViaje.problemaDeFormato(e, AHORA).isPresent(), () -> "Deberia rechazar " + e);
    }

    // ---------------- formato ----------------

    @Test
    void aceptaUnEventoBienFormado() {
        assertTrue(ReglasEventoViaje.problemaDeFormato(evento(e -> { }), AHORA).isEmpty());
        assertTrue(ReglasEventoViaje.problemaDeFormato(evento(e -> {
            e.setLatitud(-34.9);
            e.setLongitud(-56.2);
            e.setDetalle("Carga completa");
        }), AHORA).isEmpty());
    }

    @Test
    void exigeIdEventoConFormatoValido() {
        assertProblema(evento(e -> e.setIdEvento(null)));
        assertProblema(evento(e -> e.setIdEvento("corto")));
        assertProblema(evento(e -> e.setIdEvento("con espacios adentro")));
        assertProblema(evento(e -> e.setIdEvento("x".repeat(81))));
    }

    @Test
    void exigeGuiaTipoEInstante() {
        assertProblema(null);
        assertProblema(evento(e -> e.setGuiaId(null)));
        assertProblema(evento(e -> e.setTipo("LLEGADA")));
        assertProblema(evento(e -> e.setTipo(null)));
        assertProblema(evento(e -> e.setInstante("2026-10-20T08:00:00")));
        assertProblema(evento(e -> e.setInstante("ayer")));
    }

    @Test
    void rechazaInstantesEnElFuturoSalvoUnDesfaseChico() {
        assertTrue(ReglasEventoViaje.problemaDeFormato(evento(e -> e.setInstante("2026-10-20T12:04:00Z")), AHORA).isEmpty());
        assertProblema(evento(e -> e.setInstante("2026-10-20T12:06:00Z")));
    }

    @Test
    void limitaElDetalleYLoExigeEnIncidentes() {
        assertTrue(ReglasEventoViaje.problemaDeFormato(evento(e -> e.setDetalle("x".repeat(ReglasEventoViaje.DETALLE_MAX))), AHORA).isEmpty());
        assertProblema(evento(e -> e.setDetalle("x".repeat(ReglasEventoViaje.DETALLE_MAX + 1))));
        assertProblema(evento(e -> e.setTipo("INCIDENTE")));
        assertProblema(evento(e -> { e.setTipo("INCIDENTE"); e.setDetalle("   "); }));
        assertTrue(ReglasEventoViaje.problemaDeFormato(evento(e -> { e.setTipo("INCIDENTE"); e.setDetalle("Pinchadura"); }), AHORA).isEmpty());
    }

    @Test
    void exigeLatitudYLongitudJuntasYEnRango() {
        assertProblema(evento(e -> e.setLatitud(-34.9)));
        assertProblema(evento(e -> e.setLongitud(-56.2)));
        assertProblema(evento(e -> { e.setLatitud(-91.0); e.setLongitud(0.0); }));
        assertProblema(evento(e -> { e.setLatitud(0.0); e.setLongitud(181.0); }));
    }

    // ---------------- ciclo de vida de la guia ----------------

    @Test
    void elInicioSoloSeAdmiteEnUnaGuiaAsignada() {
        assertEquals(EN_CURSO, ReglasEventoViaje.estadoTrasEvento(ASIGNADA, TipoEventoViaje.INICIO));
        assertTrue(ReglasEventoViaje.motivoDeRechazo(EN_CURSO, TipoEventoViaje.INICIO).isPresent());
    }

    @Test
    void losEventosIntermediosYElFinExigenUnViajeEnCurso() {
        for (TipoEventoViaje tipo : new TipoEventoViaje[]{TipoEventoViaje.CARGA_PARCIAL, TipoEventoViaje.DESCARGA_PARCIAL,
                TipoEventoViaje.INCIDENTE, TipoEventoViaje.FIN}) {
            assertTrue(ReglasEventoViaje.motivoDeRechazo(ASIGNADA, tipo).isPresent(), tipo::name);
            assertTrue(ReglasEventoViaje.motivoDeRechazo(EN_CURSO, tipo).isEmpty(), tipo::name);
        }
        assertEquals(EN_CURSO, ReglasEventoViaje.estadoTrasEvento(EN_CURSO, TipoEventoViaje.INCIDENTE));
        assertEquals(FINALIZADA, ReglasEventoViaje.estadoTrasEvento(EN_CURSO, TipoEventoViaje.FIN));
    }

    @Test
    void unViajeFinalizadoNoAdmiteMasEventos() {
        for (TipoEventoViaje tipo : TipoEventoViaje.values()) {
            assertTrue(ReglasEventoViaje.motivoDeRechazo(FINALIZADA, tipo).isPresent(), tipo::name);
        }
        assertThrows(IllegalStateException.class, () -> ReglasEventoViaje.estadoTrasEvento(FINALIZADA, TipoEventoViaje.FIN));
    }

    // ---------------- foto ----------------

    @Test
    void laFotoEsOpcional() {
        assertNull(ReglasEventoViaje.decodificarFoto(null, 1000));
        assertNull(ReglasEventoViaje.decodificarFoto("  ", 1000));
    }

    @Test
    void decodificaUnaFotoJpeg() {
        assertArrayEquals(JPEG, ReglasEventoViaje.decodificarFoto(Base64.getEncoder().encodeToString(JPEG), 1000));
    }

    @Test
    void rechazaFotosQueNoSonJpegONoSonBase64() {
        String png = Base64.getEncoder().encodeToString(new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2});
        assertThrows(IllegalArgumentException.class, () -> ReglasEventoViaje.decodificarFoto(png, 1000));
        assertThrows(IllegalArgumentException.class, () -> ReglasEventoViaje.decodificarFoto("esto no es base64!", 1000));
    }

    @Test
    void rechazaFotosQueSuperanElTope() {
        byte[] grande = new byte[3000];
        System.arraycopy(JPEG, 0, grande, 0, JPEG.length);
        String base64 = Base64.getEncoder().encodeToString(grande);
        assertThrows(IllegalArgumentException.class, () -> ReglasEventoViaje.decodificarFoto(base64, 1000));
        assertEquals(3000, ReglasEventoViaje.decodificarFoto(base64, 3000).length);
    }
}
