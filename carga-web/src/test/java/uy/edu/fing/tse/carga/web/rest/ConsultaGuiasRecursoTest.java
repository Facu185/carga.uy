package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.ConsultaGuiasLocal;
import uy.edu.fing.tse.carga.api.dto.guia.ConsultaGuiaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.PaginaConsultaGuiasDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/** Escritas antes del recurso: delegacion, no transporte HTTP/JSON-B ni seguridad real. */
class ConsultaGuiasRecursoTest {
    @Test void rutasSoloGetSeparadasDelRecursoEmpresarial() throws Exception {
        assertEquals("/guias/consulta", ConsultaGuiasRecurso.class.getAnnotation(Path.class).value());
        assertNotNull(ConsultaGuiasRecurso.class.getMethod("consultar", String.class, String.class, String.class,
                String.class, String.class, String.class).getAnnotation(GET.class));
        var detalle = ConsultaGuiasRecurso.class.getMethod("detalle", String.class, String.class);
        assertNotNull(detalle.getAnnotation(GET.class)); assertEquals("/{id}", detalle.getAnnotation(Path.class).value());
        assertEquals(2, ConsultaGuiasRecurso.class.getDeclaredMethods().length);
        assertEquals("/guias", GuiaRecurso.class.getAnnotation(Path.class).value());
    }

    @Test void listadoDelegaParametrosSinCambiarFechasNiEstadoNiInventarDefaults() throws Exception {
        var pagina = new PaginaConsultaGuiasDTO();
        var recurso = recurso((p, m, a) -> {
            assertEquals("consultar", m.getName());
            assertArrayEquals(new Object[]{1L, "2026-10-27T09:00:00-03:00", "2026-10-28T12:00:00Z", "EN_CURSO", 2, 10}, a);
            return pagina;
        });
        assertSame(pagina, recurso.consultar("1", "2026-10-27T09:00:00-03:00", "2026-10-28T12:00:00Z", "EN_CURSO", "2", "10"));
        var sinParametros = recurso((p, m, a) -> { assertArrayEquals(new Object[]{null, null, null, null, null, null}, a); return pagina; });
        assertSame(pagina, sinParametros.consultar(null, null, null, null, null, null));
    }

    @Test void detalleDelegaEmpresaEIdPositivoAlServicio() throws Exception {
        var dto = new ConsultaGuiaDTO();
        var recurso = recurso((p, m, a) -> {
            assertEquals("detalle", m.getName()); assertArrayEquals(new Object[]{1L, 10L}, a); return dto;
        });
        assertSame(dto, recurso.detalle("1", "10"));
    }

    @Test void sintaxisNumericaInvalidaEsNegocio400SinConversionImplicita404() throws Exception {
        var recurso = recurso((p, m, a) -> { fail("No delegar numeros no interpretables"); return null; });
        for (String valor : new String[]{"", "abc", "1.5", "9223372036854775808"}) {
            assertThrows(NegocioException.class, () -> recurso.consultar(valor, null, null, null, null, null));
            assertThrows(NegocioException.class, () -> recurso.detalle(valor, "10"));
            assertThrows(NegocioException.class, () -> recurso.detalle("1", valor));
        }
        assertThrows(NegocioException.class, () -> recurso.consultar("1", null, null, null, "2147483648", null));
        assertThrows(NegocioException.class, () -> recurso.consultar("1", null, null, null, null, "abc"));
    }

    @Test void ambasOperacionesPropaganNegocioYAccesoALosMappersExistentes() throws Exception {
        for (Exception causa : new Exception[]{new EJBAccessException("sintetico"), new NegocioException("sintetico")}) {
            var recurso = recurso((p, m, a) -> { throw causa; });
            assertSame(causa, assertThrows(causa.getClass(), () -> recurso.consultar("1", null, null, null, null, null)));
            assertSame(causa, assertThrows(causa.getClass(), () -> recurso.detalle("1", "10")));
        }
    }

    private static ConsultaGuiasRecurso recurso(java.lang.reflect.InvocationHandler respuesta) throws Exception {
        var recurso = new ConsultaGuiasRecurso();
        var campo = ConsultaGuiasRecurso.class.getDeclaredField("servicio"); campo.setAccessible(true);
        campo.set(recurso, Proxy.newProxyInstance(ConsultaGuiasRecursoTest.class.getClassLoader(),
                new Class<?>[]{ConsultaGuiasLocal.class}, respuesta));
        return recurso;
    }
}
