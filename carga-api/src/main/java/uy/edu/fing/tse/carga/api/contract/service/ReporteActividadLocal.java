package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** ADR-0020: agregados solo para AUTORIDAD; fechas locales inclusivas o ambas ausentes. */
@Local
public interface ReporteActividadLocal {
    ReporteActividadDTO consultar(String desde, String hasta) throws NegocioException;
}
