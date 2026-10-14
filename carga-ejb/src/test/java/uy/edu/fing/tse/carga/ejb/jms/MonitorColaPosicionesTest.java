package uy.edu.fing.tse.carga.ejb.jms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class MonitorColaPosicionesTest {

    private long pendientes;
    private int lecturas;
    private long reloj;
    private MonitorColaPosiciones monitor;

    @BeforeEach
    void preparar() {
        pendientes = 0;
        lecturas = 0;
        reloj = 0;
        monitor = new MonitorColaPosiciones() {
            @Override
            long pendientes() {
                lecturas++;
                return pendientes;
            }

            @Override
            long ahoraMs() {
                return reloj;
            }
        };
        monitor.configurar(100, 50, 1000, 30);
    }

    @Test
    void porDebajoDelUmbralNoEstaSaturada() {
        pendientes = 99;
        assertFalse(monitor.saturada());
    }

    @Test
    void alLlegarAlUmbralAltoSeSatura() {
        pendientes = 100;
        assertTrue(monitor.saturada());
        assertEquals(30, monitor.reintentarEnSegundos());
    }

    @Test
    void saturadaSigueRechazandoHastaBajarAlUmbralBajo() {
        pendientes = 150;
        assertTrue(monitor.saturada());

        pendientes = 70;
        avanzar();
        assertTrue(monitor.saturada());

        pendientes = 50;
        avanzar();
        assertFalse(monitor.saturada());

        pendientes = 70;
        avanzar();
        assertFalse(monitor.saturada());
    }

    @Test
    void siNoSePuedeMedirNoRechaza() {
        pendientes = -1;
        assertFalse(monitor.saturada());
    }

    @Test
    void siNoSePuedeMedirMantieneElEstadoAnterior() {
        pendientes = 150;
        assertTrue(monitor.saturada());
        pendientes = -1;
        avanzar();
        assertTrue(monitor.saturada());
    }

    @Test
    void leeLaColaComoMuchoUnaVezPorSegundo() {
        monitor.saturada();
        monitor.saturada();
        reloj += 999;
        monitor.saturada();
        assertEquals(1, lecturas);

        reloj += 1;
        monitor.saturada();
        assertEquals(2, lecturas);
    }

    private void avanzar() {
        reloj += 1000;
    }
}
