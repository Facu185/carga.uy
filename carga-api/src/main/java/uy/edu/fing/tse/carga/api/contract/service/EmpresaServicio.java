package uy.edu.fing.tse.carga.api.contract.service;

import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/** Capa de negocio de empresas transportistas. */
public interface EmpresaServicio {

    EmpresaDTO agregar(EmpresaDTO empresa) throws NegocioException;

    List<EmpresaDTO> listar();

    List<EmpresaDTO> buscarPorNombre(String nombre);
}
