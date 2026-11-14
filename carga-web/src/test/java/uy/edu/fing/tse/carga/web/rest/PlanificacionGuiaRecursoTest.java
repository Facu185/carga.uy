package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJBAccessException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.PlanificacionGuiaLocal;
import uy.edu.fing.tse.carga.api.dto.planificacion.*;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PlanificacionGuiaRecursoTest {
    @Test void endpointNuevoDelegaYNoAgregaPersistencia() throws Exception {
        assertEquals("/planificacion-guias", PlanificacionGuiaRecurso.class.getAnnotation(Path.class).value());
        var llamadas = new ArrayList<String>();
        var recurso = recurso((p,m,a) -> { llamadas.add(m.getName()); return switch (m.getName()) {
            case "sugerir", "resolverAlternativas" -> List.of(); case "resolver" -> new DireccionMapaDTO(); default -> new RutaPreviaDTO();
        }; });
        assertTrue(recurso.sugerir("sucre").isEmpty());
        assertNotNull(recurso.resolver(new SugerenciaCalleDTO()));
        assertTrue(recurso.resolverAlternativas(new SugerenciaCalleDTO()).isEmpty());
        assertNotNull(recurso.calcular(new SolicitudRutaDTO()));
        assertEquals(List.of("sugerir", "resolver", "resolverAlternativas", "calcular"), llamadas);
        assertEquals("/direccion/alternativas", PlanificacionGuiaRecurso.class
                .getMethod("resolverAlternativas", SugerenciaCalleDTO.class).getAnnotation(Path.class).value());
    }

    @Test void noConviertePermisosNiErroresEnListasVacias() throws Exception {
        for (Exception e : new Exception[]{new NegocioException("sintetico"), new EJBAccessException("sintetico")}) {
            var recurso = recurso((p,m,a) -> { throw e; });
            assertSame(e, assertThrows(e.getClass(), () -> recurso.sugerir("sucre")));
            assertSame(e, assertThrows(e.getClass(), () -> recurso.resolverAlternativas(new SugerenciaCalleDTO())));
        }
    }

    @Test void alternativasConservanSelectorListaYEncabezadoSinCache() throws Exception {
        var seleccion = new SugerenciaCalleDTO();
        var opciones = List.of(new DireccionMapaDTO(), new DireccionMapaDTO());
        var recurso = recurso((p,m,a) -> {
            assertEquals("resolverAlternativas", m.getName());
            assertSame(seleccion, a[0]);
            return opciones;
        });
        assertSame(opciones, recurso.resolverAlternativas(seleccion));
    }

    private static PlanificacionGuiaRecurso recurso(java.lang.reflect.InvocationHandler servicio) throws Exception {
        var r = new PlanificacionGuiaRecurso();
        var f = PlanificacionGuiaRecurso.class.getDeclaredField("servicio"); f.setAccessible(true);
        f.set(r, Proxy.newProxyInstance(PlanificacionGuiaRecursoTest.class.getClassLoader(), new Class<?>[]{PlanificacionGuiaLocal.class}, servicio));
        f = PlanificacionGuiaRecurso.class.getDeclaredField("respuesta"); f.setAccessible(true);
        f.set(r, Proxy.newProxyInstance(PlanificacionGuiaRecursoTest.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class}, (p,m,a) -> {
            assertEquals("setHeader", m.getName()); assertArrayEquals(new String[]{"Cache-Control", "no-store"}, a); return null;
        })); return r;
    }
}
