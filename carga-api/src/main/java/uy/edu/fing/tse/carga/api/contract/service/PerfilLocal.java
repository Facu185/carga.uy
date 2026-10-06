package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.cuenta.PerfilDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** Perfil de quien ingreso con gub.uy. */
@Local
public interface PerfilLocal {

    PerfilDTO miPerfil() throws NegocioException;
}
