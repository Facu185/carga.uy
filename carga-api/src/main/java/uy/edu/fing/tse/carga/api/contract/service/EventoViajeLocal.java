package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;
import uy.edu.fing.tse.carga.api.dto.viaje.ResultadoEventoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/**
 * Registro de eventos de viaje desde el componente movil (caso de uso critico 3, frente P3).
 * El chofer sale del token: ningun metodo recibe la cedula ni el id del chofer.
 */
@Local
public interface EventoViajeLocal {

    /**
     * Registra el evento y actualiza el estado de la guia. Es idempotente por idEvento: reenviar el
     * mismo evento devuelve nuevo=false sin duplicarlo. Al registrar un FIN se avisa a P4 para que
     * obtenga los pesajes del viaje (ADR-0010).
     *
     * @throws NegocioException datos invalidos
     * @throws uy.edu.fing.tse.carga.api.exception.RecursoAjenoException la guia no es del chofer
     * @throws uy.edu.fing.tse.carga.api.exception.ConflictoException el estado del viaje no admite el evento
     */
    ResultadoEventoDTO registrar(EventoViajeDTO evento) throws NegocioException;

    /** Eventos de una guia del chofer autenticado, en orden cronologico, sin las fotos. */
    List<EventoViajeDTO> eventosDeGuia(Long guiaId) throws NegocioException;
}
