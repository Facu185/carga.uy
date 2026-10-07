package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.nodo.EstadoNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.MetricasDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.RecorridoDTO;

import java.util.List;

/**
 * Observabilidad de los nodos perifericos (frente P4).
 * Es solo de lectura: no modifica nada del sistema.
 */
@Local
public interface MonitorLocal {

    /** Contadores de la ingesta de posiciones. */
    MetricasDTO metricas();

    /** Estado de cada nodo, verificado contra el nodo real en el momento de la consulta. */
    List<EstadoNodoDTO> estadoNodos();

    /** Ultima posicion y recorrido reciente de cada vehiculo que esta reportando. */
    List<RecorridoDTO> mapaFlota(int puntosPorVehiculo);
}
