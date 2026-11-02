package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.publico.EstadisticasPublicasDTO;
import uy.edu.fing.tse.carga.api.dto.publico.ValidacionEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.publico.ValidacionVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/**
 * Frontoffice publico (letra, "Funcionalidades Ciudadanos - Frontoffice (publico)", items 2 y 3).
 * Sin rol: lo usa cualquiera, sin sesion. Nunca devuelve datos de personas ni listados de empresas
 * o vehiculos: la validacion responde por un identificador que la persona ya conoce, y las
 * estadisticas salen agregadas con el umbral del ADR-0021.
 * CONTRATO: docs/contratos.md
 */
@Local
public interface InformacionPublicaLocal {

    /** Si un vehiculo esta habilitado para circular hoy: permiso nacional e ITV vigentes. */
    ValidacionVehiculoDTO validarVehiculo(String matricula) throws NegocioException;

    /** Si una empresa de transporte esta habilitada, por su numero de empresa. */
    ValidacionEmpresaDTO validarEmpresa(String numeroEmpresa) throws NegocioException;

    /** Cifras agregadas de los ultimos 12 meses, sin celdas que permitan identificar a nadie. */
    EstadisticasPublicasDTO estadisticas();
}
