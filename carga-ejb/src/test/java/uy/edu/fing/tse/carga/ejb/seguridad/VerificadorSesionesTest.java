package uy.edu.fing.tse.carga.ejb.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;
import uy.edu.fing.tse.carga.ejb.persistencia.CuentaInternaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.FuncionarioDAO;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificadorSesionesTest {

    private VerificadorSesiones verificador;
    private CuentaInterna admin;
    private Funcionario funcionario;

    @BeforeEach
    void preparar() {
        admin = new CuentaInterna("admin", "Admin", "hash", Rol.ADMINISTRADOR);
        funcionario = new Funcionario(new Usuario("12345672", "Ana", null), "DNT");
        verificador = new VerificadorSesiones();
        verificador.cuentas = new CuentaInternaDAO() {
            @Override
            public Optional<CuentaInterna> buscarPorUsuario(String u) {
                return "admin".equals(u) ? Optional.of(admin) : Optional.empty();
            }
        };
        verificador.funcionarios = new FuncionarioDAO() {
            @Override
            public Optional<Funcionario> buscarPorCedula(String c) {
                return "12345672".equals(c) ? Optional.of(funcionario) : Optional.empty();
            }
        };
    }

    private static TokenService.DatosToken token(String usuario, String rol, String origen, int version) {
        return new TokenService.DatosToken(usuario, Set.of(rol), origen, version, false);
    }

    @Test
    void unaCuentaActivaConLaMismaVersionSigue() {
        assertTrue(verificador.sigueVigente(token("admin", "ADMINISTRADOR", TokenService.ORIGEN_INTERNO, 0)));
    }

    @Test
    void siCambioLaVersionOLaCuentaNoExisteOEstaDeBajaSeCorta() {
        admin.cambiarPassword("otro", false);
        assertFalse(verificador.sigueVigente(token("admin", "ADMINISTRADOR", TokenService.ORIGEN_INTERNO, 0)));
        assertFalse(verificador.sigueVigente(token("nadie", "ADMINISTRADOR", TokenService.ORIGEN_INTERNO, 0)));
        admin.setActivo(false);
        assertFalse(verificador.sigueVigente(token("admin", "ADMINISTRADOR", TokenService.ORIGEN_INTERNO, 2)));
    }

    @Test
    void unFuncionarioDadoDeBajaPierdeElAccesoEnElActo() {
        assertTrue(verificador.sigueVigente(token("12345672", "FUNCIONARIO", TokenService.ORIGEN_GUBUY, 0)));
        funcionario.setActivo(false);
        assertFalse(verificador.sigueVigente(token("12345672", "FUNCIONARIO", TokenService.ORIGEN_GUBUY, 0)));
    }

    @Test
    void unCiudadanoSigueYUnTokenSinOrigenNo() {
        assertTrue(verificador.sigueVigente(token("44444444", "CIUDADANO", TokenService.ORIGEN_GUBUY, 0)));
        assertFalse(verificador.sigueVigente(token("admin", "ADMINISTRADOR", null, 0)));
    }
}
