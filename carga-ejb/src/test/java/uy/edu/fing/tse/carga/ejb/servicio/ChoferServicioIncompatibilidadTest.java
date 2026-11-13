package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJBAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.guia.ChoferDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Gestionar choferes como responsable es distinto de actuar con el rol CHOFER. */
class ChoferServicioIncompatibilidadTest {
    @Test void funcionarioConResponsabilidadHeredadaNoGestionaChoferes() throws Exception {
        Escenario e = new Escenario("CIUDADANO", "FUNCIONARIO");
        e.compatibilidad.funcionariosActivos.add("persona-prueba");
        e.compatibilidad.responsablesActivos.add("persona-prueba");
        assertThrows(EJBAccessException.class, () -> e.bean.listar(1L));
        assertThrows(EJBAccessException.class, () -> e.bean.agregar(datos()));
        assertThrows(EJBAccessException.class, () -> e.bean.modificar(2L, datos()));
        assertThrows(EJBAccessException.class, () -> e.bean.darDeBaja(2L));
        assertThrows(EJBAccessException.class, () -> e.bean.darDeAlta(2L));
        assertEquals("Original", e.chofer.getNombre());
        assertTrue(e.chofer.isActivo());
        assertEquals(0, e.persistidos);
        assertEquals(0, e.consultas);
    }

    @Test void choferConResponsabilidadHeredadaNoGestionaChoferes() throws Exception {
        Escenario e = new Escenario("CHOFER");
        e.compatibilidad.responsablesActivos.add("persona-prueba");
        e.compatibilidad.choferesActivos.add("persona-prueba");
        assertThrows(EJBAccessException.class, () -> e.bean.listar(1L));
        assertThrows(EJBAccessException.class, () -> e.bean.agregar(datos()));
        assertThrows(EJBAccessException.class, () -> e.bean.modificar(2L, datos()));
        assertThrows(EJBAccessException.class, () -> e.bean.darDeBaja(2L));
        assertThrows(EJBAccessException.class, () -> e.bean.darDeAlta(2L));
        assertEquals("Original", e.chofer.getNombre());
        assertTrue(e.chofer.isActivo());
        assertEquals(0, e.persistidos);
        assertEquals(0, e.consultas);
    }

    @Test void responsableCompatibleMantieneGestionDeOtrosChoferes() throws Exception {
        Escenario e = new Escenario("CIUDADANO");
        e.compatibilidad.responsablesActivos.add("persona-prueba");
        assertEquals(1, e.bean.listar(1L).size());
        assertEquals("Nuevo", e.bean.modificar(2L, datos()).getNombre());
    }

    @Test void responsableNoPuedeRegistrarseComoChoferActivoNiPorAdministrador() throws Exception {
        for (String rol : List.of("CIUDADANO", "ADMINISTRADOR")) {
            Escenario e = new Escenario(rol);
            e.compatibilidad.responsablesActivos.add("cedula-prueba");
            assertThrows(NegocioException.class, () -> e.bean.agregar(datos()));
            assertEquals(0, e.persistidos);
        }
    }

    @Test void edicionActivaValidaLaNuevaIdentidadAntesDeMutar() throws Exception {
        Escenario e = new Escenario("ADMINISTRADOR");
        e.compatibilidad.responsablesActivos.add("cedula-prueba");
        assertThrows(NegocioException.class, () -> e.bean.modificar(2L, datos()));
        assertEquals("Original", e.chofer.getNombre());
        assertEquals("cedula-original", e.chofer.getCedula());
        assertTrue(e.chofer.isActivo());
    }

    @Test void responsableNoPuedeReactivarUnRegistroDeChofer() throws Exception {
        Escenario e = new Escenario("ADMINISTRADOR"); e.chofer.setActivo(false);
        e.compatibilidad.responsablesActivos.add("cedula-original");
        assertThrows(NegocioException.class, () -> e.bean.darDeAlta(2L));
        assertFalse(e.chofer.isActivo());
    }

