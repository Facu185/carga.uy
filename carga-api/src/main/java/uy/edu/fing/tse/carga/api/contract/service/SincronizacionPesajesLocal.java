package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoSincronizacionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/**
 * Caso de uso critico "Obtener pesajes del nodo Balanzas" (frente P4): trae las pesadas del nodo,
 * las vincula con la guia vigente, las guarda sin duplicar y avisa de cada pesada nueva por la cola
 * java:/jms/queue/pesadas (la consume la deteccion de incumplimientos, frente P5).
 * CONTRATO: docs/contratos.md
 */
@Local
public interface SincronizacionPesajesLocal {

    /**
     * Idempotente: sincronizar dos veces la misma matricula y fecha no duplica pesadas ni avisos.
     *
     * @param fecha ISO-8601 (aaaa-mm-dd); vacia o null es el dia de hoy en Uruguay
     * @throws NegocioException si falta la matricula o la fecha no es valida
     */
    ResultadoSincronizacionDTO sincronizar(String matricula, String fecha) throws NegocioException;

    /** Pesadas ya guardadas de una matricula en una fecha, ordenadas por instante. */
    List<PesadaRegistradaDTO> registradas(String matricula, String fecha) throws NegocioException;
}
