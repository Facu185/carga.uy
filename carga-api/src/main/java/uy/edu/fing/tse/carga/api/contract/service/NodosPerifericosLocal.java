package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.nodo.ActivacionNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.DatosNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.NodoPerifericoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.util.List;

/**
 * Caso de uso "Gestionar nodos perifericos" (Administrador, frente P4, ADR-0009).
 * Solo administradores. Cada cambio queda en la pista de auditoria.
 */
@Local
public interface NodosPerifericosLocal {

    /** Todos los nodos, con el estado de los activos verificado en el momento. */
    List<NodoPerifericoDTO> listar();

    /** Alta de un nodo Balanzas. Nace desactivado: se activa explicitamente. */
    NodoPerifericoDTO crear(DatosNodoDTO datos) throws NegocioException;

    /** Cambia el nombre y, si es un nodo Balanzas, la URL. El cambio de URL rige en la consulta siguiente. */
    NodoPerifericoDTO modificar(Long id, DatosNodoDTO datos) throws NegocioException;

    NodoPerifericoDTO cambiarActivacion(Long id, ActivacionNodoDTO activacion) throws NegocioException;
}
