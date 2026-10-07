package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoPesajeDTO;

import java.util.List;

/**
 * Consulta al nodo periferico Balanzas (integracion sincronica request-response).
 * CONTRATO con el nodo: ver docs/contratos.md
 */
@Local
public interface PesajeLocal {

    /**
     * Pesadas de una matricula en una fecha (ISO-8601).
     * Devuelve lista vacia si el nodo no responde; usar {@link #consultarPesadas}
     * cuando haya que distinguir "sin pesadas" de "nodo caido".
     */
    List<PesadaDTO> obtenerPesadas(String matricula, String fecha);

    /** Igual que {@link #obtenerPesadas}, pero informa si el nodo respondio y cuanto tardo. */
    ResultadoPesajeDTO consultarPesadas(String matricula, String fecha);
}
