package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.empresa.NuevaSolicitudEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.funcionario.RechazoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.*;
import uy.edu.fing.tse.carga.ejb.persistencia.SolicitudAltaEmpresaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Regresiones previas a implementar ADR-0015; los dobles no acreditan JTA. */
class SolicitudesEmpresaIncompatibilidadTest {
    @Test void funcionarioConTokenCiudadanoAnteriorTampocoPuedeSolicitarResponsabilidad() {
        Escenario e = new Escenario();
        e.compatibilidad.funcionariosActivos.add(e.persona.getCedula());
        // No figura FUNCIONARIO en la sesion, pero si en el padron persistido simulado.
        assertThrows(NegocioException.class, () -> e.bean.solicitarVinculo(1L));
        assertTrue(e.guardados.isEmpty());
        assertTrue(e.auditoria.registros.isEmpty());
    }

    @Test void funcionarioNoSolicitaVinculoNiAltaEmpresarialAunqueTengaCiudadano() {
        Escenario e = new Escenario(); e.comoFuncionario();
        assertThrows(NegocioException.class, () -> e.bean.solicitarVinculo(1L));
        assertThrows(NegocioException.class, () -> e.bean.solicitarAlta(datos()));
        assertTrue(e.guardados.isEmpty());
        assertTrue(e.altasGuardadas.isEmpty());
        assertTrue(e.auditoria.registros.isEmpty());
    }

    @Test void aprobacionDeAltaNoCreaEmpresaNiResuelveSolicitudDeFuncionarioActivo() {
        Escenario e = new Escenario(); e.comoFuncionario(); e.comoAprobador();
        assertThrows(NegocioException.class, () -> e.bean.aprobarAlta(1L));
        assertEquals(0, e.persistidos);
        assertEquals(0, e.flushes);
        assertTrue(e.alta.estaPendiente());
        assertNull(e.alta.getEmpresa());
        assertTrue(e.guardados.isEmpty());
        assertTrue(e.auditoria.registros.isEmpty());
    }

    @Test void aprobacionDeVinculoValidaAlDestinatarioNoAlFuncionarioAprobador() {
        Escenario e = new Escenario(); e.comoFuncionario(); e.comoAprobador();
        assertThrows(NegocioException.class, () -> e.bean.aprobarVinculo(2L));
        assertTrue(e.nuevo.estaPendiente());
        assertNull(e.nuevo.getAprobadoEn());
        assertTrue(e.auditoria.registros.isEmpty());
        assertEquals(List.of(e.persona.getCedula()), e.compatibilidad.bloqueadas);
    }

    @Test void transferenciaIncompatibleNoRevocaAlResponsableAnterior() {
        Escenario e = new Escenario(); e.comoFuncionario(); e.comoAprobador();
        assertThrows(NegocioException.class, () -> e.bean.transferirResponsabilidad(2L, new RechazoDTO("Prueba")));
        assertTrue(e.nuevo.estaPendiente());
        assertTrue(e.anterior.estaActivo());
        assertNull(e.anterior.getRevocadoEn());
        assertTrue(e.auditoria.registros.isEmpty());
    }

    @Test void funcionarioPuedeAprobarResponsabilidadDeOtraPersonaCompatible() throws Exception {
        Escenario e = new Escenario(); e.comoAprobador();
        e.compatibilidad.funcionariosActivos.add("aprobador");
        assertEquals("ACTIVO", e.bean.aprobarVinculo(2L).getEstado());
        assertTrue(e.nuevo.estaActivo());
        assertEquals(1, e.auditoria.registros.size());
    }

    @Test void ciudadanoYChoferCompatiblesSiguenPudiendoSolicitar() throws Exception {
        for (String rol : List.of("CIUDADANO", "CHOFER")) {
            Escenario e = new Escenario();
            e.bean.contexto = Falsos.sesion(e.persona.getCedula(), rol);
            assertEquals("PENDIENTE", e.bean.solicitarVinculo(1L).getEstado());
            assertEquals("PENDIENTE", e.bean.solicitarAlta(datos()).getEstado());
            assertEquals(1, e.guardados.size());
            assertEquals(1, e.altasGuardadas.size());
        }
    }

    @Test void trasBajaDelFuncionarioSePuedeAprobarSinRevocarHistorialImplicitamente() throws Exception {
        Escenario e = new Escenario(); e.comoFuncionario(); e.comoAprobador();
        assertThrows(NegocioException.class, () -> e.bean.aprobarVinculo(2L));
        e.compatibilidad.funcionariosActivos.clear();
        assertEquals("ACTIVO", e.bean.aprobarVinculo(2L).getEstado());
    }