    @Test void administradorPuedeResolverCombinacionHeredadaDandoDeBajaAlChofer() throws Exception {
        Escenario e = new Escenario("ADMINISTRADOR");
        e.compatibilidad.responsablesActivos.add("cedula-original");
        e.bean.darDeBaja(2L);
        assertFalse(e.chofer.isActivo());
    }

    @Test void registrosInactivosNoImpidenResponsabilidadNiSeActivanAlEditar() throws Exception {
        Escenario e = new Escenario("ADMINISTRADOR");
        e.compatibilidad.responsablesActivos.add("cedula-prueba");
        ChoferDTO dto = datos(); dto.setActivo(false);
        assertFalse(e.bean.agregar(dto).isActivo());
        assertFalse(e.bean.modificar(2L, dto).isActivo());
        assertTrue(e.compatibilidad.identidadesChoferBloqueadas.isEmpty());
    }

    @Test void choferCompatiblePuedeCrearseYReactivarseConIdentidadNormalizada() throws Exception {
        Escenario e = new Escenario("ADMINISTRADOR");
        ChoferDTO dto = datos(); dto.setCedula("  cedula-prueba  ");
        assertTrue(e.bean.agregar(dto).isActivo());
        assertEquals("cedula-prueba", e.compatibilidad.identidadesChoferBloqueadas.getFirst());
        e.chofer.setActivo(false);
        e.bean.darDeAlta(2L);
        assertTrue(e.chofer.isActivo());
        assertEquals(List.of("cedula-prueba", "cedula-original"), e.compatibilidad.identidadesChoferBloqueadas);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, e.bloqueoChofer);
    }

    @Test void administradorMantieneLaGestionOficialSinDependerDeVinculos() throws Exception {
        Escenario e = new Escenario("ADMINISTRADOR");
        e.compatibilidad.funcionariosActivos.add("persona-prueba");
        e.compatibilidad.responsablesActivos.add("persona-prueba");
        assertEquals(1, e.bean.listar(null).size());
        assertEquals("Nuevo", e.bean.modificar(2L, datos()).getNombre());
    }

    private static ChoferDTO datos() {
        ChoferDTO dto = new ChoferDTO(); dto.setEmpresaId(1L); dto.setCedula("cedula-prueba");
        dto.setNombre("Nuevo"); dto.setActivo(true); return dto;
    }

    private static void campo(Object objeto, String nombre, Object valor) throws Exception {
        var campo = objeto.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(objeto, valor);
    }

    private static final class Escenario {
        final ChoferServicioBean bean = new ChoferServicioBean();
        final Falsos.CompatibilidadFalsa compatibilidad = new Falsos.CompatibilidadFalsa();
        final Empresa empresa = Falsos.conId(new Empresa(), 1L);
        final Chofer chofer = Falsos.conId(new Chofer(), 2L);
        int persistidos, consultas;
        LockModeType bloqueoChofer;

        Escenario(String... roles) throws Exception {
            chofer.setNombre("Original"); chofer.setCedula("cedula-original"); chofer.setEmpresa(empresa); chofer.setActivo(true);
            campo(bean, "contexto", Falsos.sesion("persona-prueba", roles));
            campo(bean, "compatibilidad", compatibilidad);
            bean.vinculos = new VinculoEmpresaUsuarioDAO() {
                @Override public boolean esResponsableActivo(String cedula, Long id) { return id.equals(1L); }
            };
            campo(bean, "em", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "find" -> {
                            if (a[0] == Empresa.class) yield empresa;
                            if (a.length == 3) bloqueoChofer = (LockModeType) a[2];
                            yield chofer;
                        }
                        case "getReference" -> empresa;
                        case "persist" -> { persistidos++; yield null; }
                        case "flush" -> null;
                        case "createQuery" -> {
                            consultas++;
                            yield Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class},
                                    (q, metodo, args) -> switch (metodo.getName()) {
                                        case "setParameter" -> q;
                                        case "getSingleResult" -> 0L;
                                        case "getResultList" -> List.of(chofer);
                                        default -> throw new UnsupportedOperationException(metodo.getName());
                                    });
                        }
                        default -> throw new UnsupportedOperationException(m.getName());
                    }));
        }
    }
}
