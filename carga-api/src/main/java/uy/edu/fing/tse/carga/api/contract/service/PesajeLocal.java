package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;

import java.util.List;

/**
 * Consulta sincronica (request-response) al nodo periferico Balanzas.
 * Si el nodo no responde, devuelve lista vacia: la indisponibilidad de un nodo
 * no debe provocar la indisponibilidad del componente central (RNF 4.3 de la letra).
 */
@Local
public interface PesajeLocal {

    List<PesadaDTO> obtenerPesadas(String matricula, String fecha);
}
