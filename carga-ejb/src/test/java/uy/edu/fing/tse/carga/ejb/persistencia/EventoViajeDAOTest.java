package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.EventoViaje;
import uy.edu.fing.tse.carga.ejb.modelo.Guia;
import uy.edu.fing.tse.carga.ejb.modelo.Rubro;
import uy.edu.fing.tse.carga.ejb.modelo.TipoEventoViaje;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Captura SQL/parametros; no ejecuta PostgreSQL ni verifica concurrencia. */
class EventoViajeDAOTest {
    private final Map<Integer, Object> parametros = new HashMap<>();
    private String sql;

    @Test
    void insertaAtomicamenteConParametrosNulosTipados() throws Exception {
        EventoViaje evento = evento(null);
        assertTrue(dao(1).guardarSiEsNuevo(evento));
        assertTrue(sql.contains("ON CONFLICT (id_evento) DO NOTHING"));
        assertTrue(sql.contains("CAST(?8 AS bytea)"));
        assertTrue(sql.contains("CAST(?6 AS double precision)"));
        assertTrue(sql.contains("CAST(?7 AS double precision)"));
        assertEquals(77L, parametros.get(1));
        assertEquals("evento-0001", parametros.get(2));
        assertEquals("INICIO", parametros.get(3));
        assertEquals(evento.getInstante().atOffset(ZoneOffset.UTC), parametros.get(4));
        assertNull(parametros.get(5));
        assertNull(parametros.get(6));
        assertNull(parametros.get(7));
        assertNull(parametros.get(8));
        assertEquals(9, parametros.size());
    }

    @Test
    void conflictoNoInsertaYConservaBytesDeFoto() throws Exception {
        byte[] foto = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};
        assertFalse(dao(0).guardarSiEsNuevo(evento(foto)));
        assertArrayEquals(foto, (byte[]) parametros.get(8));
    }

    private EventoViajeDAO dao(int insertadas) {
        Query consulta = (Query) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Query.class},
                (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "setParameter" -> { parametros.put((Integer) args[0], args[1]); yield proxy; }
                    case "executeUpdate" -> insertadas;
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
        EventoViajeDAO dao = new EventoViajeDAO();
        dao.em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                (proxy, metodo, args) -> {
                    if (metodo.getName().equals("createNativeQuery")) {
                        sql = (String) args[0];
                        return consulta;
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });
        return dao;
    }

    private static EventoViaje evento(byte[] foto) throws Exception {
        Guia guia = new Guia(new Empresa(), new Vehiculo(), new Chofer(), new Rubro(), null,
                BigDecimal.TEN, BigDecimal.ONE, Instant.parse("2026-10-20T10:00:00Z"),
                Instant.parse("2026-10-20T17:00:00Z"), "Origen", null, null, "Destino", null, null);
        var id = Guia.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(guia, 77L);
        return new EventoViaje(guia, "evento-0001", TipoEventoViaje.INICIO,
                Instant.parse("2026-10-20T10:01:00Z"), null, null, null, foto);
    }
}
