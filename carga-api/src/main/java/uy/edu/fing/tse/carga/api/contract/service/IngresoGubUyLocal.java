package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.RedireccionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/**
 * Ingreso con gub.uy (OpenID Connect, Authorization Code + PKCE) para ciudadanos y funcionarios.
 * La capa web solo traduce estos resultados a redirecciones y cookies.
 */
@Local
public interface IngresoGubUyLocal {

    /** destino: "web" o "movil". La transaccion (state, nonce, PKCE) va firmada en una cookie. */
    RedireccionDTO iniciar(String destino) throws NegocioException;

    /** Vuelta de gub.uy: redirige al frontend con la sesion (o con el error) en el fragmento de la URL. */
    RedireccionDTO completar(String code, String state, String error, String transaccion);

    /** true si la cookie de la transaccion tiene que ser Secure (redirect por https). */
    boolean cookieSegura();
}
