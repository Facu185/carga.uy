package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ParametroReglaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/**
 * Parametrizacion de las reglas de fiscalizacion sin redesplegar (backoffice, administradores). Los
 * cambios rigen desde la proxima evaluacion y quedan en la auditoria con su motivo.
 */
@Local
public interface ParametrosReglasLocal {

    List<ParametroReglaDTO> listar();

    ParametroReglaDTO modificar(String clave, String valor, String motivo) throws NegocioException;
}
