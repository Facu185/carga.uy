package uy.edu.fing.tse.carga.web.jsf;

import jakarta.security.enterprise.SecurityContext;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.ChoferServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.SolicitudesEmpresaLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.VinculoEmpresaDTO;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba coordinacion de la vista, NO pertenencia ni @RolesAllowed reales del EJB. */
class ChoferVistaTest {
    private final List<Long> consultas = new ArrayList<>();

    @Test
    void administradorPuedeListarTodosSinConsultarVinculosPersonales() throws Exception {
        ChoferVista vista = preparar(true, List.of());
        vista.iniciar();
        assertEquals(1, consultas.size());
        assertNull(consultas.getFirst());
        assertEquals(2, vista.getEmpresas().size());
    }

    @Test
    void responsableListaSoloSuEmpresaActivaYNuncaListarNull() throws Exception {
        ChoferVista vista = preparar(false, List.of(vinculo(2L, "RESPONSABLE", "ACTIVO"),
                vinculo(1L, "CHOFER", "ACTIVO"), vinculo(1L, "RESPONSABLE", "REVOCADO")));
        vista.iniciar();
        assertEquals(List.of(2L), consultas);
        assertEquals(2L, vista.getEmpresaConsultaId());
        assertEquals(2L, vista.getNuevo().getEmpresaId());
        assertEquals(List.of(2L), vista.getEmpresas().stream().map(EmpresaDTO::getId).toList());
    }

    @Test
    void sinVinculoResponsableNoConsultaChoferes() throws Exception {
        ChoferVista vista = preparar(false, List.of());
        vista.iniciar();
        assertTrue(consultas.isEmpty());
        assertTrue(vista.getChoferes().isEmpty());
        assertTrue(vista.getEmpresas().isEmpty());
    }

    @Test
    void filtroAjenoNoReutilizaElListadoAnterior() throws Exception {
        ChoferVista vista = preparar(false, List.of(vinculo(2L, "RESPONSABLE", "ACTIVO")));
        vista.iniciar();
        vista.setEmpresaConsultaId(1L);
        vista.refrescar();
        assertEquals(List.of(2L), consultas);
        assertTrue(vista.getChoferes().isEmpty());
    }

    private ChoferVista preparar(boolean admin, List<VinculoEmpresaDTO> vinculos) throws Exception {
        ChoferVista vista = new ChoferVista();
        inyectar(vista, "seguridad", doble(SecurityContext.class, (nombre, args) -> {
            assertEquals("isCallerInRole", nombre);
            return admin;
        }));
        inyectar(vista, "empresaServicio", doble(EmpresaServicioLocal.class, (nombre, args) -> {
            assertEquals("listar", nombre);
            return List.of(empresa(1L), empresa(2L));
        }));
        inyectar(vista, "vinculosServicio", doble(SolicitudesEmpresaLocal.class, (nombre, args) -> {
            assertFalse(admin, "Un administrador no usa el endpoint personal de ciudadanos");
            assertEquals("misVinculos", nombre);
            return vinculos;
        }));
        inyectar(vista, "servicio", doble(ChoferServicioLocal.class, (nombre, args) -> {
            assertEquals("listar", nombre);
            consultas.add((Long) args[0]);
            return List.of();
        }));
        return vista;
    }

    private static EmpresaDTO empresa(Long id) {
        EmpresaDTO dto = new EmpresaDTO(); dto.setId(id); dto.setNombre("Empresa de prueba"); dto.setActiva(true);
        return dto;
    }

    private static VinculoEmpresaDTO vinculo(Long id, String tipo, String estado) {
        VinculoEmpresaDTO dto = new VinculoEmpresaDTO(); dto.setEmpresaId(id); dto.setTipo(tipo); dto.setEstado(estado);
        return dto;
    }

    private static void inyectar(Object destino, String nombre, Object valor) throws Exception {
        var campo = destino.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(destino, valor);
    }

    private interface Respuesta { Object obtener(String nombre, Object[] args); }

    private static <T> T doble(Class<T> tipo, Respuesta respuestas) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo},
                (proxy, metodo, args) -> respuestas.obtener(metodo.getName(), args)));
    }
}
