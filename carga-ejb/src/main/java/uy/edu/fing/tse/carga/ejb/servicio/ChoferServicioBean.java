package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.ChoferServicioLocal;
import uy.edu.fing.tse.carga.api.dto.guia.ChoferDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;

import java.util.List;

@Stateless
@DeclareRoles({Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.FUNCIONARIO, Roles.CHOFER, Roles.CIUDADANO})
@RolesAllowed(Roles.ADMINISTRADOR)
public class ChoferServicioBean implements ChoferServicioLocal {
    @PersistenceContext(unitName = "cargaPU") private EntityManager em;
    @Override public ChoferDTO agregar(ChoferDTO dto) throws NegocioException { validar(dto); if (existeCedula(dto.getCedula(), null)) throw new NegocioException("Ya existe un chofer con esa cedula."); Chofer c = new Chofer(); aplicar(c, dto); em.persist(c); em.flush(); return aDTO(c); }
    @Override public ChoferDTO modificar(Long id, ChoferDTO dto) throws NegocioException { validar(dto); Chofer c = em.find(Chofer.class, id); if (c == null) throw new NegocioException("El chofer no existe."); if (existeCedula(dto.getCedula(), id)) throw new NegocioException("Ya existe un chofer con esa cedula."); aplicar(c, dto); return aDTO(c); }
    @Override public void darDeBaja(Long id) throws NegocioException { Chofer c = em.find(Chofer.class, id); if (c == null) throw new NegocioException("El chofer no existe."); c.setActivo(false); }
    @Override public void darDeAlta(Long id) throws NegocioException { Chofer c = em.find(Chofer.class, id); if (c == null) throw new NegocioException("El chofer no existe."); c.setActivo(true); }
    @Override public List<ChoferDTO> listar(Long empresaId) { String jpql = "select c from Chofer c" + (empresaId == null ? "" : " where c.empresa.id = :empresaId") + " order by c.nombre"; var q = em.createQuery(jpql, Chofer.class); if (empresaId != null) q.setParameter("empresaId", empresaId); return q.getResultList().stream().map(this::aDTO).toList(); }
    private void validar(ChoferDTO dto) throws NegocioException { if (dto == null || dto.getEmpresaId() == null) throw new NegocioException("La empresa del chofer es obligatoria."); if (dto.getCedula() == null || dto.getCedula().isBlank() || dto.getNombre() == null || dto.getNombre().isBlank()) throw new NegocioException("La cedula y el nombre del chofer son obligatorios."); Empresa e = em.find(Empresa.class, dto.getEmpresaId()); if (e == null || !e.isActiva()) throw new NegocioException("La empresa seleccionada no existe o esta inactiva."); }
    private boolean existeCedula(String cedula, Long id) { String jpql = "select count(c) from Chofer c where c.cedula = :cedula" + (id == null ? "" : " and c.id <> :id"); var q = em.createQuery(jpql, Long.class).setParameter("cedula", cedula.trim()); if (id != null) q.setParameter("id", id); return q.getSingleResult() > 0; }
    private void aplicar(Chofer c, ChoferDTO dto) { c.setEmpresa(em.getReference(Empresa.class, dto.getEmpresaId())); c.setCedula(dto.getCedula().trim()); c.setNombre(dto.getNombre().trim()); c.setActivo(dto.isActivo()); }
    private ChoferDTO aDTO(Chofer c) { ChoferDTO dto = new ChoferDTO(); dto.setId(c.getId()); dto.setEmpresaId(c.getEmpresa().getId()); dto.setCedula(c.getCedula()); dto.setNombre(c.getNombre()); dto.setActivo(c.isActivo()); return dto; }
}
