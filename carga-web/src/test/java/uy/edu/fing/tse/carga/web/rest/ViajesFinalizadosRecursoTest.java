package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJBAccessException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.ReporteActividadLocal;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Delegacion con dobles, no prueba HTTP/JSON-B ni roles reales de WildFly. */
class ViajesFinalizadosRecursoTest {
    @Test void endpointAditivoSoloExponeLecturaYFiltrosOpcionales() throws Exception {
        assertEquals("/reportes/viajes-finalizados", ViajesFinalizadosRecurso.class.getAnnotation(Path.class).value());
        var metodo = ViajesFinalizadosRecurso.class.getMethod("consultar", String.class, String.class, String.class);
        assertNotNull(metodo.getAnnotation(GET.class));
        assertEquals(java.util.List.of("desde", "hasta", "tipoCargaId"), java.util.Arrays.stream(metodo.getParameters())
                .map(p -> p.getAnnotation(QueryParam.class).value()).toList());
        assertEquals(1, java.util.Arrays.stream(ViajesFinalizadosRecurso.class.getDeclaredMethods()).filter(m -> !m.isSynthetic()).count());
    }

    @Test void delegaAFinalizadosSinCambiarFiltrosYEvitaCache() throws Exception {
        var dto = new ReporteActividadDTO();
        for (String[] filtros : new String[][]{{null, null, null}, {"2026-11-04", "2026-11-05", "7"}, {null, null, ""}}) {
            Map<String, String> cabeceras = new HashMap<>();
            var recurso = recurso((p, m, a) -> {
                assertEquals("viajesFinalizados", m.getName()); assertArrayEquals(filtros, a); return dto;
            }, cabeceras);
            assertSame(dto, recurso.consultar(filtros[0], filtros[1], filtros[2]));
            assertEquals("no-store", cabeceras.get("Cache-Control"));
        }
    }

    @Test void errorNoSeConvierteEnReporteVacio() throws Exception {
        for (Exception causa : new Exception[]{new NegocioException("sintetico"), new EJBAccessException("sintetico")}) {
            var recurso = recurso((p, m, a) -> { throw causa; }, new HashMap<>());
            assertSame(causa, assertThrows(causa.getClass(), () -> recurso.consultar(null, null, "7")));
        }
    }

    private static ViajesFinalizadosRecurso recurso(java.lang.reflect.InvocationHandler servicio,
                                                    Map<String, String> cabeceras) throws Exception {
        var recurso = new ViajesFinalizadosRecurso();
        var campo = ViajesFinalizadosRecurso.class.getDeclaredField("servicio"); campo.setAccessible(true);
        campo.set(recurso, Proxy.newProxyInstance(ViajesFinalizadosRecursoTest.class.getClassLoader(),
                new Class<?>[]{ReporteActividadLocal.class}, servicio));
        campo = ViajesFinalizadosRecurso.class.getDeclaredField("respuesta"); campo.setAccessible(true);
        campo.set(recurso, Proxy.newProxyInstance(ViajesFinalizadosRecursoTest.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (p, m, a) -> {
                    assertEquals("setHeader", m.getName()); cabeceras.put((String) a[0], (String) a[1]); return null;
                }));
        return recurso;
    }
}
