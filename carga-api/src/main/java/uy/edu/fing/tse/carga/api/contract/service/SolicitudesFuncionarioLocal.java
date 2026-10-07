package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.funcionario.NuevaSolicitudDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.RechazoDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.SolicitudFuncionarioDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/**
 * Acceso de funcionarios: la persona entra con gub.uy y pide acceso indicando su dependencia;
 * un administrador aprueba o rechaza con motivo.
 */
@Local
public interface SolicitudesFuncionarioLocal {

    SolicitudFuncionarioDTO solicitar(NuevaSolicitudDTO datos) throws NegocioException;

    /** La ultima solicitud de quien esta logueado, o null si nunca pidio acceso. */
    SolicitudFuncionarioDTO miSolicitud();

    /** estado null o vacio = todas. */
    List<SolicitudFuncionarioDTO> listar(String estado) throws NegocioException;

    SolicitudFuncionarioDTO aprobar(Long id) throws NegocioException;

    SolicitudFuncionarioDTO rechazar(Long id, RechazoDTO datos) throws NegocioException;
}
