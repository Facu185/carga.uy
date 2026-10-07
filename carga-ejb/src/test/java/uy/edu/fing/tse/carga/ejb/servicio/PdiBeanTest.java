package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.PersonaDTO;
import uy.edu.fing.tse.carga.ejb.integracion.CircuitBreaker;
import uy.edu.fing.tse.carga.ejb.observabilidad.Metricas;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdiBeanTest {

    private int llamadas;
    private boolean falla;
    private PdiBean pdi;
    private Metricas metricas;

    @BeforeEach
    void preparar() {
        pdi = new PdiBean() {
            @Override
            PersonaDTO invocar(String cedula) throws Exception {
                llamadas++;
                if (falla) {
                    throw new TimeoutException("la PDI no contesto");
                }
                return new PersonaDTO(cedula, "Ana Perez", "1990-05-20");
            }
        };
        metricas = new Metricas();
        pdi.metricas = metricas;
        pdi.configurar("http://pdi/ServicioDnicImpl?wsdl", Duration.ofMillis(100), 1,
                new CircuitBreaker("PDI", 2, Duration.ofMinutes(1), Clock.systemUTC()));
    }

    @Test
    void devuelveLaPersonaCuandoLaPdiResponde() {
        assertEquals("Ana Perez", pdi.obtenerPersona("12345672").getNombreCompleto());
    }

    @Test
    void anteFallasReintentaYDevuelveNullSegunElContrato() {
        falla = true;
        assertNull(pdi.obtenerPersona("12345672"));
        assertEquals(2, llamadas); // el intento y un reintento
    }

    @Test
    void conFallasSostenidasElCircuitoSeAbreYNoSigueLlamando() {
        falla = true;
        pdi.obtenerPersona("12345672");
        pdi.obtenerPersona("12345672");
        assertEquals(CircuitBreaker.Estado.ABIERTO, pdi.circuito.estado());

        int antes = llamadas;
        assertNull(pdi.obtenerPersona("12345672"));
        assertEquals(antes, llamadas);
        String exportado = metricas.exportarPrometheus();
        assertTrue(exportado.contains("carga_pdi_consultas_total{resultado=\"falla\"} 2"));
        assertTrue(exportado.contains("carga_pdi_consultas_total{resultado=\"circuito_abierto\"} 1"));
    }
}
