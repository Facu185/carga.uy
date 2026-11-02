package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CasoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.PaginaCasosDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/**
 * Infracciones de una empresa para su responsable (letra, frontoffice de la empresa, punto 5).
 * La empresa se valida en el servidor: hace falta un vinculo RESPONSABLE ACTIVO con ella.
 */
@Local
public interface InfraccionesEmpresaLocal {

    PaginaCasosDTO infracciones(Long empresaId, Integer pagina, Integer tamano) throws NegocioException;

    CasoDTO infraccion(Long empresaId, Long casoId) throws NegocioException;

    /**
     * Un descargo por caso, dentro del plazo. Pasa el caso de NOTIFICADA a EN_DESCARGO.
     *
     * @throws uy.edu.fing.tse.carga.api.exception.ConflictoException si ya hay descargo, vencio el plazo o
     *         el caso ya esta resuelto (409)
     */
    CasoDTO presentarDescargo(Long empresaId, Long casoId, String fundamento) throws NegocioException;
}
