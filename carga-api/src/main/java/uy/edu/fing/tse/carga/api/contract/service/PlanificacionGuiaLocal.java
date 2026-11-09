package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.planificacion.*;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/** Vista previa de CU2; no registra guias ni cambia su autorizacion. */
@Local
public interface PlanificacionGuiaLocal {
    List<SugerenciaCalleDTO> sugerir(String texto) throws NegocioException;
    DireccionMapaDTO resolver(SugerenciaCalleDTO seleccion) throws NegocioException;
    RutaPreviaDTO calcular(SolicitudRutaDTO solicitud) throws NegocioException;
}
