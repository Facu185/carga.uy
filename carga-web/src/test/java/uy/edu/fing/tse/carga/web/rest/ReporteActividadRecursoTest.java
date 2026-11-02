package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJBAccessException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.ReporteActividadLocal;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/** Escrita antes del recurso; no ejecuta HTTP/JSON-B ni interceptores de WildFly. */
class ReporteActividadRecursoTest {
    @Test void nuevoRecursoSoloExponeGet() throws Exception {
        assertEquals("/reportes/actividad", ReporteActividadRecurso.class.getAnnotation(Path.class).value());
        assertNotNull(ReporteActividadRecurso.class.getMethod("consultar", String.class, String.class).getAnnotation(GET.class));
        // Sin los metodos sinteticos que agrega JaCoCo al medir la cobertura
        assertEquals(1, java.util.Arrays.stream(ReporteActividadRecurso.class.getDeclaredMethods()).filter(m -> !m.isSynthetic()).count());
    }

    @Test void delegaFechasSinInventarDefaultsYEvitaCache() throws Exception {
        var dto = new ReporteActividadDTO();
        for (String[] periodo : new String[][]{{null, null}, {"2026-11-01", "2026-11-02"}}) {
            var recurso = recurso((p, m, a) -> { assertArrayEquals(periodo, a); return dto; });
            var cabeceras = new java.util.HashMap<String, String>();
            var c = ReporteActividadRecurso.class.getDeclaredField("respuesta"); c.setAccessible(true);
            c.set(recurso, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                    (p, m, a) -> { assertEquals("setHeader", m.getName()); cabeceras.put((String) a[0], (String) a[1]); return null; }));
            assertSame(dto, recurso.consultar(periodo[0], periodo[1]));
            assertEquals("no-store", cabeceras.get("Cache-Control"));
        }
    }

    @Test void propagaErroresDeNegocioYDeAccesoSinDevolverCero() throws Exception {
        for (Exception causa : new Exception[]{new NegocioException("sintetico"), new EJBAccessException("sintetico")}) {
            var recurso = recurso((p, m, a) -> { throw causa; });
            assertSame(causa, assertThrows(causa.getClass(), () -> recurso.consultar("invalido", null)));
        }
    }

    private static ReporteActividadRecurso recurso(java.lang.reflect.InvocationHandler respuesta) throws Exception {
        var recurso = new ReporteActividadRecurso();
        var c = ReporteActividadRecurso.class.getDeclaredField("servicio"); c.setAccessible(true);
        c.set(recurso, Proxy.newProxyInstance(ReporteActividadRecursoTest.class.getClassLoader(),
                new Class<?>[]{ReporteActividadLocal.class}, respuesta));
        var cabecera = ReporteActividadRecurso.class.getDeclaredField("respuesta"); cabecera.setAccessible(true);
        cabecera.set(recurso, Proxy.newProxyInstance(ReporteActividadRecursoTest.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (p, m, a) -> null));
        return recurso;
    }
}
