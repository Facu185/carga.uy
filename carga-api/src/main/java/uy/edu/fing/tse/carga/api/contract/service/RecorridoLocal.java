package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/**
 * Recorrido de un vehiculo segun las posiciones del nodo Tracking (frente P4).
 * Lo consume la deteccion de incumplimientos (frente P5) para la regla de desvio de ruta.
 * Sin rol a proposito: P5 lo llama desde procesos sin usuario. La API REST si exige rol.
 * CONTRATO: docs/contratos.md
 */
@Local
public interface RecorridoLocal {

    /**
     * @param desde ISO-8601 con zona (2026-10-18T08:00:00-03:00 o 2026-10-18T11:00:00Z)
     * @param hasta ISO-8601 con zona, excluido. Como mucho 7 dias despues de desde.
     */
    RecorridoVehiculoDTO recorrido(String matricula, String desde, String hasta) throws NegocioException;

    /** El recorrido del vehiculo de la guia durante el periodo de la guia [inicio, fin). */
    RecorridoVehiculoDTO recorridoDeGuia(Long guiaId) throws NegocioException;
}
