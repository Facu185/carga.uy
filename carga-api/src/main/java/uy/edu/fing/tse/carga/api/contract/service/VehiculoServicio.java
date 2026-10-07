package uy.edu.fing.tse.carga.api.contract.service;

import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

public interface VehiculoServicio {
    VehiculoDTO agregar(VehiculoDTO vehiculo) throws NegocioException;
    VehiculoDTO modificar(Long id, VehiculoDTO vehiculo) throws NegocioException;
    void darDeBaja(Long id) throws NegocioException;
    List<VehiculoDTO> listar(Long empresaId);
}
