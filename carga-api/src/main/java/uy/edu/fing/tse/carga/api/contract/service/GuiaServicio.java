package uy.edu.fing.tse.carga.api.contract.service;

import uy.edu.fing.tse.carga.api.dto.guia.CrearGuiaCommand;
import uy.edu.fing.tse.carga.api.dto.guia.GuiaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.PaginaGuiasEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.ChoferDTO;
import uy.edu.fing.tse.carga.api.dto.guia.RubroDTO;
import uy.edu.fing.tse.carga.api.dto.guia.TipoCargaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import java.util.List;

/** Registra una guia y su asignacion de forma atomica. */
public interface GuiaServicio {
    GuiaDTO crear(CrearGuiaCommand comando) throws NegocioException;
    List<GuiaDTO> guiasDelChoferAutenticado() throws NegocioException;
    PaginaGuiasEmpresaDTO guiasDeLaEmpresaResponsable(Long empresaId, int pagina, int tamano) throws NegocioException;
    List<VehiculoDTO> vehiculosDeLaEmpresaAutenticada() throws NegocioException;
    List<ChoferDTO> choferesDeLaEmpresaAutenticada() throws NegocioException;
    List<RubroDTO> rubros();
    List<TipoCargaDTO> tiposCarga();
}
