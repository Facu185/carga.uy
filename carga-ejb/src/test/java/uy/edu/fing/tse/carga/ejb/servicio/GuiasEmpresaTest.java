package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.ConsultaGuiasInvalidaException;
import uy.edu.fing.tse.carga.ejb.modelo.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Dobles JPA: verifican consultas y DTO, no ejecutan JPQL ni interceptores de WildFly. */
class GuiasEmpresaTest {
    @Test void incluyeAsignadasYFinalizadasConChoferInactivo() throws Exception {
        Escenario e = new Escenario();
        e.chofer.setActivo(false);
        Guia finalizada = e.guia(11L);
        campo(finalizada, "estado", "FINALIZADA");
        e.guias = List.of(e.guia(12L), finalizada);

        var pagina = e.bean.guiasDeLaEmpresaResponsable(1L, 0, 20);

        assertEquals(2, pagina.getTotal());
        assertEquals(0, pagina.getPagina());
        assertEquals(20, pagina.getTamano());
        assertEquals(List.of("ASIGNADA", "FINALIZADA"), pagina.getRegistros().stream()
                .map(r -> r.getGuia().getEstado()).toList());
        assertEquals("Chofer de prueba", pagina.getRegistros().getFirst().getNombreChofer());
        assertEquals(3L, pagina.getRegistros().getFirst().getGuia().getChoferId());
        assertEquals("PRUEBA", pagina.getRegistros().getFirst().getMatricula());
        assertFalse(e.consultaGuias.contains("activo"));
        assertFalse(e.consultaGuias.contains("g.estado"));
    }

    @Test void exigeVinculoResponsableActivoDelPrincipalParaLaEmpresa() throws Exception {
        Escenario e = new Escenario();
        e.bean.guiasDeLaEmpresaResponsable(1L, 2, 20);
        assertTrue(e.consultaVinculo.contains("x.usuario.cedula = :cedula"));
        assertTrue(e.consultaVinculo.contains("x.empresa.id = :empresaId"));
        assertTrue(e.consultaVinculo.contains("x.tipo = :tipo"));
        assertTrue(e.consultaVinculo.contains("x.estado = :estado"));
        assertEquals("responsable-prueba", e.parametrosVinculo.get("cedula"));
        assertEquals(1L, e.parametrosVinculo.get("empresaId"));
        assertEquals(TipoVinculoEmpresa.RESPONSABLE, e.parametrosVinculo.get("tipo"));
        assertEquals(EstadoVinculoEmpresa.ACTIVO, e.parametrosVinculo.get("estado"));
        assertTrue(e.consultaGuias.contains("g.empresa.id = :empresaId"));
        assertTrue(e.consultaConteo.contains("g.empresa.id = :empresaId"));
        assertEquals(1L, e.parametrosGuias.get("empresaId"));
        assertEquals(1L, e.parametrosConteo.get("empresaId"));
        assertTrue(e.consultaGuias.contains("order by g.inicio desc, g.id desc"));
        assertEquals(40, e.desde);
        assertEquals(20, e.limite);
    }

    @Test void rechazaOtraEmpresaSinConsultarGuias() throws Exception {
        Escenario e = new Escenario();
        assertThrows(NegocioException.class, () -> e.bean.guiasDeLaEmpresaResponsable(99L, 0, 20));
        assertNull(e.consultaGuias);
        assertNull(e.consultaConteo);
    }

    @Test void rechazaVinculoPendienteRevocadoODeOperadorSinConsultarGuias() throws Exception {
        // El doble devuelve ausencia de coincidencia con RESPONSABLE/ACTIVO.
        Escenario e = new Escenario();
        e.autorizado = false;
        assertThrows(NegocioException.class, () -> e.bean.guiasDeLaEmpresaResponsable(1L, 0, 20));
        assertNull(e.consultaGuias);
    }

    @Test void rechazaSinPrincipal() throws Exception {
        Escenario e = new Escenario();
        e.bean.contexto = null;
        assertThrows(NegocioException.class, () -> e.bean.guiasDeLaEmpresaResponsable(1L, 0, 20));
        assertNull(e.consultaVinculo);
    }

    @Test void validaEmpresaYPaginacionSinDesbordar() throws Exception {
        Escenario e = new Escenario();
        assertThrows(ConsultaGuiasInvalidaException.class, () -> e.bean.guiasDeLaEmpresaResponsable(null, 0, 20));
        assertThrows(ConsultaGuiasInvalidaException.class, () -> e.bean.guiasDeLaEmpresaResponsable(0L, 0, 20));
        assertThrows(ConsultaGuiasInvalidaException.class, () -> e.bean.guiasDeLaEmpresaResponsable(1L, -1, 20));
        assertThrows(ConsultaGuiasInvalidaException.class, () -> e.bean.guiasDeLaEmpresaResponsable(1L, 0, 0));
        assertThrows(ConsultaGuiasInvalidaException.class, () -> e.bean.guiasDeLaEmpresaResponsable(1L, 0, 101));
        assertThrows(ConsultaGuiasInvalidaException.class, () -> e.bean.guiasDeLaEmpresaResponsable(1L, Integer.MAX_VALUE, 20));
    }

