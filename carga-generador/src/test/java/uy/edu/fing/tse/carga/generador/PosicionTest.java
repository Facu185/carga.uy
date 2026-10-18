package uy.edu.fing.tse.carga.generador;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosicionTest {
    private final Posicion p = new Posicion("e1", "GEN00001", -34.9, -56.16, "2026-10-14T10:00:00Z", false);

    @Test
    void elJsonTieneElFormatoQueEsperaElCentral() {
        assertEquals("{\"idEvento\":\"e1\",\"matricula\":\"GEN00001\",\"latitud\":-34.900000,"
            + "\"longitud\":-56.160000,\"instante\":\"2026-10-14T10:00:00Z\"}", p.json());
    }

    @Test
    void elDecimalVaConPuntoAunqueElSistemaEsteEnEspanol() {
        Locale anterior = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("es-UY"));
            assertEquals(true, p.json().contains("\"latitud\":-34.900000"));
        } finally {
            Locale.setDefault(anterior);
        }
    }

    @Test
    void unLoteEsUnArregloJson() {
        Posicion q = new Posicion("e2", "GEN00001", -34.8, -56.1, "2026-10-14T10:00:30Z", false);
        assertEquals("[" + p.json() + "," + q.json() + "]", Posicion.lote(List.of(p, q)));
    }
}