    @Test void transferenciaCompatibleConservaElCircuitoAdministrativoExplicito() throws Exception {
        Escenario e = new Escenario(); e.comoAprobador();
        assertEquals("ACTIVO", e.bean.transferirResponsabilidad(2L, new RechazoDTO("Transferencia solicitada")).getEstado());
        assertEquals(EstadoVinculoEmpresa.REVOCADO, e.anterior.getEstado());
        assertTrue(e.nuevo.estaActivo());
    }

    @Test void altaEmpresarialCompatibleConservaCreacionDeEmpresaYResponsable() throws Exception {
        Escenario e = new Escenario(); e.comoAprobador();
        assertEquals("APROBADA", e.bean.aprobarAlta(1L).getEstado());
        assertEquals(1, e.persistidos);
        assertEquals(1, e.flushes);
        assertTrue(e.guardados.getFirst().estaActivo());
        assertEquals(2, e.auditoria.registros.size());
    }

    private static NuevaSolicitudEmpresaDTO datos() {
        return new NuevaSolicitudEmpresaDTO("Empresa prueba", null, "Direccion prueba", 123L);
    }

    private static final class Escenario {
        final SolicitudesEmpresaBean bean = new SolicitudesEmpresaBean();
        final Falsos.CompatibilidadFalsa compatibilidad = new Falsos.CompatibilidadFalsa();
        final Falsos.AuditoriaFalsa auditoria = new Falsos.AuditoriaFalsa();
        final Falsos.UsuariosEnMemoria usuarios = new Falsos.UsuariosEnMemoria();
        final Usuario persona = usuarios.guardar(new Usuario("12345672", "Persona prueba", null));
        final Empresa empresa = Falsos.conId(new Empresa(), 1L);
        final SolicitudAltaEmpresa alta = Falsos.conId(new SolicitudAltaEmpresa(persona, "Empresa prueba", null, "Direccion", 123L), 1L);
        final VinculoEmpresaUsuario nuevo = Falsos.conId(new VinculoEmpresaUsuario(persona, empresa, TipoVinculoEmpresa.RESPONSABLE), 2L);
        final VinculoEmpresaUsuario anterior = Falsos.conId(new VinculoEmpresaUsuario(new Usuario("anterior", "Anterior", null), empresa, TipoVinculoEmpresa.RESPONSABLE), 3L);
        final List<VinculoEmpresaUsuario> guardados = new ArrayList<>();
        final List<SolicitudAltaEmpresa> altasGuardadas = new ArrayList<>();
        int persistidos, flushes;

        Escenario() {
            empresa.setNombre("Empresa prueba"); anterior.aprobar("admin");
            bean.usuarios = usuarios; bean.auditoria = auditoria; bean.compatibilidad = compatibilidad;
            bean.contexto = Falsos.sesion(persona.getCedula(), "CIUDADANO");
            bean.vinculos = new VinculoEmpresaUsuarioDAO() {
                @Override public Optional<VinculoEmpresaUsuario> pendiente(Long usuarioId, Long empresaId) { return Optional.empty(); }
                @Override public VinculoEmpresaUsuario guardar(VinculoEmpresaUsuario v) { guardados.add(Falsos.conId(v, 9L)); return v; }
                @Override public Optional<VinculoEmpresaUsuario> buscarPorId(Long id) { return Optional.of(nuevo); }
                @Override public List<VinculoEmpresaUsuario> activosDeEmpresa(Long id) { return List.of(anterior); }
            };
            bean.solicitudes = new SolicitudAltaEmpresaDAO() {
                @Override public Optional<SolicitudAltaEmpresa> buscarPorId(Long id) { return Optional.of(alta); }
                @Override public Optional<SolicitudAltaEmpresa> pendientePorUsuario(String cedula) { return Optional.empty(); }
                @Override public SolicitudAltaEmpresa guardar(SolicitudAltaEmpresa s) { altasGuardadas.add(Falsos.conId(s, 9L)); return s; }
            };
            bean.em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "find" -> empresa;
                        case "persist" -> { persistidos++; Falsos.conId(a[0], 10L); yield null; }
                        case "flush" -> { flushes++; yield null; }
                        case "createQuery" -> Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class},
                                (q, metodo, args) -> switch (metodo.getName()) {
                                    case "setParameter" -> q;
                                    case "getSingleResult" -> 0L;
                                    default -> throw new UnsupportedOperationException(metodo.getName());
                                });
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
        }

        void comoFuncionario() { compatibilidad.funcionariosActivos.add(persona.getCedula()); bean.contexto = Falsos.sesion(persona.getCedula(), "CIUDADANO", "FUNCIONARIO"); }
        void comoAprobador() { bean.contexto = Falsos.sesion("aprobador", "FUNCIONARIO"); }
    }
}
