package uy.edu.fing.tse.carga.ejb.integracion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Corte ante fallas sostenidas.
 * - CERRADO: las llamadas pasan. Tras N fallas seguidas se abre.
 * - ABIERTO: no se llama al sistema externo (respuesta inmediata en modo degradado) durante un tiempo.
 * - SEMI_ABIERTO: pasado ese tiempo se deja pasar UNA llamada de prueba; si anda se cierra, si falla se vuelve a abrir.
 * El estado es por instancia del central: cada nodo se protege a si mismo.
 */
public class CircuitBreaker {

    public enum Estado { CERRADO, ABIERTO, SEMI_ABIERTO }

    private static final Logger LOG = Logger.getLogger(CircuitBreaker.class.getName());

    private final String nombre;
    private final int umbralFallas;
    private final Duration tiempoAbierto;
    private final Clock reloj;

    private Estado estado = Estado.CERRADO;
    private int fallasSeguidas;
    private Instant abiertoDesde;
    private boolean pruebaEnCurso;

    public CircuitBreaker(String nombre, int umbralFallas, Duration tiempoAbierto, Clock reloj) {
        this.nombre = nombre;
        this.umbralFallas = umbralFallas;
        this.tiempoAbierto = tiempoAbierto;
        this.reloj = reloj;
    }

    public synchronized boolean permiteLlamada() {
        if (estado == Estado.ABIERTO && !reloj.instant().isBefore(abiertoDesde.plus(tiempoAbierto))) {
            cambiarA(Estado.SEMI_ABIERTO);
        }
        if (estado == Estado.SEMI_ABIERTO) {
            if (pruebaEnCurso) {
                return false;
            }
            pruebaEnCurso = true;
            return true;
        }
        return estado == Estado.CERRADO;
    }

    public synchronized void registrarExito() {
        fallasSeguidas = 0;
        pruebaEnCurso = false;
        if (estado != Estado.CERRADO) {
            cambiarA(Estado.CERRADO);
        }
    }

    public synchronized void registrarFalla() {
        fallasSeguidas++;
        pruebaEnCurso = false;
        if (estado == Estado.SEMI_ABIERTO || fallasSeguidas >= umbralFallas) {
            abiertoDesde = reloj.instant();
            if (estado != Estado.ABIERTO) {
                cambiarA(Estado.ABIERTO);
            }
        }
    }

    public synchronized Estado estado() {
        return estado;
    }

    private void cambiarA(Estado nuevo) {
        LOG.warning("Circuito " + nombre + ": " + estado + " -> " + nuevo);
        estado = nuevo;
    }
}
