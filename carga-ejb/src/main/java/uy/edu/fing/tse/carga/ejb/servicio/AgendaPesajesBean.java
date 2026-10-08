package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import uy.edu.fing.tse.carga.api.contract.service.SincronizacionPesajesLocal;
import uy.edu.fing.tse.carga.ejb.persistencia.PesadaDAO;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Disparo periodico de "Obtener pesajes del nodo Balanzas": el actor Sistema del caso de uso.
 * Cada 15 minutos sincroniza los vehiculos con una guia vigente en las ultimas 24 horas (una
 * balanza puede informar la pesada un rato despues de que termino el viaje).
 *
 * NOT_SUPPORTED: cada sincronizacion corre en su propia transaccion. Si fuera una sola para todos
 * los vehiculos, la falla de uno desharia lo guardado de los demas.
 *
 * Se apaga con PESAJES_AGENDA_ACTIVA=false (por ejemplo, para que no ensucie una demostracion).
 * El otro disparo del caso de uso, "al registrar el fin del viaje", depende del frente P3.
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AgendaPesajesBean {

    private static final Logger LOG = Logger.getLogger(AgendaPesajesBean.class.getName());
    static final Duration VENTANA = Duration.ofHours(24);

    @EJB
    SincronizacionPesajesLocal sincronizacion;

    @EJB
    PesadaDAO pesadas;

    @Schedule(minute = "*/15", hour = "*", persistent = false, info = "Sincronizacion de pesajes")
    public void sincronizarPeriodicamente() {
        if (!activa()) {
            return;
        }
        int ok = sincronizarVehiculosConGuia(Instant.now());
        LOG.info("Sincronizacion periodica de pesajes: " + ok + " vehiculo(s) sincronizado(s)");
    }

    /** @return cuantos vehiculos se sincronizaron sin error */
    int sincronizarVehiculosConGuia(Instant ahora) {
        List<String> matriculas = pesadas.matriculasConGuiaEntre(ahora.minus(VENTANA), ahora.plusSeconds(1));
        String hoy = LocalDate.ofInstant(ahora, PesadaDAO.ZONA).toString();
        int ok = 0;
        for (String matricula : matriculas) {
            try {
                sincronizacion.sincronizar(matricula, hoy);
                ok++;
            } catch (Exception e) {
                // Un vehiculo con problemas no frena a los demas
                LOG.warning("No se pudo sincronizar los pesajes de " + matricula + ": " + e.getMessage());
            }
        }
        return ok;
    }

    boolean activa() {
        return !"false".equalsIgnoreCase(System.getenv().getOrDefault("PESAJES_AGENDA_ACTIVA", "true").trim());
    }
}
