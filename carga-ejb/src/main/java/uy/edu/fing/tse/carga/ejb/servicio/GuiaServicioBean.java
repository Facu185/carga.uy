package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.GuiaServicioLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.api.dto.guia.CrearGuiaCommand;
import uy.edu.fing.tse.carga.api.dto.guia.GuiaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Caso de uso P2. El bloqueo pesimista de vehiculo y chofer serializa asignaciones
 * simultaneas para una misma fecha dentro de la transaccion EJB.
 */
@Stateless
@DeclareRoles({Roles.CIUDADANO, Roles.CHOFER, Roles.FUNCIONARIO, Roles.AUTORIDAD, Roles.ADMINISTRADOR})
@RolesAllowed(Roles.CIUDADANO)
public class GuiaServicioBean implements GuiaServicioLocal {
    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    @jakarta.annotation.Resource
    SessionContext contexto;

    @Override
    public GuiaDTO crear(CrearGuiaCommand comando) throws NegocioException {
        if (comando == null) throw new NegocioException("Los datos de la guia son obligatorios.");
        Instant inicio = instante(comando.getInicio(), "inicio");
        Instant fin = instante(comando.getFin(), "fin");
        try {
            Guia.validarDatos(comando.getPesoKg(), comando.getVolumenM3(), inicio, fin, comando.getOrigen(), comando.getDestino());
        } catch (java.lang.IllegalArgumentException e) {
            throw new NegocioException(e.getMessage());
        }
        if (comando.getVehiculoId() == null || comando.getChoferId() == null || comando.getRubroId() == null)
            throw new NegocioException("Vehiculo, chofer y rubro son obligatorios.");

        Empresa empresa = empresaDelResponsable();
        // Bloqueos antes de consultar conflictos: una sola asignacion concurrente puede avanzar.
        Vehiculo vehiculo = em.find(Vehiculo.class, comando.getVehiculoId(), LockModeType.PESSIMISTIC_WRITE);
        Chofer chofer = em.find(Chofer.class, comando.getChoferId(), LockModeType.PESSIMISTIC_WRITE);
        if (vehiculo == null || chofer == null) throw new NegocioException("El vehiculo o el chofer no existe.");
        if (!empresa.getId().equals(vehiculo.getEmpresa().getId()) || !empresa.getId().equals(chofer.getEmpresa().getId()))
            throw new NegocioException("El vehiculo y el chofer deben pertenecer a la empresa responsable.");
        // El intervalo es [inicio, fin): para verificar su ultimo instante se resta
        // exactamente un nanosegundo, que es la precision de Instant (no un milisegundo).
        if (!vehiculo.habilitadoEn(inicio.atZone(java.time.ZoneOffset.UTC).toLocalDate()) || !vehiculo.habilitadoEn(fin.minusNanos(1).atZone(java.time.ZoneOffset.UTC).toLocalDate()))
            throw new NegocioException("El permiso nacional de circulacion o la ITV no estan vigentes para la fecha del viaje.");
        try {
            Guia.validarCapacidades(vehiculo, comando.getPesoKg(), comando.getVolumenM3());
        } catch (IllegalArgumentException e) {
            throw new NegocioException(e.getMessage());
        }
        if (hayConflicto(vehiculo.getId(), chofer.getId(), inicio, fin))
            throw new NegocioException("El vehiculo o el chofer ya tiene una guia asignada en ese periodo.");

        Rubro rubro = em.find(Rubro.class, comando.getRubroId());
        if (rubro == null) throw new NegocioException("El rubro seleccionado no existe.");
        TipoCarga tipoCarga = comando.getTipoCargaId() == null ? null : em.find(TipoCarga.class, comando.getTipoCargaId());
        if (comando.getTipoCargaId() != null && tipoCarga == null) throw new NegocioException("El tipo de carga seleccionado no existe.");

        Guia guia = new Guia(empresa, vehiculo, chofer, rubro, tipoCarga, comando.getPesoKg(), comando.getVolumenM3(), inicio, fin,
                comando.getOrigen(), comando.getOrigenLat(), comando.getOrigenLon(), comando.getDestino(),
                comando.getDestinoLat(), comando.getDestinoLon());
        em.persist(guia);
        em.flush();
        return guia.aDTO();
    }

    @Override
    @RolesAllowed(Roles.CHOFER)
    public List<GuiaDTO> guiasDelChoferAutenticado() throws NegocioException {
        String cedula = principalAutenticado();
        Chofer chofer = em.createQuery("select c from Chofer c where c.cedula = :cedula", Chofer.class)
                .setParameter("cedula", cedula).getResultStream().findFirst().orElse(null);
        if (chofer == null) throw new NegocioException("El usuario autenticado no esta registrado como chofer.");
        return em.createQuery("select g from Guia g where g.chofer.id = :choferId order by g.inicio desc", Guia.class)
                .setParameter("choferId", chofer.getId()).getResultList().stream().map(Guia::aDTO).toList();
    }

    private Empresa empresaDelResponsable() throws NegocioException {
        String cedula = principalAutenticado();
        Usuario usuario = em.createQuery("select u from Usuario u join fetch u.empresa where u.cedula = :cedula", Usuario.class)
                .setParameter("cedula", cedula).getResultStream().findFirst().orElse(null);
        if (usuario == null || usuario.getEmpresa() == null)
            throw new NegocioException("El usuario autenticado no esta vinculado a una empresa.");
        return usuario.getEmpresa();
    }

    private boolean hayConflicto(Long vehiculoId, Long choferId, Instant inicio, Instant fin) {
        Long cantidad = em.createQuery("select count(g) from Guia g where g.inicio < :fin and g.fin > :inicio and " +
                        "(g.vehiculo.id = :vehiculoId or g.chofer.id = :choferId) and g.estado <> 'FINALIZADA'", Long.class)
                .setParameter("inicio", inicio).setParameter("fin", fin).setParameter("vehiculoId", vehiculoId).setParameter("choferId", choferId)
                .getSingleResult();
        return cantidad != null && cantidad > 0;
    }

    private String principalAutenticado() throws NegocioException {
        if (contexto == null || contexto.getCallerPrincipal() == null || contexto.getCallerPrincipal().getName() == null || contexto.getCallerPrincipal().getName().isBlank())
            throw new NegocioException("No hay un usuario autenticado.");
        return contexto.getCallerPrincipal().getName();
    }
    private Instant instante(String valor, String campo) throws NegocioException {
        if (valor == null || valor.isBlank()) throw new NegocioException("El " + campo + " del viaje es obligatorio.");
        try { return Instant.parse(valor); }
        catch (DateTimeParseException e) { throw new NegocioException("El " + campo + " debe tener formato ISO-8601 con zona horaria."); }
    }
}
