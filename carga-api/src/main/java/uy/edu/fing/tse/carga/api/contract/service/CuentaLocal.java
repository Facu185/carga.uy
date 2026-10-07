package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.cuenta.CambioPasswordDTO;
import uy.edu.fing.tse.carga.api.dto.cuenta.SesionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** Lo que cada persona hace sobre su propia sesion o cuenta. */
@Local
public interface CuentaLocal {

    /** Cambiar la clave cierra las demas sesiones: devuelve una sesion nueva para seguir trabajando. */
    SesionDTO cambiarMiPassword(CambioPasswordDTO datos) throws NegocioException;

    /** "Seguir conectado": token nuevo antes de que venza el actual. */
    SesionDTO renovar() throws NegocioException;
}
