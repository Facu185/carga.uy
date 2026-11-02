package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import uy.edu.fing.tse.carga.api.contract.service.DeteccionLocal;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Revisa cada 5 minutos la actividad de tracking reciente (viaje sin guia y vehiculo sin habilitacion,
 * ADR-0022). Por agenda y no por posicion: a 100 posiciones/s evaluar cada una cargaria la ingesta, y
 * el hecho (un vehiculo en un dia) es el mismo para todas sus posiciones. La ventana de 10 minutos se
 * solapa con la anterior a proposito: la deduplicacion por hecho absorbe el solapamiento.
 * Se apaga con FISCALIZACION_AGENDA_ACTIVA=false (por ejemplo, durante la prueba de performance).
 */
@Singleton
public class AgendaDeteccionBean {

    static final int VENTANA_MINUTOS = 10;
    private static final Logger LOG = Logger.getLogger(AgendaDeteccionBean.class.getName());
    private static final boolean ACTIVA = !"false".equalsIgnoreCase(System.getenv("FISCALIZACION_AGENDA_ACTIVA"));

    @EJB
    DeteccionLocal deteccion;

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    void revisarTracking() {
        if (!ACTIVA) {
            return;
        }
        try {
            int nuevos = deteccion.evaluarActividadTracking(VENTANA_MINUTOS);
            if (nuevos > 0) {
                LOG.info("Deteccion por tracking: " + nuevos + " caso(s) nuevo(s)");
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo revisar la actividad de tracking; se reintenta en la proxima vuelta", e);
        }
    }
}
