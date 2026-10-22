package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.service.PerfilEmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.EditarPerfilEmpresaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/** Declaracion/delegacion; no ejecuta JAX-RS, JSON-B ni seguridad de contenedor. */
class EmpresaPerfilRecursoTest {
    @Test void consultaYEdicionTienenRutaEspecificaSinAlterarRutaAdministrativa() throws Exception {
        assertEquals("/empresas", EmpresaRecurso.class.getAnnotation(Path.class).value());
        var get = EmpresaRecurso.class.getMethod("consultarPerfil", Long.class);
        var put = EmpresaRecurso.class.getMethod("modificarPerfil", Long.class, EditarPerfilEmpresaDTO.class);
        assertNotNull(get.getAnnotation(GET.class)); assertNotNull(put.getAnnotation(PUT.class));
        assertEquals("/{id}/perfil", get.getAnnotation(Path.class).value());
        assertEquals("/{id}/perfil", put.getAnnotation(Path.class).value());
        assertEquals("/{id}", EmpresaRecurso.class.getMethod("modificar", Long.class, EmpresaDTO.class)
                .getAnnotation(Path.class).value());
    }

    @Test void consultaDelegaIdYDevuelveDtoDelServicio() throws Exception {
        EmpresaDTO dto = new EmpresaDTO();
        EmpresaRecurso recurso = recurso((p, m, a) -> {
            assertEquals("consultar", m.getName()); assertArrayEquals(new Object[]{1L}, a); return dto;
        });
        assertSame(dto, recurso.consultarPerfil(1L));
    }

    @Test void edicionDelegaSoloIdYDtoEstrecho() throws Exception {
        EditarPerfilEmpresaDTO datos = new EditarPerfilEmpresaDTO(); EmpresaDTO dto = new EmpresaDTO();
        EmpresaRecurso recurso = recurso((p, m, a) -> {
            assertEquals("modificar", m.getName()); assertArrayEquals(new Object[]{1L, datos}, a); return dto;
        });
        assertSame(dto, recurso.modificarPerfil(1L, datos));
    }

    @Test void denegacionYNegocioSePropaganALosMappersExistentes() throws Exception {
        EJBAccessException denegacion = new EJBAccessException("denegado sintetico");
        EmpresaRecurso denegado = recurso((p, m, a) -> { throw denegacion; });
        assertSame(denegacion, assertThrows(EJBAccessException.class, () -> denegado.consultarPerfil(1L)));
        assertSame(denegacion, assertThrows(EJBAccessException.class, () -> denegado.modificarPerfil(1L, null)));
        NegocioException negocio = new NegocioException("negocio sintetico");
        EmpresaRecurso invalido = recurso((p, m, a) -> { throw negocio; });
        assertSame(negocio, assertThrows(NegocioException.class, () -> invalido.modificarPerfil(1L, null)));
    }

    private static EmpresaRecurso recurso(java.lang.reflect.InvocationHandler respuesta) throws Exception {
        EmpresaRecurso recurso = new EmpresaRecurso();
        var campo = EmpresaRecurso.class.getDeclaredField("perfiles"); campo.setAccessible(true);
        campo.set(recurso, Proxy.newProxyInstance(EmpresaPerfilRecursoTest.class.getClassLoader(),
                new Class<?>[]{PerfilEmpresaServicioLocal.class}, respuesta));
        return recurso;
    }
}
