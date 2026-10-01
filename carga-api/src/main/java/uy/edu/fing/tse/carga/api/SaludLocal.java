package uy.edu.fing.tse.carga.api;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.EstadoSaludDTO;

/** Verificacion de estado del central y de lo que necesita para atender (Seccion 4.4, punto 8). */
@Local
public interface SaludLocal {

    /**
     * estado UP si puede atender pedidos; DOWN si no (por ejemplo, sin base de datos).
     * Un sistema externo caido deja el componente en DEGRADADO pero el central sigue UP:
     * es justamente el modo degradado que pide la letra.
     */
    EstadoSaludDTO verificar();
}
