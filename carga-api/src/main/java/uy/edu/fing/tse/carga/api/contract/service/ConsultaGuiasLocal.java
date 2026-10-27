package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.guia.ConsultaGuiaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.PaginaConsultaGuiasDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** C14 P2: lectura administrativa sin vinculo; no concede creacion ni mutaciones. */
@Local
public interface ConsultaGuiasLocal {
    PaginaConsultaGuiasDTO consultar(Long empresaId, String desde, String hasta, String estado,
            Integer pagina, Integer tamano) throws NegocioException;

    ConsultaGuiaDTO detalle(Long empresaId, Long guiaId) throws NegocioException;
}
