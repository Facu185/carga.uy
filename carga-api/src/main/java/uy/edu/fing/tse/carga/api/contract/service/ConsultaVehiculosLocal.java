package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.vehiculo.PaginaConsultaVehiculosDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** C13: lectura FUNCIONARIO/ADMINISTRADOR sin vinculo; no concede operaciones del ABM. */
@Local
public interface ConsultaVehiculosLocal {
    PaginaConsultaVehiculosDTO consultar(Long empresaId, String matricula, Integer pagina, Integer tamano)
            throws NegocioException;
}
