package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.Stateless;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.VehiculoServicioLocal;
import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
@RolesAllowed({Roles.ADMINISTRADOR, Roles.CIUDADANO, Roles.CHOFER})
public class VehiculoServicioBean implements VehiculoServicioLocal {
    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @Override public VehiculoDTO agregar(VehiculoDTO dto) throws NegocioException {
        validar(dto);
        if (existeMatricula(dto.getMatricula(), null)) throw new NegocioException("Ya existe un vehiculo con esa matricula.");
        Vehiculo vehiculo = new Vehiculo(); aplicar(vehiculo, dto); em.persist(vehiculo); em.flush(); return aDTO(vehiculo);
    }

    @Override public VehiculoDTO modificar(Long id, VehiculoDTO dto) throws NegocioException {
        validar(dto);
        Vehiculo vehiculo = em.find(Vehiculo.class, id);
        if (vehiculo == null) throw new NegocioException("El vehiculo no existe.");
        if (existeMatricula(dto.getMatricula(), id)) throw new NegocioException("Ya existe un vehiculo con esa matricula.");
        aplicar(vehiculo, dto); return aDTO(vehiculo);
    }

    @Override public void darDeBaja(Long id) throws NegocioException {
        Vehiculo vehiculo = em.find(Vehiculo.class, id);
        if (vehiculo == null) throw new NegocioException("El vehiculo no existe.");
        vehiculo.setHabilitado(false);
    }

    @Override public List<VehiculoDTO> listar(Long empresaId) {
        String jpql = "select v from Vehiculo v" + (empresaId == null ? "" : " where v.empresa.id = :empresaId") + " order by v.matricula";
        var query = em.createQuery(jpql, Vehiculo.class);
        if (empresaId != null) query.setParameter("empresaId", empresaId);
        return query.getResultList().stream().map(this::aDTO).toList();
    }

    private void validar(VehiculoDTO dto) throws NegocioException {
        if (dto == null || dto.getEmpresaId() == null) throw new NegocioException("La empresa del vehiculo es obligatoria.");
        if (dto.getMatricula() == null || dto.getMatricula().isBlank()) throw new NegocioException("La matricula es obligatoria.");
        if (dto.getPesoKg() == null || dto.getPesoKg() < 0 || dto.getCapacidadCargaKg() == null || dto.getCapacidadCargaKg() <= 0)
            throw new NegocioException("El peso y la capacidad de carga deben ser validos.");
        if (dto.getCapacidadVolumenM3() == null || dto.getCapacidadVolumenM3().signum() <= 0)
            throw new NegocioException("La capacidad volumetrica debe ser positiva.");
        LocalDate desde = fecha(dto.getPermisoDesde(), "permiso desde"); LocalDate hasta = fecha(dto.getPermisoHasta(), "permiso hasta");
        fecha(dto.getItvVencimiento(), "vencimiento de ITV");
        if (dto.getNumeroPermiso() == null || dto.getNumeroPermiso().isBlank() || desde == null || hasta == null)
            throw new NegocioException("El numero y el periodo de vigencia del permiso son obligatorios.");
        if (desde.isAfter(hasta)) throw new NegocioException("El inicio del permiso no puede ser posterior a su vencimiento.");
        Empresa empresa = em.find(Empresa.class, dto.getEmpresaId());
        if (empresa == null || !empresa.isActiva()) throw new NegocioException("La empresa seleccionada no existe o esta inactiva.");
    }
    private boolean existeMatricula(String matricula, Long id) {
        String jpql = "select count(v) from Vehiculo v where upper(v.matricula) = :matricula" + (id == null ? "" : " and v.id <> :id");
        var query = em.createQuery(jpql, Long.class).setParameter("matricula", matricula.trim().toUpperCase()); if (id != null) query.setParameter("id", id);
        return query.getSingleResult() > 0;
    }
    private void aplicar(Vehiculo v, VehiculoDTO dto) throws NegocioException {
        v.setEmpresa(em.getReference(Empresa.class, dto.getEmpresaId())); v.setMatricula(dto.getMatricula().trim().toUpperCase()); v.setMarca(dto.getMarca()); v.setModelo(dto.getModelo());
        v.setPesoKg(dto.getPesoKg()); v.setCapacidadCargaKg(dto.getCapacidadCargaKg()); v.setCapacidadVolumenM3(dto.getCapacidadVolumenM3()); v.setNumeroPermiso(dto.getNumeroPermiso().trim());
        v.setPermisoDesde(fecha(dto.getPermisoDesde(), "permiso desde")); v.setPermisoHasta(fecha(dto.getPermisoHasta(), "permiso hasta")); v.setItvVencimiento(fecha(dto.getItvVencimiento(), "vencimiento de ITV")); v.setHabilitado(dto.isHabilitado());
    }
    private LocalDate fecha(String valor, String campo) throws NegocioException { try { return valor == null || valor.isBlank() ? null : LocalDate.parse(valor); } catch (DateTimeParseException e) { throw new NegocioException("El " + campo + " debe tener formato AAAA-MM-DD."); } }
    private VehiculoDTO aDTO(Vehiculo v) { VehiculoDTO dto = new VehiculoDTO(); dto.setId(v.getId()); dto.setEmpresaId(v.getEmpresa().getId()); dto.setMatricula(v.getMatricula()); dto.setMarca(v.getMarca()); dto.setModelo(v.getModelo()); dto.setPesoKg(v.getPesoKg()); dto.setCapacidadCargaKg(v.getCapacidadCargaKg()); dto.setCapacidadVolumenM3(v.getCapacidadVolumenM3()); dto.setNumeroPermiso(v.getNumeroPermiso()); dto.setPermisoDesde(String.valueOf(v.getPermisoDesde())); dto.setPermisoHasta(String.valueOf(v.getPermisoHasta())); dto.setItvVencimiento(String.valueOf(v.getItvVencimiento())); dto.setHabilitado(v.isHabilitado()); return dto; }
}
