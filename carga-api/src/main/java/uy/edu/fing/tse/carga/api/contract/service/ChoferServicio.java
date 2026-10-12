package uy.edu.fing.tse.carga.api.contract.service;

import uy.edu.fing.tse.carga.api.dto.guia.ChoferDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

public interface ChoferServicio {
    ChoferDTO agregar(ChoferDTO chofer) throws NegocioException;
    ChoferDTO modificar(Long id, ChoferDTO chofer) throws NegocioException;
    void darDeBaja(Long id) throws NegocioException;
    void darDeAlta(Long id) throws NegocioException;
    List<ChoferDTO> listar(Long empresaId) throws NegocioException;
}
