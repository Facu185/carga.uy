package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.TipoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.VinculoEmpresaUsuario;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Regresión del servicio existente antes del consumidor F12; no prueba contenedor, JPQL ni JTA. */
class EmpresaServicioBeanAdministracionTest {
    @Test void editaCamposAdministrativosSinReactivarEmpresa() throws Exception {
        Escenario e = new Escenario(); e.empresa.setActiva(false);
        EmpresaDTO resultado = e.bean.modificar(1L, datos());
        assertEquals("NUEVA", resultado.getNombre());
        assertEquals("RAZON", resultado.getRazonSocial());
        assertEquals("DIRECCION", resultado.getDireccion());
        assertEquals(200L, resultado.getNumeroEmpresa());
        assertEquals(LocalDate.of(2021, 2, 3), resultado.getFechaAlta());
        assertEquals(1L, resultado.getId());
        assertFalse(resultado.isActiva());
        assertTrue(e.auditorias.isEmpty());
    }

    @Test void fechaAusenteSeCompletaSegunContratoAdministrativoExistente() throws Exception {
        Escenario e = new Escenario(); EmpresaDTO dto = datos(); dto.setFechaAlta(null);
        LocalDate antes = LocalDate.now();
        LocalDate fecha = e.bean.modificar(1L, dto).getFechaAlta();
        assertFalse(fecha.isBefore(antes)); assertFalse(fecha.isAfter(LocalDate.now()));
    }

    @Test void invalidoODuplicadoNoModificaEntidad() throws Exception {
        List<EmpresaDTO> invalidos = new ArrayList<>(); invalidos.add(null);
        EmpresaDTO vacio = datos(); vacio.setNombre(" "); invalidos.add(vacio);
        EmpresaDTO numero = datos(); numero.setNumeroEmpresa(0); invalidos.add(numero);
        EmpresaDTO fecha = datos(); fecha.setFechaAlta(LocalDate.now().plusDays(2)); invalidos.add(fecha);
        for (EmpresaDTO dto : invalidos) {
            Escenario e = new Escenario();
            assertThrows(NegocioException.class, () -> e.bean.modificar(1L, dto));
            assertEquals("ORIGINAL", e.empresa.getNombre()); assertEquals(100L, e.empresa.getNumeroEmpresa());
        }
        Escenario duplicado = new Escenario(); duplicado.duplicados = 1L;
        assertThrows(NegocioException.class, () -> duplicado.bean.modificar(1L, datos()));
        assertEquals("ORIGINAL", duplicado.empresa.getNombre());
        assertEquals(200L, duplicado.parametros.get("num"));
    }

    @Test void empresaInexistenteNoSeEditaNiDaDeBaja() throws Exception {
        Escenario e = new Escenario(); e.existe = false;
        assertThrows(NegocioException.class, () -> e.bean.modificar(1L, datos()));
        assertThrows(NegocioException.class, () -> e.bean.darDeBaja(1L));
        assertTrue(e.auditorias.isEmpty());
    }

    @Test void bajaRevocaVinculosActivosYRegistraAuditoria() throws Exception {
        Escenario e = new Escenario(); e.bean.darDeBaja(1L);
        assertFalse(e.empresa.isActiva());
        assertEquals("ORIGINAL", e.empresa.getNombre()); // no borra datos históricos
        assertEquals(1L, e.parametros.get("empresa"));
        assertEquals(EstadoVinculoEmpresa.ACTIVO, e.parametros.get("estado"));
        for (VinculoEmpresaUsuario vinculo : e.vinculos) {
            assertEquals(EstadoVinculoEmpresa.REVOCADO, vinculo.getEstado());
            assertEquals("funcionario-sintetico", vinculo.getRevocadoPor());
            assertNotNull(vinculo.getRevocadoEn());
            assertEquals("Baja administrativa de empresa", vinculo.getMotivoRevocacion());
        }
        assertEquals(List.of(AccionAuditada.EMPRESA_DADA_DE_BAJA, "Empresa", 1L,
                "Baja administrativa de empresa", "vinculosRevocados=true"), e.auditorias.get(0));
    }

