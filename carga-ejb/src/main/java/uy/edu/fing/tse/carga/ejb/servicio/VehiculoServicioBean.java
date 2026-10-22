package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.Stateless;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.VehiculoServicioLocal;
import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.CompatibilidadFuncionarioEmpresa;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
@RolesAllowed({Roles.ADMINISTRADOR, Roles.CIUDADANO, Roles.CHOFER})
public class VehiculoServicioBean implements VehiculoServicioLocal {
    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @Resource
    private SessionContext contexto;

    @jakarta.inject.Inject
    private VinculoEmpresaUsuarioDAO vinculos;

    @jakarta.inject.Inject
    private CompatibilidadFuncionarioEmpresa compatibilidad;

    @Override public VehiculoDTO agregar(VehiculoDTO dto) throws NegocioException {
        validarEmpresaInformada(dto);
        autorizarEmpresa(dto.getEmpresaId());
        validar(dto);
        if (existeMatricula(dto.getMatricula(), null)) throw new NegocioException("Ya existe un vehiculo con esa matricula.");
        Vehiculo vehiculo = new Vehiculo(); aplicar(vehiculo, dto); em.persist(vehiculo); em.flush(); return aDTO(vehiculo);
    }

    @Override public VehiculoDTO modificar(Long id, VehiculoDTO dto) throws NegocioException {
        validarEmpresaInformada(dto);
        Vehiculo vehiculo = buscarParaModificar(id);
        autorizarEmpresa(vehiculo.getEmpresa().getId());
        autorizarEmpresa(dto.getEmpresaId());
        // El PUT tambien puede dar de baja: no debe eludir la regla del DELETE.
        if (!dto.isHabilitado()) validarSinGuiaEnCurso(id);
        validar(dto);
        if (existeMatricula(dto.getMatricula(), id)) throw new NegocioException("Ya existe un vehiculo con esa matricula.");
        aplicar(vehiculo, dto); return aDTO(vehiculo);
    }

    @Override public void darDeBaja(Long id) throws NegocioException {
        Vehiculo vehiculo = buscarParaModificar(id);
        autorizarEmpresa(vehiculo.getEmpresa().getId());
        validarSinGuiaEnCurso(id);
        vehiculo.setHabilitado(false);
    }

    @Override public void reactivar(Long id) throws NegocioException {
        Vehiculo vehiculo = buscarParaModificar(id);
        autorizarEmpresa(vehiculo.getEmpresa().getId());
        vehiculo.setHabilitado(true);
    }

    private Vehiculo buscarParaModificar(Long id) throws NegocioException {
        // Comparte exclusion con la asignacion de guias y las otras mutaciones del ABM.
        Vehiculo vehiculo = em.find(Vehiculo.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (vehiculo == null) throw new NegocioException("El vehiculo no existe.");
        return vehiculo;
    }

    private void validarSinGuiaEnCurso(Long vehiculoId) throws NegocioException {
        long cantidad = em.createQuery("select count(g) from Guia g where g.vehiculo.id = :vehiculoId and g.estado = :estado", Long.class)
                .setParameter("vehiculoId", vehiculoId).setParameter("estado", "EN_CURSO").getSingleResult();
        if (cantidad > 0) throw new NegocioException("No se puede dar de baja un vehiculo con una guia en curso.");
    }

    @Override public List<VehiculoDTO> listar(Long empresaId) {
        autorizarEmpresa(empresaId);
        String jpql = "select v from Vehiculo v" + (empresaId == null ? "" : " where v.empresa.id = :empresaId") + " order by v.matricula";
        var query = em.createQuery(jpql, Vehiculo.class);
        if (empresaId != null) query.setParameter("empresaId", empresaId);
        return query.getResultList().stream().map(this::aDTO).toList();
    }

    private boolean esAdministrador() {
        return contexto != null && contexto.isCallerInRole(Roles.ADMINISTRADOR);
    }

    private void autorizarEmpresa(Long empresaId) {
        if (esAdministrador()) return;
        Principal principal = contexto == null ? null : contexto.getCallerPrincipal();
        if (empresaId == null || principal == null || principal.getName() == null || principal.getName().isBlank()
                || !vinculos.esResponsableActivo(principal.getName(), empresaId)) {
            throw new EJBAccessException("No tienes permiso para gestionar los vehiculos de esta empresa.");
        }
        compatibilidad.verificarEjercicioResponsabilidad(principal.getName());
    }

    private void validarEmpresaInformada(VehiculoDTO dto) throws NegocioException {
        if (dto == null || dto.getEmpresaId() == null) throw new NegocioException("La empresa del vehiculo es obligatoria.");
    }

    private void validar(VehiculoDTO dto) throws NegocioException {
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
    private VehiculoDTO aDTO(Vehiculo v) { VehiculoDTO dto = new VehiculoDTO(); dto.setId(v.getId()); dto.setEmpresaId(v.getEmpresa().getId()); dto.setMatricula(v.getMatricula()); dto.setMarca(v.getMarca()); dto.setModelo(v.getModelo()); dto.setPesoKg(v.getPesoKg()); dto.setCapacidadCargaKg(v.getCapacidadCargaKg()); dto.setCapacidadVolumenM3(v.getCapacidadVolumenM3()); dto.setNumeroPermiso(v.getNumeroPermiso()); dto.setPermisoDesde(texto(v.getPermisoDesde())); dto.setPermisoHasta(texto(v.getPermisoHasta())); dto.setItvVencimiento(texto(v.getItvVencimiento())); dto.setHabilitado(v.isHabilitado()); return dto; }
    private static String texto(LocalDate fecha) { return fecha == null ? null : fecha.toString(); }
}
