package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.comun.PaginaAuditoriaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.RegistroAuditoria;
import uy.edu.fing.tse.carga.ejb.persistencia.RegistroAuditoriaDAO;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaBeanTest {

    private AuditoriaBean auditoria;
    private final List<RegistroAuditoria> guardados = new ArrayList<>();
    private int ultimoDesde;
    private int ultimaCantidad;

    @BeforeEach
    void preparar() {
        auditoria = new AuditoriaBean();
        auditoria.dao = new RegistroAuditoriaDAO() {
            @Override
            public void agregar(RegistroAuditoria registro) {
                guardados.add(registro);
            }

            @Override
            public List<RegistroAuditoria> buscar(String u, String e, String id, int desde, int cantidad) {
                ultimoDesde = desde;
                ultimaCantidad = cantidad;
                return List.copyOf(guardados);
            }

            @Override
            public long contar(String u, String e, String id) {
                return guardados.size();
            }
        };
        auditoria.contexto = Falsos.sesion("12345672", "FUNCIONARIO");
    }

    private RegistroAuditoria ultimo() {
        return guardados.get(guardados.size() - 1);
    }

    @Test
    void registraElUsuarioYElRolDeLaSesionNoDeParametros() throws NegocioException {
        auditoria.registrar(AccionAuditada.CASO_CREADO, "CasoFiscalizacion", 42L, "Exceso de peso", null);

        assertEquals("12345672", ultimo().getUsuario());
        assertEquals("FUNCIONARIO", ultimo().getRol());
        assertEquals("42", ultimo().getEntidadId());
        assertEquals("CASO_CREADO", ultimo().getAccion());
    }

    @Test
    void lasAccionesDeFiscalizacionExigenFundamentoYLasDeAdministracionNo() {
        assertThrows(NegocioException.class,
                () -> auditoria.registrar(AccionAuditada.CASO_RESUELTO, "CasoFiscalizacion", 1L, " ", null));
        assertTrue(guardados.isEmpty());
        assertDoesNotThrow(
                () -> auditoria.registrar(AccionAuditada.FUNCIONARIO_HABILITADO, "Funcionario", "1", null, null));
    }

    @Test
    void sinSesionQuedaComoSistema() throws NegocioException {
        auditoria.contexto = Falsos.sesion(null);
        auditoria.registrar(AccionAuditada.CASO_CREADO, "CasoFiscalizacion", 1L, "Deteccion automatica", null);

        assertEquals(AuditoriaBean.USUARIO_SISTEMA, ultimo().getUsuario());
        assertNull(ultimo().getRol());
    }

    @Test
    void registrarComoUsaElUsuarioIndicadoYSinUsuarioQuedaDesconocido() {
        auditoria.registrarComo("admin", null, AccionAuditada.INGRESO_FALLIDO, "CuentaInterna", "admin", null, null);
        assertEquals("admin", ultimo().getUsuario());

        auditoria.registrarComo(" ", null, AccionAuditada.INGRESO_FALLIDO, "CuentaInterna", "x", null, null);
        assertEquals("desconocido", ultimo().getUsuario());
    }

    @Test
    void exigeAccionEntidadEId() {
        assertThrows(IllegalArgumentException.class,
                () -> auditoria.registrar(null, "Entidad", 1L, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> auditoria.registrar(AccionAuditada.CASO_CREADO, " ", 1L, "f", null));
        assertThrows(IllegalArgumentException.class,
                () -> auditoria.registrar(AccionAuditada.CASO_CREADO, "Entidad", null, "f", null));
    }

    @Test
    void laConsultaPideLaPaginaCorrectaYRespetaElTamanoMaximo() {
        PaginaAuditoriaDTO pagina = auditoria.buscar(null, null, null, 2, 500);
        assertEquals(AuditoriaBean.TAMANO_MAXIMO, ultimaCantidad);
        assertEquals(2 * AuditoriaBean.TAMANO_MAXIMO, ultimoDesde);
        assertEquals(2, pagina.getPagina());

        auditoria.buscar(null, null, null, null, null);
        assertEquals(AuditoriaBean.TAMANO_POR_DEFECTO, ultimaCantidad);
        assertEquals(0, ultimoDesde);
    }
}
