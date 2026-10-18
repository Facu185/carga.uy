package uy.edu.fing.tse.carga.generador;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadisticasTest {
    @Test
    void percentilPorRangoMasCercano() {
        long[] valores = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertEquals(5, Estadisticas.percentil(valores, 50));
        assertEquals(10, Estadisticas.percentil(valores, 95));
        assertEquals(1, Estadisticas.percentil(valores, 1));
        assertEquals(0, Estadisticas.percentil(new long[0], 95));
    }

    @Test
    void lasUnicasAceptadasNoCuentanReenviosNiRechazadas() {
        Estadisticas e = new Estadisticas();
        Posicion a = posicion("a", false);
        Posicion b = posicion("b", false);
        Posicion aRepetida = posicion("a", true);
        Posicion invalida = posicion("x", false);

        e.registrar(List.of(a, b, aRepetida, invalida), new Respuesta(Respuesta.Tipo.ACEPTADA, 202, 0, Set.of(3), 20));

        Estadisticas.Total t = e.total();
        assertEquals(3, t.aceptadas());
        assertEquals(2, t.unicasAceptadas());
        assertEquals(1, t.rechazadas());
    }

    @Test
    void cuentaSaturacionesPorPosicionYErroresPorPedido() {
        Estadisticas e = new Estadisticas();
        e.registrar(List.of(posicion("a", false), posicion("b", false)),
            new Respuesta(Respuesta.Tipo.SATURADA, 503, 30, Set.of(), 3));
        e.registrar(List.of(posicion("c", false), posicion("d", false)), Respuesta.sinRespuesta(30000));

        Estadisticas.Total t = e.total();
        assertEquals(2, t.saturadas());
        assertEquals(1, t.errores());
        assertEquals(0, t.unicasAceptadas());
        assertEquals(2, t.pedidos());
    }

    @Test
    void elIntervaloSoloMuestraLoNuevo() {
        Estadisticas e = new Estadisticas();
        e.registrar(List.of(posicion("a", false)), new Respuesta(Respuesta.Tipo.ACEPTADA, 202, 0, Set.of(), 10));
        assertEquals(1, e.tomarIntervalo(10).aceptadas());

        e.registrar(List.of(posicion("b", false)), new Respuesta(Respuesta.Tipo.ACEPTADA, 202, 0, Set.of(), 10));
        e.registrar(List.of(posicion("c", false)), new Respuesta(Respuesta.Tipo.ACEPTADA, 202, 0, Set.of(), 10));
        Estadisticas.Intervalo i = e.tomarIntervalo(10);
        assertEquals(2, i.aceptadas());
        assertEquals(0.2, i.pedidosPorSeg(), 1e-9);
    }

    @Test
    void leeUnaSerieDeLasMetricasDelCentral() {
        String prometheus = "# TYPE carga_tracking_posiciones_total counter\n"
            + "carga_tracking_posiciones_total{resultado=\"aceptada\"} 120\n"
            + "carga_tracking_posiciones_total{resultado=\"nueva\"} 118\n";
        assertEquals(OptionalDouble.of(118),
            ClienteHttp.valorDe(prometheus, "carga_tracking_posiciones_total{resultado=\"nueva\"}"));

        assertEquals(OptionalDouble.of(0), ClienteHttp.valorDe("", "carga_tracking_cola_pendientes"));
    }

    private static Posicion posicion(String id, boolean duplicada) {
        return new Posicion(id, "GEN00001", -34.9, -56.1, "2026-10-14T10:00:00Z", duplicada);
    }
}
