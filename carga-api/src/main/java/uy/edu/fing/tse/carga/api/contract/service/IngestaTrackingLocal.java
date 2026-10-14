package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

@Local
public interface IngestaTrackingLocal {
    void encolarPosicion(PosicionDTO posicion) throws NegocioException;
}
