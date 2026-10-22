package uy.edu.fing.tse.carga.web.jsf;

import jakarta.ejb.EJBAccessException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.security.enterprise.SecurityContext;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.SolicitudesEmpresaLocal;
import uy.edu.fing.tse.carga.api.contract.service.VehiculoServicioLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.VinculoEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Coordinacion JSF con dobles; no certifica AJAX, sesion ni autorizacion del contenedor. */
class VehiculoVistaTest {
    @Test void administradorListaGlobalSinConsultarVinculosPersonales() throws Exception {
        Escenario e = new Escenario(true);
        e.vista.iniciar();
        assertEquals(1, e.consultas.size());
        assertNull(e.consultas.getFirst());
        assertEquals(2, e.vista.getEmpresas().size());
    }

    @Test void responsableListaSoloEmpresaAutorizadaYNuncaGlobal() throws Exception {
        Escenario e = new Escenario(false);
        e.vinculos = List.of(vinculo(2L, "RESPONSABLE", "ACTIVO"), vinculo(1L, "CHOFER", "ACTIVO"),
                vinculo(1L, "RESPONSABLE", "PENDIENTE"), vinculo(1L, "RESPONSABLE", "REVOCADO"));
        e.vista.iniciar();
        assertEquals(List.of(2L), e.consultas);
        assertEquals(2L, e.vista.getEmpresaConsultaId());
        assertEquals(2L, e.vista.getNuevo().getEmpresaId());
        assertEquals(List.of(2L), e.vista.getEmpresas().stream().map(EmpresaDTO::getId).toList());
    }

    @Test void puedeSeleccionarOtraEmpresaEntreMultiplesVinculosPropios() throws Exception {
        Escenario e = new Escenario(false);
        e.vinculos = List.of(vinculo(1L, "RESPONSABLE", "ACTIVO"), vinculo(2L, "RESPONSABLE", "ACTIVO"));
        e.vista.iniciar();
        e.vista.setEmpresaConsultaId(2L);
        e.vista.refrescar();
        assertEquals(List.of(1L, 2L), e.consultas);
        assertEquals(2L, e.vista.getNuevo().getEmpresaId());
    }

    @Test void sinVinculoNoConsultaFlota() throws Exception {
        Escenario e = new Escenario(false);
        e.vista.iniciar();
        assertTrue(e.consultas.isEmpty());
        assertTrue(e.vista.getVehiculos().isEmpty());
        assertTrue(e.vista.getEmpresas().isEmpty());
    }

    @Test void filtroAjenoORevocadoLimpiaListadoAnterior() throws Exception {
        Escenario e = new Escenario(false);
        e.vinculos = List.of(vinculo(1L, "RESPONSABLE", "ACTIVO"));
        e.vista.iniciar();
        assertFalse(e.vista.getVehiculos().isEmpty());
        e.vista.setEmpresaConsultaId(2L);
        e.vista.refrescar();
        assertTrue(e.vista.getVehiculos().isEmpty());
        assertNull(e.vista.getNuevo().getEmpresaId());
        e.vista.setEmpresaConsultaId(1L);
        e.vinculos = List.of();
        e.vista.refrescar();
        assertEquals(List.of(1L), e.consultas);
        assertTrue(e.vista.getVehiculos().isEmpty());
        assertNull(e.vista.getNuevo().getEmpresaId());
    }

    @Test void denegacionEnDependenciasDeRefrescoLimpiaDatosSinPropagar500() throws Exception {
        for (String etapa : List.of("empresas", "vinculos", "listar")) {
            Escenario e = new Escenario(false);
            e.vinculos = List.of(vinculo(1L, "RESPONSABLE", "ACTIVO"));
            e.vista.iniciar();
            e.denegado = etapa;
            try (Mensajes mensajes = new Mensajes()) {
                assertDoesNotThrow(e.vista::refrescar);
                assertEquals(1, mensajes.mensajes.size());
                assertEquals(FacesMessage.SEVERITY_ERROR, mensajes.mensajes.getFirst().getSeverity());
            }
            assertTrue(e.vista.getVehiculos().isEmpty());
            assertTrue(e.vista.getEmpresas().isEmpty());
            assertNull(e.vista.getNuevo().getEmpresaId());
        }
    }

