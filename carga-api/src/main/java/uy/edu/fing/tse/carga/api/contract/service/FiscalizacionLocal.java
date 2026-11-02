package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CasoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.PaginaCasosDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ReporteFiscalizacionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/**
 * Gestion de casos de fiscalizacion por el funcionario (CU6, frente P5) e indicadores para la
 * autoridad. CONTRATO: docs/contratos.md, ADR-0022.
 */
@Local
public interface FiscalizacionLocal {

    /** Bandeja de trabajo. estado y tipo son opcionales; pagina desde 0, tamano entre 1 y 100. */
    PaginaCasosDTO bandeja(String estado, String tipo, Integer pagina, Integer tamano) throws NegocioException;

    CasoDTO caso(Long casoId) throws NegocioException;

    /**
     * Confirma o desestima la infraccion con un fundamento obligatorio. Solo desde NOTIFICADA con el
     * plazo de descargo vencido, o desde EN_DESCARGO; desestimar se admite siempre que no este resuelto.
     *
     * @throws uy.edu.fing.tse.carga.api.exception.ConflictoException si el estado no lo admite (409)
     */
    CasoDTO resolver(Long casoId, boolean confirmar, String fundamento) throws NegocioException;

    /** Indicadores por tipo de infraccion entre dos dias de Uruguay (aaaa-mm-dd, incluidos). Autoridad. */
    ReporteFiscalizacionDTO reporte(String desde, String hasta) throws NegocioException;
}
