package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.cuenta.CambioRolDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.CuentaInternaDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.NuevaCuentaInternaDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.PasswordTemporalDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/** Gestion de cuentas del backoffice (autoridades y administradores). Solo administradores. */
@Local
public interface CuentasInternasLocal {

    List<CuentaInternaDTO> listar();

    CuentaInternaDTO crear(NuevaCuentaInternaDTO datos) throws NegocioException;

    CuentaInternaDTO cambiarEstado(Long id, boolean activo) throws NegocioException;

    CuentaInternaDTO cambiarRol(Long id, CambioRolDTO datos) throws NegocioException;

    PasswordTemporalDTO resetearPassword(Long id) throws NegocioException;

    CuentaInternaDTO desbloquear(Long id) throws NegocioException;
}
