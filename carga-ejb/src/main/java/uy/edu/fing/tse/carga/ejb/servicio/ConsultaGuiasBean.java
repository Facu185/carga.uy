package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.ConsultaGuiasLocal;
import uy.edu.fing.tse.carga.api.dto.guia.ConsultaGuiaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.PaginaConsultaGuiasDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Guia;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Lectura C14 separada de CU2 y de la consulta empresarial con pertenencia. */
@Stateless
@DeclareRoles({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
@RolesAllowed({Roles.FUNCIONARIO, Roles.ADMINISTRADOR})
public class ConsultaGuiasBean implements ConsultaGuiasLocal {
    private static final Set<String> ESTADOS = Set.of("CREADA", "ASIGNADA", "EN_CURSO", "FINALIZADA");
    private static final String ASIGNACIONES = " from Guia g left join fetch g.chofer left join fetch g.vehiculo";

    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @Resource
    private SessionContext contexto;

    @Override
    public PaginaConsultaGuiasDTO consultar(Long empresaId, String desde, String hasta, String estado,
            Integer pagina, Integer tamano) throws NegocioException {
        autorizar();
        validarId(empresaId, "La empresa");
        int numero = pagina == null ? 0 : pagina;
        int cantidad = tamano == null ? 20 : tamano;
        if (numero < 0 || cantidad < 1 || cantidad > 100 || (long) numero * cantidad > Integer.MAX_VALUE)
            throw new NegocioException("La pagina o el tamano solicitado no son validos.");

        String filtroEstado = estado == null ? "" : estado.trim();
        if (!filtroEstado.isEmpty() && !ESTADOS.contains(filtroEstado))
            throw new NegocioException("El estado solicitado no es valido.");
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("empresaId", empresaId);
        String condicion = " where g.empresa.id = :empresaId";
        if (desde != null || hasta != null) {
            Instant inicio = instante(desde), fin = instante(hasta);
            if (!inicio.isBefore(fin)) throw new NegocioException("Desde debe ser anterior a hasta.");
            condicion += " and g.inicio < :hasta and g.fin > :desde";
            parametros.put("desde", inicio); parametros.put("hasta", fin);
        }
        if (!filtroEstado.isEmpty()) {
            condicion += " and g.estado = :estado";
            parametros.put("estado", filtroEstado);
        }
        Empresa empresa = empresaExistente(empresaId);
        var conteo = em.createQuery("select count(g) from Guia g" + condicion, Long.class);
        var consulta = em.createQuery("select g" + ASIGNACIONES + condicion + " order by g.inicio desc, g.id desc", Guia.class);
        parametros.forEach(conteo::setParameter); parametros.forEach(consulta::setParameter);
        var resultado = new PaginaConsultaGuiasDTO();
        resultado.setPagina(numero); resultado.setTamano(cantidad); resultado.setTotal(conteo.getSingleResult());
        resultado.setRegistros(consulta.setFirstResult(numero * cantidad).setMaxResults(cantidad)
                .getResultList().stream().map(g -> aDTO(g, empresa)).toList());
        return resultado;
    }

    @Override
    public ConsultaGuiaDTO detalle(Long empresaId, Long guiaId) throws NegocioException {
        autorizar();
        validarId(empresaId, "La empresa"); validarId(guiaId, "La guia");
        Empresa empresa = empresaExistente(empresaId);
        // El predicado por empresa impide entregar una guia ajena al contexto seleccionado.
        Guia guia = em.createQuery("select g" + ASIGNACIONES + " where g.empresa.id = :empresaId and g.id = :guiaId", Guia.class)
                .setParameter("empresaId", empresaId).setParameter("guiaId", guiaId).setMaxResults(1)
                .getResultList().stream().findFirst().orElse(null);
        if (guia == null) throw new NegocioException("La guia no existe en la empresa seleccionada.");
        return aDTO(guia, empresa);
    }

    private void autorizar() {
        if (contexto == null || contexto.getCallerPrincipal() == null
                || contexto.getCallerPrincipal().getName() == null || contexto.getCallerPrincipal().getName().isBlank()
                || !(contexto.isCallerInRole(Roles.FUNCIONARIO) || contexto.isCallerInRole(Roles.ADMINISTRADOR)))
            throw new EJBAccessException("No tienes permiso para consultar guias.");
    }

    private static void validarId(Long id, String campo) throws NegocioException {
        if (id == null || id <= 0) throw new NegocioException(campo + " debe ser un identificador positivo.");
    }

    private Empresa empresaExistente(Long empresaId) throws NegocioException {
        Empresa empresa = em.find(Empresa.class, empresaId);
        if (empresa == null) throw new NegocioException("La empresa no existe.");
        return empresa;
    }

    private static Instant instante(String valor) throws NegocioException {
        if (valor == null || valor.isBlank()) throw new NegocioException("El periodo requiere desde y hasta con zona horaria.");
        try { return OffsetDateTime.parse(valor).toInstant(); }
        catch (DateTimeParseException e) { throw new NegocioException("Desde y hasta deben ser instantes ISO-8601 con zona horaria."); }
    }

    private static ConsultaGuiaDTO aDTO(Guia guia, Empresa empresa) {
        var dto = new ConsultaGuiaDTO();
        dto.setGuia(guia.aDTO()); dto.setNombreEmpresa(empresa.getNombre());
        dto.setMatricula(guia.getVehiculo() == null ? null : guia.getVehiculo().getMatricula());
        dto.setNombreChofer(guia.getChofer() == null ? null : guia.getChofer().getNombre());
        return dto;
    }
}
