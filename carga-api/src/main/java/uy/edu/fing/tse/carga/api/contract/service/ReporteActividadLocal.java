package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
import uy.edu.fing.tse.carga.api.dto.guia.TipoCargaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/** ADR-0020: agregados solo para AUTORIDAD; fechas locales inclusivas o ambas ausentes. */
@Local
public interface ReporteActividadLocal {
    ReporteActividadDTO consultar(String desde, String hasta) throws NegocioException;

    /** Tipo opcional solo para actividad; ausente equivale a todos, incluido sin tipo. */
    ReporteActividadDTO consultar(String desde, String hasta, String tipoCargaId) throws NegocioException;

    /** ADR-0023: viajes por instante FIN aceptado, no por creacion o recepcion. */
    ReporteActividadDTO viajesFinalizados(String desde, String hasta, String tipoCargaId) throws NegocioException;

    /** Catalogo de lectura para AUTORIDAD; no cambia permisos de creacion de guias. */
    List<TipoCargaDTO> tiposCarga();
}
