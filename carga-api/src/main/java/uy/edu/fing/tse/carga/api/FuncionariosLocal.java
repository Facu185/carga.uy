package uy.edu.fing.tse.carga.api;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.CambioDependenciaDTO;
import uy.edu.fing.tse.carga.api.dto.FuncionarioDTO;
import uy.edu.fing.tse.carga.api.dto.NuevoFuncionarioDTO;

import java.util.List;

/** Funcionarios del MTOP habilitados. Solo administradores. */
@Local
public interface FuncionariosLocal {

    List<FuncionarioDTO> listar();

    /** Alta manual (plan B): lo habitual es aprobar una solicitud de acceso. */
    FuncionarioDTO crear(NuevoFuncionarioDTO datos) throws NegocioException;

    FuncionarioDTO cambiarEstado(Long id, boolean activo) throws NegocioException;

    FuncionarioDTO cambiarDependencia(Long id, CambioDependenciaDTO datos) throws NegocioException;
}
