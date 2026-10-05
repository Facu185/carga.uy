package uy.edu.fing.tse.carga.api;

import uy.edu.fing.tse.carga.api.dto.CrearGuiaCommand;
import uy.edu.fing.tse.carga.api.dto.GuiaDTO;

/** Registra una guia y su asignacion de forma atomica. */
public interface GuiaServicio {
    GuiaDTO crear(CrearGuiaCommand comando) throws NegocioException;
}
