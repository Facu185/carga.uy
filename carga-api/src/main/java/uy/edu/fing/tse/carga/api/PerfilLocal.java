package uy.edu.fing.tse.carga.api;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.PerfilDTO;

/** Perfil de quien ingreso con gub.uy. */
@Local
public interface PerfilLocal {

    PerfilDTO miPerfil() throws NegocioException;
}
