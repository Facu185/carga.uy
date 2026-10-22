package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.EditarPerfilEmpresaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** Perfil propio F10: principal responsable activo y empresa activa comprobados en EJB. */
@Local
public interface PerfilEmpresaServicioLocal {
    EmpresaDTO consultar(Long id) throws NegocioException;

    EmpresaDTO modificar(Long id, EditarPerfilEmpresaDTO datos) throws NegocioException;
}
