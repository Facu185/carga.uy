package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.planificacion.*;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.integracion.ClienteDireccionesIde;
import uy.edu.fing.tse.carga.ejb.integracion.ClienteRutasOrs;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.seguridad.CompatibilidadFuncionarioEmpresa;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;

/** Dobles: no acredita roles, JTA ni seguridad real de WildFly. */
class PlanificacionGuiaBeanTest {
    @Test void calculaTaraMasCargaDesdeVehiculoAutorizadoSinEscrituras() throws Exception {
        var e = new Escenario(); var dto = solicitud();
        e.bean.calcular(dto);
        assertEquals(new BigDecimal("20500"), e.pesoEnviado);
        assertEquals(1, e.llamadasOrs);
        assertTrue(e.ultimaConsulta.contains("RESPONSABLE") || e.parametros.containsValue(uy.edu.fing.tse.carga.ejb.modelo.TipoVinculoEmpresa.RESPONSABLE));
        assertEquals(3L, e.parametros.get("vehiculo"));
        assertEquals(TransactionAttributeType.NOT_SUPPORTED,
                PlanificacionGuiaBean.class.getAnnotation(TransactionAttribute.class).value());
    }

    @Test void sinPrincipalVinculoOEmpresaActivaNoInvocaProveedores() {
        for (boolean principal : List.of(true, false)) {
            var e = new Escenario(); e.autorizado = false; e.conPrincipal = principal;
            assertThrows(EJBAccessException.class, () -> e.bean.sugerir("sucre"));
            assertThrows(EJBAccessException.class, () -> e.bean.resolver(new SugerenciaCalleDTO()));
            assertThrows(EJBAccessException.class, () -> e.bean.calcular(solicitud()));
            assertEquals(0, e.llamadasIde + e.llamadasOrs);
        }
    }

    @Test void vehiculoAjenoOInhabilitadoNoConsumeCuotaOrs() {
        var e = new Escenario(); e.vehiculoVisible = false;
        assertThrows(EJBAccessException.class, () -> e.bean.calcular(solicitud()));
        assertEquals(0, e.llamadasOrs);
        assertTrue(e.ultimaConsulta.contains("v.habilitado = true"));
        assertTrue(e.ultimaConsulta.contains("x.empresa.activa = true"));
        assertTrue(e.ultimaConsulta.contains("x.usuario.cedula = :cedula"));
    }

    @Test void incompatibilidadHeredadaRechazaAntesDeConsultarProveedor() {
        var e = new Escenario(); e.incompatible = true;
        assertThrows(EJBAccessException.class, () -> e.bean.sugerir("sucre"));
        assertEquals(0, e.llamadasIde);
    }

    @Test void cargaCoordenadasEIdentificadorInvalidosNoCalculanRuta() {
        for (BigDecimal peso : new BigDecimal[]{null, BigDecimal.ZERO, BigDecimal.ONE.negate(), new BigDecimal("20001")}) {
            var e = new Escenario(); var s = solicitud(); s.setCargaKg(peso);
            assertThrows(NegocioException.class, () -> e.bean.calcular(s)); assertEquals(0, e.llamadasOrs);
        }
        for (Double lat : new Double[]{null, Double.NaN, 91.0}) {
            var e = new Escenario(); var s = solicitud(); s.getOrigen().setLatitud(lat);
            assertThrows(NegocioException.class, () -> e.bean.calcular(s)); assertEquals(0, e.llamadasOrs);
        }
        var e = new Escenario(); var s = solicitud(); s.setVehiculoId(0L);
        assertThrows(NegocioException.class, () -> e.bean.calcular(s));
        assertThrows(NegocioException.class, () -> e.bean.calcular(null));
    }

    private static SolicitudRutaDTO solicitud() {
        var s = new SolicitudRutaDTO(); s.setVehiculoId(3L); s.setCargaKg(new BigDecimal("12500"));
        var a = new PuntoMapaDTO(); a.setLatitud(-34.9); a.setLongitud(-56.2);
        var b = new PuntoMapaDTO(); b.setLatitud(-34.4); b.setLongitud(-55.2);
        s.setOrigen(a); s.setDestino(b); return s;
    }

    private static class Escenario {
        final PlanificacionGuiaBean bean = new PlanificacionGuiaBean();
        boolean autorizado = true, conPrincipal = true, vehiculoVisible = true, incompatible;
        int llamadasIde, llamadasOrs; BigDecimal pesoEnviado; String ultimaConsulta = "";
        Map<String,Object> parametros = new HashMap<>();

        Escenario() {
            bean.contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                    (p,m,a) -> { if (m.getName().equals("getCallerPrincipal")) return conPrincipal ? (Principal) () -> "responsable" : null;
                        throw new UnsupportedOperationException(m.getName()); });
            bean.compatibilidad = new CompatibilidadFuncionarioEmpresa() {
                @Override public void verificarEjercicioResponsabilidad(String cedula) {
                    if (incompatible) throw new EJBAccessException("incompatibilidad sintetica");
                }
            };
            bean.em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class}, (p,m,a) -> {
                if (!m.getName().equals("createQuery")) throw new UnsupportedOperationException(m.getName());
                ultimaConsulta = (String) a[0]; parametros.clear();
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class}, (q,n,b) -> switch (n.getName()) {
                    case "setParameter" -> { parametros.put((String)b[0], b[1]); yield q; }
                    case "setMaxResults" -> q;
                    case "getSingleResult" -> autorizado ? 1L : 0L;
                    case "getResultList" -> {
                        var v = new Vehiculo(); v.setPesoKg(8000); v.setCapacidadCargaKg(20000);
                        yield vehiculoVisible ? List.of(v) : List.<Vehiculo>of();
                    }
                    default -> throw new UnsupportedOperationException(n.getName());
                });
            });
            bean.ide = new ClienteDireccionesIde() {
                @Override public List<SugerenciaCalleDTO> sugerir(String texto) { llamadasIde++; return List.of(); }
                @Override public DireccionMapaDTO resolver(SugerenciaCalleDTO s) { llamadasIde++; return new DireccionMapaDTO(); }
            };
            bean.ors = new ClienteRutasOrs() {
                @Override public RutaPreviaDTO calcular(PuntoMapaDTO a, PuntoMapaDTO b, BigDecimal kg) {
                    llamadasOrs++; pesoEnviado = kg; return new RutaPreviaDTO();
                }
            };
        }
    }
}
