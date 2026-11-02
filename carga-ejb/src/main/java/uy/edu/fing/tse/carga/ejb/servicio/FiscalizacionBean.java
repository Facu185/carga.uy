package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.FiscalizacionLocal;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CasoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ConteoInfraccionesDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.PaginaCasosDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ReporteFiscalizacionDTO;
import uy.edu.fing.tse.carga.api.exception.ConflictoException;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.CasoFiscalizacion;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoCaso;
import uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion de casos de fiscalizacion (CU6, frente P5, ADR-0022): bandeja del funcionario, detalle con la
 * evidencia y el descargo, y resolucion fundada. Indicadores para la autoridad.
 */
@Stateless
@RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
public class FiscalizacionBean implements FiscalizacionLocal {

    @EJB
    FiscalizacionDAO dao;

    @EJB
    AuditoriaLocal auditoria;

    @Resource
    SessionContext contexto;

    @Override
    public PaginaCasosDTO bandeja(String estado, String tipo, Integer pagina, Integer tamano) throws NegocioException {
        EstadoCaso e = Casos.estado(estado);
        TipoInfraccion t = Casos.tipo(tipo);
        int p = Casos.pagina(pagina);
        int n = Casos.tamano(tamano);
        List<CasoFiscalizacion> casos = dao.buscar(null, e, t, p * n, n);
        return Casos.pagina(casos, dao.descargos(casos.stream().map(CasoFiscalizacion::getId).toList()),
                dao.contar(null, e, t), p, n);
    }

    @Override
    public CasoDTO caso(Long casoId) throws NegocioException {
        CasoFiscalizacion c = existente(casoId);
        return Casos.aDTO(c, dao.descargo(casoId).orElse(null), Instant.now());
    }

    @Override
    public CasoDTO resolver(Long casoId, boolean confirmar, String fundamento) throws NegocioException {
        String f = Casos.fundamento(fundamento, "fundamento de la resolución");
        CasoFiscalizacion c = existente(casoId);
        Instant ahora = Instant.now();
        if (c.getEstado().resuelto()) {
            throw new ConflictoException("El caso ya está resuelto (" + c.getEstado() + ").");
        }
        // Confirmar sin escuchar a la empresa solo si ya se le dio la oportunidad: vencido el plazo
        if (confirmar && c.getEstado() == EstadoCaso.NOTIFICADA && !c.plazoVencido(ahora)) {
            throw new ConflictoException("La empresa todavía puede presentar su descargo (hasta "
                    + c.getVenceDescargo() + "). Se puede desestimar ahora, pero confirmar solo después.");
        }
        EstadoCaso anterior = c.getEstado();
        try {
            c.resolver(confirmar, usuario(), f, ahora);
        } catch (IllegalStateException e) {
            throw new ConflictoException(e.getMessage());
        }
        auditoria.registrar(AccionAuditada.CASO_RESUELTO, "CasoFiscalizacion", casoId, f,
                anterior + " -> " + c.getEstado());
        return Casos.aDTO(c, dao.descargo(casoId).orElse(null), ahora);
    }

    @Override
    @RolesAllowed({Roles.AUTORIDAD, Roles.ADMINISTRADOR})
    public ReporteFiscalizacionDTO reporte(String desde, String hasta) throws NegocioException {
        LocalDate d = dia(desde, "desde");
        LocalDate h = dia(hasta, "hasta");
        if (h.isBefore(d)) throw new NegocioException("El período está invertido.");
        if (d.plusDays(366).isBefore(h)) throw new NegocioException("El período puede abarcar hasta 366 días.");

        Map<TipoInfraccion, ConteoInfraccionesDTO> porTipo = new EnumMap<>(TipoInfraccion.class);
        for (TipoInfraccion t : TipoInfraccion.values()) {
            ConteoInfraccionesDTO c = new ConteoInfraccionesDTO();
            c.setTipo(t.name());
            porTipo.put(t, c);
        }
        ReporteFiscalizacionDTO r = new ReporteFiscalizacionDTO();
        r.setDesde(d.toString());
        r.setHasta(h.toString());
        for (Object[] f : dao.conteoPorTipoYEstado(d.atStartOfDay(DeteccionBean.ZONA).toInstant(),
                h.plusDays(1).atStartOfDay(DeteccionBean.ZONA).toInstant())) {
            ConteoInfraccionesDTO c = porTipo.get((TipoInfraccion) f[0]);
            EstadoCaso estado = (EstadoCaso) f[1];
            long cantidad = ((Number) f[2]).longValue();
            c.setDetectados(c.getDetectados() + cantidad);
            r.setDetectados(r.getDetectados() + cantidad);
            if (estado == EstadoCaso.CONFIRMADA) {
                c.setConfirmados(c.getConfirmados() + cantidad);
                r.setConfirmados(r.getConfirmados() + cantidad);
            } else if (estado == EstadoCaso.DESESTIMADA) {
                c.setDesestimados(c.getDesestimados() + cantidad);
                r.setDesestimados(r.getDesestimados() + cantidad);
            } else {
                r.setPendientes(r.getPendientes() + cantidad);
            }
        }
        r.setPorTipo(List.copyOf(porTipo.values()));
        r.setTasaConfirmacion(r.getDetectados() == 0 ? null : (double) r.getConfirmados() / r.getDetectados());
        return r;
    }

    private CasoFiscalizacion existente(Long casoId) throws NegocioException {
        if (casoId == null || casoId <= 0) throw new NegocioException("El caso indicado no es válido.");
        return dao.caso(casoId).orElseThrow(() -> new NegocioException("No existe el caso indicado."));
    }

    private String usuario() {
        return contexto == null || contexto.getCallerPrincipal() == null ? "desconocido"
                : contexto.getCallerPrincipal().getName();
    }

    private static LocalDate dia(String valor, String campo) throws NegocioException {
        try {
            return LocalDate.parse(valor == null ? "" : valor.trim());
        } catch (DateTimeParseException e) {
            throw new NegocioException("'" + campo + "' tiene que tener el formato aaaa-mm-dd.");
        }
    }
}