    @Test void bajaRepetidaEsNegocioYNoRepiteAuditoria() throws Exception {
        Escenario e = new Escenario(); e.bean.darDeBaja(1L);
        assertThrows(NegocioException.class, () -> e.bean.darDeBaja(1L));
        assertEquals(1, e.auditorias.size());
    }

    @Test void falloDeAuditoriaSePropagaSinSimularRollbackDeLosDobles() throws Exception {
        Escenario e = new Escenario(); e.falloAuditoria = new NegocioException("Fallo sintético");
        assertSame(e.falloAuditoria, assertThrows(NegocioException.class, () -> e.bean.darDeBaja(1L)));
        // Las entidades en memoria ya fueron mutadas: solo una prueba JTA acredita el rollback real.
        assertTrue(NegocioException.class.getAnnotation(jakarta.ejb.ApplicationException.class).rollback());
    }

    private static EmpresaDTO datos() {
        EmpresaDTO dto = new EmpresaDTO(); dto.setNombre(" NUEVA "); dto.setRazonSocial("RAZON");
        dto.setDireccion("DIRECCION"); dto.setNumeroEmpresa(200L);
        dto.setFechaAlta(LocalDate.of(2021, 2, 3)); return dto;
    }

    private static final class Escenario {
        final EmpresaServicioBean bean = new EmpresaServicioBean();
        final Empresa empresa = new Empresa();
        final List<VinculoEmpresaUsuario> vinculos = new ArrayList<>();
        final List<List<Object>> auditorias = new ArrayList<>();
        final Map<String, Object> parametros = new HashMap<>();
        boolean existe = true;
        long duplicados;
        NegocioException falloAuditoria;

        Escenario() throws Exception {
            campo(empresa, "id", 1L); empresa.setNombre("ORIGINAL"); empresa.setNumeroEmpresa(100L);
            empresa.setFechaAlta(LocalDate.of(2020, 1, 1)); empresa.setActiva(true);
            for (TipoVinculoEmpresa tipo : TipoVinculoEmpresa.values()) {
                VinculoEmpresaUsuario v = new VinculoEmpresaUsuario(null, empresa, tipo);
                v.aprobar("aprobador-sintetico"); vinculos.add(v);
            }
            campo(bean, "em", proxy(EntityManager.class, (objeto, metodo, args) -> switch (metodo.getName()) {
                case "find" -> existe ? empresa : null;
                case "createQuery" -> consulta();
                default -> throw new UnsupportedOperationException(metodo.getName());
            }));
            campo(bean, "contexto", proxy(SessionContext.class, (objeto, metodo, args) -> {
                if (metodo.getName().equals("getCallerPrincipal")) return (Principal) () -> "funcionario-sintetico";
                throw new UnsupportedOperationException(metodo.getName());
            }));
            campo(bean, "auditoria", proxy(AuditoriaLocal.class, (objeto, metodo, args) -> {
                if (!metodo.getName().equals("registrar")) throw new UnsupportedOperationException(metodo.getName());
                if (falloAuditoria != null) throw falloAuditoria;
                auditorias.add(List.of(args)); return null;
            }));
        }

        private TypedQuery<?> consulta() {
            return proxy(TypedQuery.class, (objeto, metodo, args) -> switch (metodo.getName()) {
                case "setParameter" -> { parametros.put((String) args[0], args[1]); yield objeto; }
                case "getSingleResult" -> duplicados;
                case "getResultList" -> vinculos;
                default -> throw new UnsupportedOperationException(metodo.getName());
            });
        }
    }

    private static <T> T proxy(Class<T> tipo, java.lang.reflect.InvocationHandler handler) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, handler));
    }

    private static void campo(Object objeto, String nombre, Object valor) throws Exception {
        var campo = objeto.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(objeto, valor);
    }
}
