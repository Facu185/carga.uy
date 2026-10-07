package uy.edu.fing.tse.carga.ejb.seguridad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;
import uy.edu.fing.tse.carga.ejb.persistencia.ChoferDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.CuentaInternaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.FuncionarioDAO;

/**
 * Revocacion inmediata: un token bien firmado y sin vencer igual se rechaza si, desde que se emitio,
 * la cuenta fue dada de baja, le cambiaron el rol o cambio la clave.
 * Cuesta una consulta por clave unica en cada pedido autenticado; a cambio no hay que esperar
 * a que el token venza para cortar un acceso.
 */
@ApplicationScoped
public class VerificadorSesiones {

    @Inject
    CuentaInternaDAO cuentas;

    @Inject
    FuncionarioDAO funcionarios;

    @Inject
    ChoferDAO choferes;

    public boolean sigueVigente(TokenService.DatosToken datos) {
        if (TokenService.ORIGEN_INTERNO.equals(datos.origen())) {
            return cuentas.buscarPorUsuario(datos.usuario())
                    .map(c -> c.isActivo() && c.getVersionCredenciales() == datos.version())
                    .orElse(false);
        }
        if (TokenService.ORIGEN_GUBUY.equals(datos.origen())) {
            // Cada rol de dominio incluido en el token debe seguir vigente. Si tiene ambos roles,
            // no alcanza con conservar uno: el token aun habilitaria el otro.
            if (datos.roles().contains(Roles.FUNCIONARIO)
                    && !funcionarios.buscarPorCedula(datos.usuario()).map(Funcionario::isActivo).orElse(false)) {
                return false;
            }
            if (datos.roles().contains(Roles.CHOFER)
                    && !choferes.buscarPorCedula(datos.usuario()).map(Chofer::isActivo).orElse(false)) {
                return false;
            }
            return true; // un ciudadano no tiene nada que revocar: su identidad la garantiza gub.uy
        }
        return false; // token sin origen: se pide ingresar de nuevo
    }
}