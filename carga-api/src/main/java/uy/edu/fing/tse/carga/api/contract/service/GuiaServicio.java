package uy.edu.fing.tse.carga.api.contract.service;

import uy.edu.fing.tse.carga.api.dto.guia.CrearGuiaCommand;
import uy.edu.fing.tse.carga.api.dto.guia.GuiaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import java.util.List;

/** Registra una guia y su asignacion de forma atomica. */
public interface GuiaServicio {
    GuiaDTO crear(CrearGuiaCommand comando) throws NegocioException;
    List<GuiaDTO> guiasDelChoferAutenticado() throws NegocioException;
}
