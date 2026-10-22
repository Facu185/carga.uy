package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.ConsultaVehiculosLocal;
import uy.edu.fing.tse.carga.api.dto.vehiculo.ConsultaVehiculoDTO;
import uy.edu.fing.tse.carga.api.dto.vehiculo.PaginaConsultaVehiculosDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/** Consulta administrativa C13 sin reutilizar ni debilitar la pertenencia del ABM. */
@Stateless
@DeclareRoles({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
@RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
public class ConsultaVehiculosBean implements ConsultaVehiculosLocal {
    private static final ZoneId ZONA = ZoneId.of("America/Montevideo");

    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @Resource
    private SessionContext contexto;

    private Clock reloj = Clock.system(ZONA);

    @Override
    public PaginaConsultaVehiculosDTO consultar(Long empresaId, String matricula, Integer pagina, Integer tamano)
            throws NegocioException {
        autorizar();
        if (empresaId == null || empresaId <= 0) throw new NegocioException("La empresa debe ser un identificador positivo.");
        int numero = pagina == null ? 0 : pagina;
        int cantidad = tamano == null ? 20 : tamano;
        if (numero < 0 || cantidad < 1 || cantidad > 100 || (long) numero * cantidad > Integer.MAX_VALUE)
            throw new NegocioException("La pagina o el tamano solicitado no son validos.");
        if (em.find(Empresa.class, empresaId) == null) throw new NegocioException("La empresa no existe.");

        String filtro = matricula == null ? "" : matricula.trim();
        String condicion = " where v.empresa.id = :empresaId"
                + (filtro.isEmpty() ? "" : " and upper(v.matricula) like :matricula escape '!'");
        var conteo = em.createQuery("select count(v) from Vehiculo v" + condicion, Long.class)
                .setParameter("empresaId", empresaId);
        var consulta = em.createQuery("select v from Vehiculo v" + condicion + " order by v.matricula, v.id", Vehiculo.class)
                .setParameter("empresaId", empresaId);
        if (!filtro.isEmpty()) {
            String patron = "%" + filtro.toUpperCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            conteo.setParameter("matricula", patron); consulta.setParameter("matricula", patron);
        }
        LocalDate referencia = LocalDate.now(reloj.withZone(ZONA));
        var resultado = new PaginaConsultaVehiculosDTO();
        resultado.setPagina(numero); resultado.setTamano(cantidad); resultado.setTotal(conteo.getSingleResult());
        resultado.setFechaReferencia(referencia.toString()); resultado.setZonaHoraria(ZONA.getId());
        resultado.setRegistros(consulta.setFirstResult(numero * cantidad).setMaxResults(cantidad)
                .getResultList().stream().map(v -> aDTO(v, referencia)).toList());
        return resultado;
    }

    private void autorizar() {
        if (contexto == null || contexto.getCallerPrincipal() == null
                || contexto.getCallerPrincipal().getName() == null || contexto.getCallerPrincipal().getName().isBlank()
                || !(contexto.isCallerInRole(Roles.FUNCIONARIO) || contexto.isCallerInRole(Roles.ADMINISTRADOR)))
            throw new EJBAccessException("No tienes permiso para consultar vehiculos.");
    }

    private ConsultaVehiculoDTO aDTO(Vehiculo v, LocalDate referencia) {
        var dto = new ConsultaVehiculoDTO();
        dto.setId(v.getId()); dto.setEmpresaId(v.getEmpresa().getId()); dto.setMatricula(v.getMatricula());
        dto.setHabilitado(v.isHabilitado()); dto.setNumeroPermiso(v.getNumeroPermiso());
        dto.setPermisoDesde(texto(v.getPermisoDesde())); dto.setPermisoHasta(texto(v.getPermisoHasta()));
        dto.setItvVencimiento(texto(v.getItvVencimiento())); dto.setEstadoPermiso(estadoPermiso(v, referencia));
        dto.setEstadoItv(v.getItvVencimiento() == null ? "SIN_DATOS"
                : referencia.isAfter(v.getItvVencimiento()) ? "VENCIDO" : "VIGENTE");
        return dto;
    }

    private String estadoPermiso(Vehiculo v, LocalDate referencia) {
        if (v.getNumeroPermiso() == null || v.getNumeroPermiso().isBlank()
                || v.getPermisoDesde() == null || v.getPermisoHasta() == null || v.getPermisoDesde().isAfter(v.getPermisoHasta()))
            return "SIN_DATOS";
        if (referencia.isBefore(v.getPermisoDesde())) return "FUTURO";
        return referencia.isAfter(v.getPermisoHasta()) ? "VENCIDO" : "VIGENTE";
    }

    private static String texto(LocalDate fecha) { return fecha == null ? null : fecha.toString(); }
}
