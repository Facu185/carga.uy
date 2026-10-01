package uy.edu.fing.tse.carga.api;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.DatosSesionDTO;
import uy.edu.fing.tse.carga.api.dto.LoginDTO;
import uy.edu.fing.tse.carga.api.dto.SesionDTO;

/** Ingreso al backoffice y validacion de las sesiones emitidas por el central. */
@Local
public interface AutenticacionLocal {

    /**
     * Mecanismo interno (base de datos o LDAP, segun AUTENTICACION_INTERNA).
     * @throws CredencialesInvalidasException usuario o clave incorrectos
     * @throws CuentaBloqueadaException demasiados intentos fallidos
     * @throws ServicioNoDisponibleException el directorio LDAP no respondio
     */
    SesionDTO login(LoginDTO credenciales);

    /** null si el token no es valido, vencio o fue revocado (baja, cambio de rol o de clave). */
    DatosSesionDTO validarToken(String token);
}
