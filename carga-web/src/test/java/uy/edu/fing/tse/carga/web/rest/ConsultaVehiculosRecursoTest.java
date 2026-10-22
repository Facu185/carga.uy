package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.ConsultaVehiculosLocal;
import uy.edu.fing.tse.carga.api.dto.vehiculo.PaginaConsultaVehiculosDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/** Declaracion/delegacion; no ejecuta transporte, serializacion ni seguridad EJB. */
class ConsultaVehiculosRecursoTest {
    @Test void rutaSoloGetYServicioIndependiente() throws Exception {
        assertEquals("/vehiculos/consulta", ConsultaVehiculosRecurso.class.getAnnotation(Path.class).value());
        assertNotNull(ConsultaVehiculosRecurso.class.getMethod("consultar", String.class, String.class,
                String.class, String.class).getAnnotation(GET.class));
        assertEquals("/vehiculos", VehiculoRecurso.class.getAnnotation(Path.class).value());
    }

    @Test void delegaParametrosInterpretadosSinInventarDefaultsEnTransporte() throws Exception {
        var pagina = new PaginaConsultaVehiculosDTO();
        var recurso = recurso((p, m, a) -> {
            assertEquals("consultar", m.getName());
            assertArrayEquals(new Object[]{1L, "abc", 2, 10}, a); return pagina;
        });
        assertSame(pagina, recurso.consultar("1", "abc", "2", "10"));
        var sinParametros = recurso((p, m, a) -> {
            assertArrayEquals(new Object[]{null, null, null, null}, a); return pagina;
        });
        assertSame(pagina, sinParametros.consultar(null, null, null, null));
    }

    @Test void numerosMalFormadosSonNegocio400NoConversionJaxRs404() throws Exception {
        var recurso = recurso((p, m, a) -> { fail("No delegar datos no interpretables"); return null; });
        for (String valor : new String[]{"", "abc", "1.5", "9223372036854775808"})
            assertThrows(NegocioException.class, () -> recurso.consultar(valor, null, null, null));
        assertThrows(NegocioException.class, () -> recurso.consultar("1", null, "2147483648", null));
        assertThrows(NegocioException.class, () -> recurso.consultar("1", null, null, "abc"));
    }

    @Test void propagaErroresALosMappersExistentes() throws Exception {
        EJBAccessException acceso = new EJBAccessException("sintetico");
        var denegado = recurso((p, m, a) -> { throw acceso; });
        assertSame(acceso, assertThrows(EJBAccessException.class, () -> denegado.consultar("1", null, null, null)));
        NegocioException negocio = new NegocioException("sintetico");
        var invalido = recurso((p, m, a) -> { throw negocio; });
        assertSame(negocio, assertThrows(NegocioException.class, () -> invalido.consultar("1", null, null, null)));
    }

    private static ConsultaVehiculosRecurso recurso(java.lang.reflect.InvocationHandler respuesta) throws Exception {
        var recurso = new ConsultaVehiculosRecurso();
        var campo = ConsultaVehiculosRecurso.class.getDeclaredField("servicio"); campo.setAccessible(true);
        campo.set(recurso, Proxy.newProxyInstance(ConsultaVehiculosRecursoTest.class.getClassLoader(),
                new Class<?>[]{ConsultaVehiculosLocal.class}, respuesta));
        return recurso;
    }
}
