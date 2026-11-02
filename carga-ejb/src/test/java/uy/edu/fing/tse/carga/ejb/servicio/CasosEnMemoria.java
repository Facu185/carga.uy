package uy.edu.fing.tse.carga.ejb.servicio;

import uy.edu.fing.tse.carga.ejb.modelo.CasoFiscalizacion;
import uy.edu.fing.tse.carga.ejb.modelo.Descargo;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoCaso;
import uy.edu.fing.tse.carga.ejb.modelo.ParametroRegla;
import uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** FiscalizacionDAO en memoria con la semantica de la base: el hecho es unico (un caso por hecho). */
class CasosEnMemoria extends FiscalizacionDAO {

    final Map<String, CasoFiscalizacion> porHecho = new LinkedHashMap<>();
    final Map<Long, CasoFiscalizacion> porId = new HashMap<>();
    final Map<Long, Descargo> descargos = new HashMap<>();
    final Map<String, Vehiculo> vehiculos = new HashMap<>();
    final Map<Long, String> estadosGuia = new HashMap<>();
    final Map<String, String> parametros = new HashMap<>();
    final List<String> notificaciones = new ArrayList<>();
    final List<Actividad> actividad = new ArrayList<>();
    Recorrido recorrido;
    Vehiculo vehiculoDeGuia;
    private long siguiente = 1;

    @Override
    public Optional<Long> crearSiEsNuevo(TipoInfraccion tipo, String hecho, Long empresaId, Long vehiculoId,
                                         String matricula, Long guiaId, String evidencia) {
        if (porHecho.containsKey(hecho)) {
            return Optional.empty();
        }
        Empresa empresa = Falsos.conId(new Empresa(), empresaId);
        CasoFiscalizacion c = new CasoFiscalizacion(siguiente++, tipo, EstadoCaso.DETECTADA, empresa, matricula);
        poner(c, "hecho", hecho);
        poner(c, "evidencia", evidencia);
        poner(c, "guiaId", guiaId);
        porHecho.put(hecho, c);
        porId.put(c.getId(), c);
        return Optional.of(c.getId());
    }

    /** Un caso ya notificado, para probar la gestion; vence = cuando vence el plazo de descargo. */
    CasoFiscalizacion notificado(long empresaId, Instant vence) {
        Long id = crearSiEsNuevo(TipoInfraccion.EXCESO_CARGA, "prueba:" + siguiente, empresaId, 1L, "STZ1234", null,
                "Peso registrado 31.500 kg.").orElseThrow();
        CasoFiscalizacion c = porId.get(id);
        c.notificar(vence.minusSeconds(864_000), vence);
        return c;
    }

    @Override
    public void notificar(Long empresaId, Long casoId, String canal, String mensaje) {
        notificaciones.add(empresaId + ":" + casoId + ":" + mensaje);
    }

    @Override
    public Optional<CasoFiscalizacion> caso(Long id) {
        return Optional.ofNullable(porId.get(id));
    }

    @Override
    public List<CasoFiscalizacion> buscar(Long empresaId, EstadoCaso estado, TipoInfraccion tipo, int desde, int cantidad) {
        return porId.values().stream()
                .filter(c -> empresaId == null || c.getEmpresa().getId().equals(empresaId))
                .filter(c -> estado == null || c.getEstado() == estado)
                .filter(c -> tipo == null || c.getTipo() == tipo)
                .skip(desde).limit(cantidad).toList();
    }

    @Override
    public long contar(Long empresaId, EstadoCaso estado, TipoInfraccion tipo) {
        return buscar(empresaId, estado, tipo, 0, Integer.MAX_VALUE).size();
    }

    @Override
    public Optional<Descargo> descargo(Long casoId) {
        return Optional.ofNullable(descargos.get(casoId));
    }

    @Override
    public Map<Long, Descargo> descargos(List<Long> casos) {
        Map<Long, Descargo> r = new HashMap<>();
        casos.forEach(id -> { if (descargos.containsKey(id)) r.put(id, descargos.get(id)); });
        return r;
    }

    @Override
    public void guardar(Descargo descargo) {
        descargos.put(descargo.getCasoId(), descargo);
    }

    @Override
    public Optional<Vehiculo> vehiculo(String matricula) {
        return Optional.ofNullable(vehiculos.get(matricula));
    }

    @Override
    public Optional<String> estadoGuia(Long guiaId) {
        return Optional.ofNullable(estadosGuia.get(guiaId));
    }

    @Override
    public Optional<Vehiculo> vehiculoDeGuia(Long guiaId) {
        return Optional.ofNullable(vehiculoDeGuia);
    }

    @Override
    public Optional<Recorrido> recorrido(Long guiaId, double toleranciaM, double toleranciaRelativaPct) {
        return Optional.ofNullable(recorrido);
    }

    @Override
    public List<Actividad> actividadDesde(Instant desde) {
        return actividad;
    }

    @Override
    public Optional<ParametroRegla> parametro(String clave) {
        if (!parametros.containsKey(clave)) {
            return Optional.empty();
        }
        ParametroRegla p = new ParametroRegla();
        poner(p, "clave", clave);
        p.setValor(parametros.get(clave));
        return Optional.of(p);
    }

    /** Camion de 9.000 kg con 21.000 kg de capacidad: el maximo admitido es 30.000 kg. */
    static Vehiculo camion(String matricula, long empresaId, String permisoHasta, String itv) {
        Empresa empresa = Falsos.conId(new Empresa(), empresaId);
        poner(empresa, "activa", true);
        Vehiculo v = Falsos.conId(new Vehiculo(), 10L + empresaId);
        poner(v, "empresa", empresa);
        poner(v, "matricula", matricula);
        poner(v, "pesoKg", 9000);
        poner(v, "capacidadCargaKg", 21000);
        poner(v, "habilitado", true);
        poner(v, "numeroPermiso", "PNC-1");
        poner(v, "permisoDesde", LocalDate.parse("2026-01-01"));
        poner(v, "permisoHasta", LocalDate.parse(permisoHasta));
        poner(v, "itvVencimiento", LocalDate.parse(itv));
        return v;
    }

    static void poner(Object objeto, String campo, Object valor) {
        try {
            Field f = objeto.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(objeto, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
