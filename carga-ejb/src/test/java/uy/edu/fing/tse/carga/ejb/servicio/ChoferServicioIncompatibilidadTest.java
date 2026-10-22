package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJBAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.guia.ChoferDTO;
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

    @Test void choferResponsableCompatibleMantieneElPermisoExistente() throws Exception {
        Escenario e = new Escenario("CHOFER");
        e.compatibilidad.responsablesActivos.add("persona-prueba");
        assertEquals(1, e.bean.listar(1L).size());
        assertEquals("Nuevo", e.bean.modificar(2L, datos()).getNombre());
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

        Escenario(String... roles) throws Exception {
            chofer.setNombre("Original"); chofer.setEmpresa(empresa); chofer.setActivo(true);
            campo(bean, "contexto", Falsos.sesion("persona-prueba", roles));
            campo(bean, "compatibilidad", compatibilidad);
            bean.vinculos = new VinculoEmpresaUsuarioDAO() {
                @Override public boolean esResponsableActivo(String cedula, Long id) { return id.equals(1L); }
            };
            campo(bean, "em", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "find" -> a[0] == Empresa.class ? empresa : chofer;
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
