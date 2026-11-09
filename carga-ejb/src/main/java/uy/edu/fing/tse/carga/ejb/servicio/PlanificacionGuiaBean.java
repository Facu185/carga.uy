package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.PlanificacionGuiaLocal;
import uy.edu.fing.tse.carga.api.dto.planificacion.*;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.integracion.ClienteDireccionesIde;
import uy.edu.fing.tse.carga.ejb.integracion.ClienteRutasOrs;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.TipoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.seguridad.CompatibilidadFuncionarioEmpresa;

import java.math.BigDecimal;
import java.util.List;

/** Consulta informativa: no retiene transaccion/locks durante llamadas a proveedores. */
@Stateless
@DeclareRoles(Roles.CIUDADANO)
@RolesAllowed(Roles.CIUDADANO)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PlanificacionGuiaBean implements PlanificacionGuiaLocal {
    @PersistenceContext(unitName = "cargaPU") EntityManager em;
    @Resource SessionContext contexto;
    @Inject CompatibilidadFuncionarioEmpresa compatibilidad;
    @Inject ClienteDireccionesIde ide;
    @Inject ClienteRutasOrs ors;

    @Override
    public List<SugerenciaCalleDTO> sugerir(String texto) throws NegocioException {
        responsable(); return ide.sugerir(texto);
    }

    @Override
    public DireccionMapaDTO resolver(SugerenciaCalleDTO seleccion) throws NegocioException {
        responsable(); return ide.resolver(seleccion);
    }

    @Override
    public RutaPreviaDTO calcular(SolicitudRutaDTO s) throws NegocioException {
        String cedula = responsable();
        if (s == null || s.getVehiculoId() == null || s.getVehiculoId() <= 0 || s.getCargaKg() == null
                || s.getCargaKg().signum() <= 0 || s.getCargaKg().compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0
                || !puntoValido(s.getOrigen()) || !puntoValido(s.getDestino()))
            throw new NegocioException("Seleccione vehiculo, carga positiva y coordenadas validas de origen y destino.");
        if (s.getOrigen().getLatitud().equals(s.getDestino().getLatitud())
                && s.getOrigen().getLongitud().equals(s.getDestino().getLongitud()))
            throw new NegocioException("El origen y el destino deben ser distintos.");
        Vehiculo vehiculo = em.createQuery("select v from Vehiculo v where v.id = :vehiculo and v.habilitado = true and exists "
                        + "(select x.id from VinculoEmpresaUsuario x where x.empresa = v.empresa and x.empresa.activa = true "
                        + "and x.usuario.cedula = :cedula and x.tipo = :tipo and x.estado = :estado)", Vehiculo.class)
                .setParameter("vehiculo", s.getVehiculoId()).setParameter("cedula", cedula)
                .setParameter("tipo", TipoVinculoEmpresa.RESPONSABLE).setParameter("estado", EstadoVinculoEmpresa.ACTIVO)
                .setMaxResults(1).getResultList().stream().findFirst().orElseThrow(PlanificacionGuiaBean::sinAcceso);
        if (vehiculo.getPesoKg() <= 0 || s.getCargaKg().compareTo(BigDecimal.valueOf(vehiculo.getCapacidadCargaKg())) > 0)
            throw new NegocioException("Revise el peso vacio del vehiculo y la capacidad de carga declarada.");
        BigDecimal pesoTotal = BigDecimal.valueOf(vehiculo.getPesoKg()).add(s.getCargaKg());
        return ors.calcular(s.getOrigen(), s.getDestino(), pesoTotal);
    }

    private String responsable() {
        var principal = contexto == null ? null : contexto.getCallerPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) throw sinAcceso();
        String cedula = principal.getName();
        compatibilidad.verificarEjercicioResponsabilidad(cedula);
        long vinculos = em.createQuery("select count(x) from VinculoEmpresaUsuario x where x.usuario.cedula = :cedula "
                        + "and x.tipo = :tipo and x.estado = :estado and x.empresa.activa = true", Long.class)
                .setParameter("cedula", cedula).setParameter("tipo", TipoVinculoEmpresa.RESPONSABLE)
                .setParameter("estado", EstadoVinculoEmpresa.ACTIVO).getSingleResult();
        if (vinculos == 0) throw sinAcceso();
        return cedula;
    }

    private static boolean puntoValido(PuntoMapaDTO punto) {
        return punto != null && punto.getLatitud() != null && punto.getLongitud() != null
                && Double.isFinite(punto.getLatitud()) && Double.isFinite(punto.getLongitud())
                && Math.abs(punto.getLatitud()) <= 90 && Math.abs(punto.getLongitud()) <= 180
                && (punto.getLatitud() != 0 || punto.getLongitud() != 0);
    }
    private static EJBAccessException sinAcceso() {
        return new EJBAccessException("No tiene acceso a la planificacion de esta empresa o vehiculo.");
    }
}