    @Test void revocacionDuranteMutacionesLimpiaDatosYPresentaErrorSeguro() throws Exception {
        for (String operacion : List.of("agregar", "modificar", "darDeBaja")) {
            Escenario e = new Escenario(false);
            e.vinculos = List.of(vinculo(1L, "RESPONSABLE", "ACTIVO"));
            e.vista.iniciar();
            e.denegado = operacion;
            try (Mensajes mensajes = new Mensajes()) {
                assertDoesNotThrow(() -> {
                    switch (operacion) {
                        case "agregar" -> e.vista.agregar();
                        case "modificar" -> e.vista.modificar(e.vehiculo);
                        case "darDeBaja" -> e.vista.darDeBaja(e.vehiculo);
                        default -> fail(operacion);
                    }
                });
                assertEquals(1, mensajes.mensajes.size());
                assertFalse(mensajes.mensajes.getFirst().getSummary().contains("detalle interno"));
            }
            assertTrue(e.vista.getVehiculos().isEmpty());
            assertTrue(e.vista.getEmpresas().isEmpty());
            assertNull(e.vista.getNuevo().getEmpresaId());
        }
    }

    @Test void fallaTecnicaNoSeDisfrazaDeDenegacion() throws Exception {
        Escenario e = new Escenario(false);
        e.vinculos = List.of(vinculo(1L, "RESPONSABLE", "ACTIVO"));
        e.vista.iniciar();
        e.fallaTecnica = true;
        assertThrows(IllegalStateException.class, e.vista::refrescar);
        assertTrue(e.vista.getVehiculos().isEmpty());
    }

    private static class Escenario {
        final VehiculoVista vista = new VehiculoVista();
        final VehiculoDTO vehiculo = new VehiculoDTO();
        final List<Long> consultas = new ArrayList<>();
        List<VinculoEmpresaDTO> vinculos = List.of();
        String denegado;
        boolean fallaTecnica;

        Escenario(boolean admin) throws Exception {
            vehiculo.setId(10L); vehiculo.setEmpresaId(1L);
            campo(vista, "seguridad", doble(SecurityContext.class, (nombre, args) -> admin));
            campo(vista, "empresaServicio", doble(EmpresaServicioLocal.class, (nombre, args) -> {
                denegar("empresas"); return List.of(empresa(1L), empresa(2L));
            }));
            campo(vista, "vinculosServicio", doble(SolicitudesEmpresaLocal.class, (nombre, args) -> {
                assertFalse(admin, "Un administrador no consulta vinculos personales");
                assertEquals("misVinculos", nombre); denegar("vinculos"); return vinculos;
            }));
            campo(vista, "servicio", doble(VehiculoServicioLocal.class, (nombre, args) -> {
                denegar(nombre);
                if (fallaTecnica) throw new IllegalStateException("fallo tecnico");
                if (nombre.equals("listar")) { consultas.add((Long) args[0]); return List.of(vehiculo); }
                return nombre.equals("darDeBaja") ? null : vehiculo;
            }));
        }

        private void denegar(String etapa) {
            if (etapa.equals(denegado)) throw new EJBAccessException("detalle interno");
        }
    }

    private static class Mensajes extends FacesContextWrapper implements AutoCloseable {
        final List<FacesMessage> mensajes = new ArrayList<>();
        Mensajes() { setCurrentInstance(this); }
        @Override public FacesContext getWrapped() { return null; }
        @Override public void addMessage(String id, FacesMessage mensaje) { mensajes.add(mensaje); }
        @Override public void close() { setCurrentInstance(null); }
    }

    private static EmpresaDTO empresa(Long id) {
        EmpresaDTO dto = new EmpresaDTO(); dto.setId(id); dto.setNombre("Empresa de prueba"); dto.setActiva(true); return dto;
    }

    private static VinculoEmpresaDTO vinculo(Long empresa, String tipo, String estado) {
        VinculoEmpresaDTO dto = new VinculoEmpresaDTO(); dto.setEmpresaId(empresa); dto.setTipo(tipo); dto.setEstado(estado); return dto;
    }

    private static void campo(Object objeto, String nombre, Object valor) throws Exception {
        var campo = objeto.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(objeto, valor);
    }

    private interface Respuesta { Object obtener(String nombre, Object[] args); }
    private static <T> T doble(Class<T> tipo, Respuesta respuesta) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo},
                (proxy, metodo, args) -> respuesta.obtener(metodo.getName(), args)));
    }
}
