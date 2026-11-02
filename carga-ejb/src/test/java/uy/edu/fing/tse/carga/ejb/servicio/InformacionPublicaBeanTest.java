package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.publico.EstadisticasPublicasDTO;
import uy.edu.fing.tse.carga.api.dto.publico.ValidacionEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.publico.ValidacionVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.persistencia.InformacionPublicaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.InformacionPublicaDAO.Fila;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InformacionPublicaBeanTest {

    /** 2 de noviembre de 2026, 10:00 en Montevideo. */
    private static final Instant AHORA = Instant.parse("2026-11-02T13:00:00Z");

    private final Map<String, Vehiculo> vehiculos = new HashMap<>();
    private final Map<Long, Empresa> empresas = new HashMap<>();
    private final List<Fila> guias = new ArrayList<>();
    private final List<Fila> flota = new ArrayList<>();
    private Instant guiasDesde;
    private Instant guiasHasta;
    private InformacionPublicaBean bean;

    @BeforeEach
    void preparar() {
        bean = new InformacionPublicaBean();
        bean.reloj = Clock.fixed(AHORA, InformacionPublicaBean.ZONA);
        bean.dao = new InformacionPublicaDAO() {
            @Override
            public Optional<Vehiculo> vehiculo(String matricula) {
                return Optional.ofNullable(vehiculos.get(matricula));
            }

            @Override
            public Optional<Empresa> empresa(long numero) {
                return Optional.ofNullable(empresas.get(numero));
            }

            @Override
            public List<Fila> guias(Instant desde, Instant hasta) {
                guiasDesde = desde;
                guiasHasta = hasta;
                return guias;
            }

            @Override
            public List<Fila> flota(LocalDate dia) {
                return flota;
            }
        };
        flota.addAll(List.of(fila("EMPRESAS", 4, 1, 2, 3, 4), fila("HABILITADOS", 12, 1, 2, 3, 4),
                fila("AL_DIA", 7, 1, 2, 3), fila("NO_AL_DIA", 5, 2, 3, 4)));
    }

    @Test
    void unVehiculoAlDiaEsVigenteYLaMatriculaSeNormaliza() throws Exception {
        vehiculos.put("STZ1234", vehiculo(true, true, "PNC-1", "2026-01-01", "2026-12-31", "2027-03-01"));

        ValidacionVehiculoDTO r = bean.validarVehiculo(" stz-1234 ");

        assertTrue(r.isEncontrado());
        assertTrue(r.isVigente());
        assertEquals("VIGENTE", r.getEstadoPermiso());
        assertEquals("VIGENTE", r.getEstadoItv());
        assertEquals("2026-11-02", r.getFechaReferencia());
    }

    @Test
    void conLaItvVencidaOLaEmpresaDeBajaNoEsVigente() throws Exception {
        vehiculos.put("ITV0001", vehiculo(true, true, "PNC-1", "2026-01-01", "2026-12-31", "2026-11-01"));
        vehiculos.put("BAJ0001", vehiculo(true, false, "PNC-1", "2026-01-01", "2026-12-31", "2027-03-01"));

        ValidacionVehiculoDTO itv = bean.validarVehiculo("ITV0001");
        ValidacionVehiculoDTO baja = bean.validarVehiculo("BAJ0001");

        assertFalse(itv.isVigente());
        assertEquals("VENCIDO", itv.getEstadoItv());
        assertFalse(baja.isVigente());
        assertFalse(baja.isEmpresaHabilitada());
    }

    @Test
    void unPermisoQueEmpiezaMasAdelanteEsFuturoYSinNumeroEsSinDatos() throws Exception {
        vehiculos.put("FUT0001", vehiculo(true, true, "PNC-1", "2026-12-01", "2027-12-31", "2027-03-01"));
        vehiculos.put("SIN0001", vehiculo(true, true, " ", "2026-01-01", "2026-12-31", "2027-03-01"));

        assertEquals("FUTURO", bean.validarVehiculo("FUT0001").getEstadoPermiso());
        assertEquals("SIN_DATOS", bean.validarVehiculo("SIN0001").getEstadoPermiso());
    }

    @Test
    void unaMatriculaQueNoExisteNoEsUnError() throws Exception {
        ValidacionVehiculoDTO r = bean.validarVehiculo("NOX9999");

        assertFalse(r.isEncontrado());
        assertFalse(r.isVigente());
    }

    @Test
    void rechazaMatriculasYNumerosInvalidos() {
        assertThrows(NegocioException.class, () -> bean.validarVehiculo(" "));
        assertThrows(NegocioException.class, () -> bean.validarVehiculo("STZ'; DROP"));
        assertThrows(NegocioException.class, () -> bean.validarVehiculo("A".repeat(21)));
        assertThrows(NegocioException.class, () -> bean.validarEmpresa("abc"));
        assertThrows(NegocioException.class, () -> bean.validarEmpresa("0"));
    }

    @Test
    void laEmpresaDevuelveSoloNombreYHabilitacion() throws Exception {
        Empresa e = new Empresa();
        set(e, "nombre", "Transportes del Este");
        set(e, "activa", true);
        empresas.put(1001L, e);

        ValidacionEmpresaDTO r = bean.validarEmpresa(" 1001 ");

        assertTrue(r.isEncontrada());
        assertTrue(r.isHabilitada());
        assertEquals("Transportes del Este", r.getNombre());
        assertFalse(bean.validarEmpresa("2002").isEncontrada());
    }

    @Test
    void lasEstadisticasCubrenDoceMesesCompletosConLosVaciosEnCero() {
        guias.add(new Fila("MES", "2026-10", "2026-10", 9, Set.of(1L, 2L, 3L), 90_000));

        EstadisticasPublicasDTO r = bean.estadisticas();

        assertEquals("2025-12", r.getDesde());
        assertEquals("2026-11", r.getHasta());
        assertEquals(12, r.getPorMes().size());
        assertEquals(0L, r.getPorMes().get(0).getGuias());
        assertEquals(9L, r.getPorMes().get(10).getGuias());
        // desde el 1/12/2025 a las 00:00 de Montevideo hasta el 1/12/2026, excluido
        assertEquals(Instant.parse("2025-12-01T03:00:00Z"), guiasDesde);
        assertEquals(Instant.parse("2026-12-01T03:00:00Z"), guiasHasta);
        assertEquals(4L, r.getEmpresasHabilitadas());
        assertEquals(12L, r.getVehiculosHabilitados());
        assertEquals(7L, r.getVehiculosAlDia());
    }

    @Test
    void noPublicaLosVehiculosAlDiaSiElComplementoIdentificariaAUnaEmpresa() {
        flota.removeIf(f -> f.grupo().equals("NO_AL_DIA"));
        flota.add(fila("NO_AL_DIA", 5, 4));

        EstadisticasPublicasDTO r = bean.estadisticas();

        assertEquals(12L, r.getVehiculosHabilitados());
        assertNull(r.getVehiculosAlDia());
    }

    private static Fila fila(String grupo, long cantidad, long... empresas) {
        return new Fila(grupo, "", "", cantidad,
                java.util.Arrays.stream(empresas).boxed().collect(java.util.stream.Collectors.toSet()), 0);
    }

    private static Vehiculo vehiculo(boolean habilitado, boolean empresaActiva, String permiso,
                                     String desde, String hasta, String itv) throws Exception {
        Empresa e = new Empresa();
        set(e, "activa", empresaActiva);
        Vehiculo v = new Vehiculo();
        set(v, "empresa", e);
        set(v, "matricula", "STZ1234");
        set(v, "habilitado", habilitado);
        set(v, "numeroPermiso", permiso);
        set(v, "permisoDesde", LocalDate.parse(desde));
        set(v, "permisoHasta", LocalDate.parse(hasta));
        set(v, "itvVencimiento", LocalDate.parse(itv));
        return v;
    }

    private static void set(Object objeto, String campo, Object valor) throws Exception {
        Field f = objeto.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(objeto, valor);
    }
}
