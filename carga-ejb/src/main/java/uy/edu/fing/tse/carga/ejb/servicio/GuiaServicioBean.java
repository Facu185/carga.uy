package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.GuiaServicioLocal;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.Roles;
import uy.edu.fing.tse.carga.api.dto.CrearGuiaCommand;
import uy.edu.fing.tse.carga.api.dto.GuiaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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
        LocalDate fecha = fecha(comando.getFecha());
        try {
            Guia.validarDatos(comando.getPesoKg(), comando.getVolumenM3(), fecha, comando.getOrigen(), comando.getDestino());
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
        if (!vehiculo.habilitadoEn(fecha))
            throw new NegocioException("El permiso nacional de circulacion o la ITV no estan vigentes para la fecha del viaje.");
        try {
            Guia.validarCapacidades(vehiculo, comando.getPesoKg(), comando.getVolumenM3());
        } catch (IllegalArgumentException e) {
            throw new NegocioException(e.getMessage());
        }
        if (hayConflicto(vehiculo.getId(), chofer.getId(), fecha))
            throw new NegocioException("El vehiculo o el chofer ya tiene una guia asignada para esa fecha.");

        Rubro rubro = em.find(Rubro.class, comando.getRubroId());
        if (rubro == null) throw new NegocioException("El rubro seleccionado no existe.");
        TipoCarga tipoCarga = comando.getTipoCargaId() == null ? null : em.find(TipoCarga.class, comando.getTipoCargaId());
        if (comando.getTipoCargaId() != null && tipoCarga == null) throw new NegocioException("El tipo de carga seleccionado no existe.");

        Guia guia = new Guia(empresa, vehiculo, chofer, rubro, tipoCarga, comando.getPesoKg(), comando.getVolumenM3(), fecha,
                comando.getOrigen(), comando.getOrigenLat(), comando.getOrigenLon(), comando.getDestino(),
                comando.getDestinoLat(), comando.getDestinoLon());
        em.persist(guia);
        em.flush();
        return guia.aDTO();
    }

    private Empresa empresaDelResponsable() throws NegocioException {
        if (contexto == null || contexto.getCallerPrincipal() == null) throw new NegocioException("No hay un responsable autenticado.");
        String cedula = contexto.getCallerPrincipal().getName();
        Usuario usuario = em.createQuery("select u from Usuario u join fetch u.empresa where u.cedula = :cedula", Usuario.class)
                .setParameter("cedula", cedula).getResultStream().findFirst().orElse(null);
        if (usuario == null || usuario.getEmpresa() == null)
            throw new NegocioException("El usuario autenticado no esta vinculado a una empresa.");
        return usuario.getEmpresa();
    }

    private boolean hayConflicto(Long vehiculoId, Long choferId, LocalDate fecha) {
        Long cantidad = em.createQuery("select count(g) from Guia g where g.fecha = :fecha and " +
                        "(g.vehiculo.id = :vehiculoId or g.chofer.id = :choferId) and g.estado <> 'FINALIZADA'", Long.class)
                .setParameter("fecha", fecha).setParameter("vehiculoId", vehiculoId).setParameter("choferId", choferId)
                .getSingleResult();
        return cantidad != null && cantidad > 0;
    }

    private LocalDate fecha(String valor) throws NegocioException {
        if (valor == null || valor.isBlank()) throw new NegocioException("La fecha del viaje es obligatoria.");
        try { return LocalDate.parse(valor); }
        catch (DateTimeParseException e) { throw new NegocioException("La fecha del viaje debe tener formato ISO (yyyy-MM-dd)."); }
    }
}
