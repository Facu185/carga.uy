package uy.edu.fing.tse.carga.generador;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehiculoTest {
    private Configuracion config;
    private Random azar;
    private Vehiculo vehiculo;

    @BeforeEach
    void preparar() {
        config = Configuracion.desdeEntorno(Map.of("GEN_INTERVALO_SEG", "30", "GEN_PROB_SIN_COBERTURA", "0"));
        azar = new Random(1);
        vehiculo = new Vehiculo(7, 30, 0, azar);
    }

    @Test
    void reportaUnaVezPorIntervalo() {
        int generadas = 0;
        for (long t = 0; t < 120_000; t += 100) {
            if (vehiculo.quizasReportar(t, config, azar)) {
                generadas++;
            }
        }
        assertEquals(4, generadas);
        assertEquals(4, vehiculo.pendientes());
        assertEquals("GEN00007", vehiculo.matricula());
    }

    @Test
    void conRespuestaAceptadaLasPosicionesSalenDelBuffer() {
        acumular(3);
        List<Posicion> envio = vehiculo.tomarParaEnviar(200_000, 500);

        vehiculo.resultado(envio, respuesta(Respuesta.Tipo.ACEPTADA, 0), 200_000);

        assertEquals(3, envio.size());
        assertEquals(0, vehiculo.pendientes());
        assertFalse(vehiculo.enVuelo());
    }

    @Test
    void conSaturacionConservaTodoYEsperaLoQueDiceRetryAfter() {
        acumular(2);
        List<Posicion> envio = vehiculo.tomarParaEnviar(200_000, 500);

        vehiculo.resultado(envio, respuesta(Respuesta.Tipo.SATURADA, 10), 200_000);

        assertEquals(2, vehiculo.pendientes());
        assertTrue(vehiculo.tomarParaEnviar(209_999, 500).isEmpty());
        List<Posicion> reintento = vehiculo.tomarParaEnviar(210_000, 500);
        assertEquals(envio, reintento);
    }

    @Test
    void conErrorDeRedConservaTodoYReintentaMasTarde() {
        acumular(1);
        List<Posicion> envio = vehiculo.tomarParaEnviar(200_000, 500);

        vehiculo.resultado(envio, Respuesta.sinRespuesta(5000), 200_000);

        assertEquals(1, vehiculo.pendientes());
        assertTrue(vehiculo.tomarParaEnviar(200_000 + Vehiculo.ESPERA_ERROR_MS - 1, 500).isEmpty());
        assertEquals(1, vehiculo.tomarParaEnviar(200_000 + Vehiculo.ESPERA_ERROR_MS, 500).size());
    }

    @Test
    void conRechazoDescartaPorqueReenviarNoLoArregla() {
        acumular(2);
        List<Posicion> envio = vehiculo.tomarParaEnviar(200_000, 500);

        vehiculo.resultado(envio, respuesta(Respuesta.Tipo.RECHAZADA, 0), 200_000);

        assertEquals(0, vehiculo.pendientes());
    }

    @Test
    void noMandaDosVecesALaVez() {
        acumular(2);
        assertEquals(2, vehiculo.tomarParaEnviar(200_000, 500).size());
        acumular(1);
        assertTrue(vehiculo.tomarParaEnviar(200_000, 500).isEmpty());
    }

    @Test
    void sinCoberturaAcumulaYAlVolverMandaTodoJuntoRespetandoElMaximo() {
        vehiculo.cortarCobertura(600_000);
        for (long t = 0; t < 600_000; t += 1000) {
            vehiculo.quizasReportar(t, config, azar);
            assertTrue(vehiculo.tomarParaEnviar(t, 500).isEmpty());
        }
        assertEquals(20, vehiculo.pendientes());

        List<Posicion> lote = vehiculo.tomarParaEnviar(600_000, 15);
        assertEquals(15, lote.size());
        assertEquals(5, vehiculo.pendientes());
    }

    @Test
    void lasPosicionesSalenDeLaMasViejaALaMasNueva() {
        acumular(3);
        List<Posicion> envio = vehiculo.tomarParaEnviar(200_000, 500);
        assertTrue(envio.get(0).instante().compareTo(envio.get(1).instante()) < 0);
        assertTrue(envio.get(1).instante().compareTo(envio.get(2).instante()) < 0);
    }

    @Test
    void unReenvioIntencionalVuelveAlBufferMarcadoComoDuplicado() {
        acumular(1);
        Posicion original = vehiculo.tomarParaEnviar(200_000, 500).get(0);
        vehiculo.resultado(List.of(original), respuesta(Respuesta.Tipo.ACEPTADA, 0), 200_000);

        vehiculo.reinyectar(original);

        Posicion reenvio = vehiculo.tomarParaEnviar(200_000, 500).get(0);
        assertEquals(original.idEvento(), reenvio.idEvento());
        assertTrue(reenvio.duplicada());
    }

    @Test
    void restaurarCoberturaPermiteMandarAunqueEstuvieraCortada() {
        acumular(1);
        vehiculo.cortarCobertura(Long.MAX_VALUE);
        assertTrue(vehiculo.tomarParaEnviar(200_000, 500).isEmpty());

        vehiculo.restaurarCobertura();

        assertEquals(1, vehiculo.tomarParaEnviar(200_000, 500).size());
    }

    private void acumular(int n) {
        int generadas = 0;
        for (long t = 0; generadas < n; t += 1000) {
            if (vehiculo.quizasReportar(t, config, azar)) {
                generadas++;
            }
        }
    }

    private static Respuesta respuesta(Respuesta.Tipo tipo, int reintentarSeg) {
        return new Respuesta(tipo, 0, reintentarSeg, Set.of(), 10);
    }
}
