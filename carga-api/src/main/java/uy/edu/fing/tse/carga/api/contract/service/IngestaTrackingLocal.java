package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;

/**
 * Ingesta de posiciones del nodo Tracking.
 * La recepcion se desacopla del procesamiento: encola y retorna (RNF 4.1 de la letra).
 */
@Local
public interface IngestaTrackingLocal {

    void encolarPosicion(PosicionDTO posicion);
}
