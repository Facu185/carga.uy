package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoLoteDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

@Local
public interface IngestaTrackingLocal {

    void encolarPosicion(PosicionDTO posicion) throws NegocioException;
    ResultadoLoteDTO encolarLote(List<PosicionDTO> posiciones) throws NegocioException;
}
