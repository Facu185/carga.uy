package uy.edu.fing.tse.carga.api;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.PosicionDTO;

/**
 * Ingesta de posiciones del nodo Tracking.
 * La recepcion se desacopla del procesamiento: encola y retorna (RNF 4.1 de la letra).
 */
@Local
public interface IngestaTrackingLocal {

    void encolarPosicion(PosicionDTO posicion);
}
