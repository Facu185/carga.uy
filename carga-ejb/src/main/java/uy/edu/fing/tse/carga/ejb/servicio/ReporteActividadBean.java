package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import uy.edu.fing.tse.carga.api.contract.service.ReporteActividadLocal;
import uy.edu.fing.tse.carga.api.dto.reporte.ConteoActividadDTO;
import uy.edu.fing.tse.carga.api.dto.reporte.ReporteActividadDTO;
import uy.edu.fing.tse.carga.api.dto.guia.TipoCargaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.persistencia.ReporteActividadDAO;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Politicas temporales ADR-0020/0023; no modifica estado ni consulta integraciones externas. */
@Stateless
@DeclareRoles(Roles.AUTORIDAD)
@RolesAllowed(Roles.AUTORIDAD)
public class ReporteActividadBean implements ReporteActividadLocal {
    private static final ZoneId ZONA = ZoneId.of("America/Montevideo");
    private static final int MAXIMO_DIAS = 366;
    private static final List<String> ESTADOS = List.of("CREADA", "ASIGNADA", "EN_CURSO", "FINALIZADA");

    @EJB
    private ReporteActividadDAO dao;

    @Resource
    private SessionContext contexto;

    private Clock reloj = Clock.system(ZONA);

    @Override
    public ReporteActividadDTO consultar(String desde, String hasta) throws NegocioException {
        return consultar(desde, hasta, null);
    }

    @Override
    public ReporteActividadDTO consultar(String desde, String hasta, String tipoCargaId) throws NegocioException {
        return consultarPeriodo(desde, hasta, tipoCargaId, false);
    }

    @Override
    public ReporteActividadDTO viajesFinalizados(String desde, String hasta, String tipoCargaId) throws NegocioException {
        return consultarPeriodo(desde, hasta, tipoCargaId, true);
    }

    private ReporteActividadDTO consultarPeriodo(String desde, String hasta, String tipoCargaId, boolean finalizados)
            throws NegocioException {
        autorizar();
        LocalDate inicio, fin;
        if (desde == null && hasta == null) {
            fin = LocalDate.now(reloj.withZone(ZONA)); inicio = fin.minusDays(29);
        } else {
            inicio = fecha(desde); fin = fecha(hasta);
        }
        long diferencia = ChronoUnit.DAYS.between(inicio, fin);
        if (diferencia < 0 || diferencia >= MAXIMO_DIAS)
            throw new NegocioException("El periodo debe estar ordenado y abarcar entre 1 y 366 dias inclusivos.");

        Long tipoId = tipoId(tipoCargaId);
        TipoCargaDTO tipo = tipoId == null ? null : dao.tipoCarga(tipoId)
                .orElseThrow(() -> new NegocioException("El tipo de carga indicado no existe."));
        // El dia siguiente se calcula en calendario local, nunca sumando 24 horas a un instante.
        var desdeInstante = inicio.atStartOfDay(ZONA).toInstant();
        var hastaInstante = fin.plusDays(1).atStartOfDay(ZONA).toInstant();
        var resultado = finalizados ? dao.consultarFinalizados(desdeInstante, hastaInstante, tipoId)
                : dao.consultar(desdeInstante, hastaInstante, tipoId);
        resultado.setDesde(inicio.toString()); resultado.setHasta(fin.toString());
        resultado.setZonaHoraria(ZONA.getId()); resultado.setCriterioTemporal(finalizados ? "FIN_VIAJE" : "CREACION_GUIA");
        resultado.setTipoCargaId(tipoId); resultado.setTipoCargaNombre(tipo == null ? null : tipo.getNombre());
        Map<String, Long> dias = cantidades(resultado.getPorDia()), estados = cantidades(resultado.getPorEstado());
        var serie = new ArrayList<ConteoActividadDTO>();
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            String clave = dia.toString();
            serie.add(new ConteoActividadDTO(clave, clave, dias.getOrDefault(clave, 0L)));
        }
        resultado.setPorDia(serie);
        resultado.setPorEstado(ESTADOS.stream().map(estado ->
                new ConteoActividadDTO(estado, estado, estados.getOrDefault(estado, 0L))).toList());
        return resultado;
    }

    @Override
    public List<TipoCargaDTO> tiposCarga() {
        autorizar();
        return dao.tiposCarga();
    }

    private static Long tipoId(String valor) throws NegocioException {
        if (valor == null) return null;
        if (!valor.matches("[1-9][0-9]*"))
            throw new NegocioException("El tipo de carga debe ser un identificador entero positivo.");
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new NegocioException("El identificador del tipo de carga es demasiado grande.");
        }
    }

    private void autorizar() {
        if (contexto == null || contexto.getCallerPrincipal() == null
                || contexto.getCallerPrincipal().getName() == null || contexto.getCallerPrincipal().getName().isBlank()
                || !contexto.isCallerInRole(Roles.AUTORIDAD))
            throw new EJBAccessException("No tienes permiso para consultar reportes de autoridad.");
    }

    private static LocalDate fecha(String valor) throws NegocioException {
        if (valor == null || !valor.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
            throw new NegocioException("Completa desde y hasta con fechas YYYY-MM-DD.");
        try {
            LocalDate fecha = LocalDate.parse(valor);
            if (fecha.getYear() < 1) throw new NegocioException("El anio de la fecha debe ser positivo.");
            return fecha;
        } catch (DateTimeParseException e) {
            throw new NegocioException("Desde y hasta deben ser fechas de calendario validas.");
        }
    }

    private static Map<String, Long> cantidades(List<ConteoActividadDTO> grupos) {
        return grupos.stream().collect(Collectors.toMap(ConteoActividadDTO::getClave, ConteoActividadDTO::getCantidad));
    }
}
