package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.VehiculoServicioLocal;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/** Contrato declarado y delegacion: no ejecuta JAX-RS, EJB ni serializacion HTTP. */
class VehiculoRecursoTest {
    @Test void reactivacionEsPutConRutaPropiaSinDto() throws Exception {
        assertEquals("/vehiculos", VehiculoRecurso.class.getAnnotation(Path.class).value());
        var metodo = VehiculoRecurso.class.getMethod("reactivar", Long.class);
        assertNotNull(metodo.getAnnotation(PUT.class));
        assertEquals("/{id}/reactivar", metodo.getAnnotation(Path.class).value());
    }

    @Test void reactivacionDelegaIdentificadorYPropagaDenegacionAlMapper() throws Exception {
        VehiculoRecurso recurso = new VehiculoRecurso();
        EJBAccessException rechazo = new EJBAccessException("denegado de prueba");
        var campo = VehiculoRecurso.class.getDeclaredField("servicio");
        campo.setAccessible(true);
        campo.set(recurso, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{VehiculoServicioLocal.class}, (p, m, a) -> {
            assertEquals("reactivar", m.getName());
            assertArrayEquals(new Object[]{10L}, a);
            throw rechazo;
        }));
        assertSame(rechazo, assertThrows(EJBAccessException.class, () -> recurso.reactivar(10L)));
    }
}
