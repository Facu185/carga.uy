package uy.edu.fing.tse.carga.generador;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespuestaTest {
    @Test
    void clasificaLasRespuestasDelCentral() {
        assertEquals(Respuesta.Tipo.ACEPTADA, Respuesta.desde(202, "{\"idEvento\":\"x\"}", null, 5).tipo());
        assertEquals(Respuesta.Tipo.RECHAZADA, Respuesta.desde(400, "{\"error\":\"...\"}", null, 5).tipo());
        assertEquals(Respuesta.Tipo.RECHAZADA, Respuesta.desde(401, "", null, 5).tipo());
        assertEquals(Respuesta.Tipo.SATURADA, Respuesta.desde(503, "", "30", 5).tipo());
        assertEquals(Respuesta.Tipo.ERROR, Respuesta.desde(500, "", null, 5).tipo());
        assertEquals(Respuesta.Tipo.ERROR, Respuesta.sinRespuesta(5).tipo());
    }

    @Test
    void tomaLaEsperaDelHeaderRetryAfter() {
        assertEquals(30, Respuesta.desde(503, "", "30", 5).reintentarSeg());
        assertEquals(Respuesta.REINTENTO_POR_DEFECTO_SEG, Respuesta.desde(503, "", null, 5).reintentarSeg());
        assertEquals(Respuesta.REINTENTO_POR_DEFECTO_SEG, Respuesta.desde(503, "", "manana", 5).reintentarSeg());
    }

    @Test
    void leeLosIndicesRechazadosDeUnLote() {
        String cuerpo = "{\"aceptadas\":2,\"rechazadas\":[{\"idEvento\":\"b\",\"indice\":1,\"motivo\":\"...\"},"
            + "{\"idEvento\":\"d\",\"indice\":3,\"motivo\":\"...\"}],\"recibidas\":4}";
        assertEquals(Set.of(1, 3), Respuesta.desde(202, cuerpo, null, 5).rechazadas());
    }

    @Test
    void sinRechazosNoHayIndices() {
        assertTrue(Respuesta.desde(202, "{\"aceptadas\":3,\"rechazadas\":[],\"recibidas\":3}", null, 5)
            .rechazadas().isEmpty());
        assertTrue(Respuesta.desde(202, "{\"idEvento\":\"x\"}", null, 5).rechazadas().isEmpty());
    }
}
