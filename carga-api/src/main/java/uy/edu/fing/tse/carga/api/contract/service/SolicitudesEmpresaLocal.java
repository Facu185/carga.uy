package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.empresa.NuevaSolicitudEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.SolicitudAltaEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.VinculoEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.RechazoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import java.util.List;

/** Altas de empresas y autorizaciones de personas sobre empresas, ambas aprobables y auditables. */
@Local
public interface SolicitudesEmpresaLocal {
    SolicitudAltaEmpresaDTO solicitarAlta(NuevaSolicitudEmpresaDTO datos) throws NegocioException;
    SolicitudAltaEmpresaDTO miSolicitudAlta();
    VinculoEmpresaDTO solicitarVinculo(Long empresaId) throws NegocioException;
    List<VinculoEmpresaDTO> misVinculos();
    List<SolicitudAltaEmpresaDTO> listarSolicitudes(String estado) throws NegocioException;
    List<VinculoEmpresaDTO> listarVinculos(String estado) throws NegocioException;
    SolicitudAltaEmpresaDTO aprobarAlta(Long id) throws NegocioException;
    SolicitudAltaEmpresaDTO rechazarAlta(Long id, RechazoDTO datos) throws NegocioException;
    VinculoEmpresaDTO aprobarVinculo(Long id) throws NegocioException;
    VinculoEmpresaDTO transferirResponsabilidad(Long id, RechazoDTO datos) throws NegocioException;
    VinculoEmpresaDTO rechazarVinculo(Long id, RechazoDTO datos) throws NegocioException;
    void darDeBajaEmpresa(Long empresaId, RechazoDTO datos) throws NegocioException;
}
