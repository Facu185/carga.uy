package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.ViajeFinalizadoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/**
 * Lo llama el frente P3 al registrar el fin de un viaje: dispara la obtencion de pesajes de ese
 * vehiculo (caso de uso critico 4, "se dispara al finalizar un viaje"). ADR-0010.
 *
 * Es asincronico: el aviso se encola dentro de la transaccion de quien llama (si esa transaccion
 * se deshace, el aviso no sale) y la consulta al nodo Balanzas ocurre despues. Quien registra el
 * fin del viaje no espera al nodo ni falla si el nodo esta caido.
 */
@Local
public interface AvisoFinDeViajeLocal {

    /** @throws NegocioException si falta la matricula o el fin, o las fechas no son ISO-8601 con zona */
    void avisar(ViajeFinalizadoDTO viaje) throws NegocioException;
}
