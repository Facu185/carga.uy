package uy.edu.fing.tse.carga.api.dto.viaje;

import java.io.Serializable;

/**
 * Respuesta al registrar un evento. nuevo=false significa que ese idEvento ya estaba registrado
 * (reenvio del dispositivo): no es un error, la app lo da por enviado.
 */
public class ResultadoEventoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean nuevo;
    /** Estado de la guia despues de procesar el evento (ASIGNADA, EN_CURSO o FINALIZADA). */
    private String estadoGuia;
    private EventoViajeDTO evento;

    public ResultadoEventoDTO() {
    }

    public ResultadoEventoDTO(boolean nuevo, String estadoGuia, EventoViajeDTO evento) {
        this.nuevo = nuevo;
        this.estadoGuia = estadoGuia;
        this.evento = evento;
    }

    public boolean isNuevo() { return nuevo; }
    public void setNuevo(boolean nuevo) { this.nuevo = nuevo; }

    public String getEstadoGuia() { return estadoGuia; }
    public void setEstadoGuia(String estadoGuia) { this.estadoGuia = estadoGuia; }

    public EventoViajeDTO getEvento() { return evento; }
    public void setEvento(EventoViajeDTO evento) { this.evento = evento; }
}