    @Test void devuelvePaginaVacia() throws Exception {
        Escenario e = new Escenario();
        var pagina = e.bean.guiasDeLaEmpresaResponsable(1L, 0, 20);
        assertTrue(pagina.getRegistros().isEmpty());
        assertEquals(0, pagina.getTotal());
    }

    @Test void conservaGuiasHistoricasSinAsignaciones() throws Exception {
        Escenario e = new Escenario();
        Guia guia = e.guia(13L);
        campo(guia, "chofer", null);
        campo(guia, "vehiculo", null);
        campo(guia, "estado", "CREADA");
        e.guias = List.of(guia);
        var registro = e.bean.guiasDeLaEmpresaResponsable(1L, 0, 20).getRegistros().getFirst();
        assertEquals("CREADA", registro.getGuia().getEstado());
        assertNull(registro.getGuia().getChoferId());
        assertNull(registro.getGuia().getVehiculoId());
        assertNull(registro.getNombreChofer());
        assertNull(registro.getMatricula());
        assertTrue(e.consultaGuias.contains("left join fetch g.chofer"));
        assertTrue(e.consultaGuias.contains("left join fetch g.vehiculo"));
    }

    private static void campo(Object objeto, String nombre, Object valor) throws Exception {
        var campo = objeto.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(objeto, valor);
    }

    private static class Escenario {
        final GuiaServicioBean bean = new GuiaServicioBean();
        final Empresa empresa = new Empresa();
        final Chofer chofer = new Chofer();
        final Vehiculo vehiculo = new Vehiculo();
        List<Guia> guias = List.of();
        boolean autorizado = true;
        String consultaVinculo, consultaGuias, consultaConteo;
        Map<String, Object> parametrosVinculo, parametrosGuias, parametrosConteo;
        int desde, limite;

        Escenario() throws Exception {
            campo(empresa, "id", 1L);
            campo(chofer, "id", 3L);
            campo(vehiculo, "id", 4L);
            chofer.setNombre("Chofer de prueba");
            chofer.setEmpresa(empresa);
            vehiculo.setEmpresa(empresa);
            vehiculo.setMatricula("PRUEBA");
            bean.contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{SessionContext.class}, (p, m, a) -> {
                        if (m.getName().equals("getCallerPrincipal")) return (java.security.Principal) () -> "responsable-prueba";
                        throw new UnsupportedOperationException(m.getName());
                    });
            bean.em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{EntityManager.class}, (p, m, a) -> {
                        if (m.getName().equals("createQuery")) return consulta((String) a[0]);
                        throw new UnsupportedOperationException(m.getName());
                    });
        }

        Guia guia(Long id) throws Exception {
            Guia guia = new Guia(empresa, vehiculo, chofer, null, null, BigDecimal.ONE, BigDecimal.ONE,
                    Instant.parse("2026-10-01T10:00:00Z"), Instant.parse("2026-10-01T11:00:00Z"),
                    "Origen", null, null, "Destino", null, null);
            campo(guia, "id", id);
            return guia;
        }

        TypedQuery<?> consulta(String jpql) {
            boolean vinculo = jpql.contains("from VinculoEmpresaUsuario");
            boolean conteo = jpql.startsWith("select count(g)");
            Map<String, Object> parametros = new HashMap<>();
            if (vinculo) { consultaVinculo = jpql; parametrosVinculo = parametros; }
            else if (conteo) { consultaConteo = jpql; parametrosConteo = parametros; }
            else { consultaGuias = jpql; parametrosGuias = parametros; }
            return (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{TypedQuery.class}, (p, m, a) -> switch (m.getName()) {
                        case "setParameter" -> { parametros.put((String) a[0], a[1]); yield p; }
                        case "setFirstResult" -> { desde = (Integer) a[0]; yield p; }
                        case "setMaxResults" -> { if (!vinculo) limite = (Integer) a[0]; yield p; }
                        case "getResultStream" -> autorizado && Objects.equals(1L, parametros.get("empresaId"))
                                ? List.of(empresa).stream() : List.of().stream();
                        case "getResultList" -> guias;
                        case "getSingleResult" -> (long) guias.size();
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
        }
    }
}
