package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioRemote;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;

import java.time.LocalDate;
import java.util.List;

/**
 * Capa de negocio. Ejemplo de referencia para el resto de los servicios:
 * valida las reglas y delega la persistencia en JPA.
 */
@Stateless
public class EmpresaServicioBean implements EmpresaServicioLocal, EmpresaServicioRemote {

    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @Override
    public EmpresaDTO agregar(EmpresaDTO dto) throws NegocioException {
        validar(dto);
        if (existeNumero(dto.getNumeroEmpresa())) {
            throw new NegocioException("Ya existe una empresa con numero " + dto.getNumeroEmpresa() + ".");
        }
        if (dto.getFechaAlta() == null) {
            dto.setFechaAlta(LocalDate.now());
        }
        if (dto.getFechaAlta().isAfter(LocalDate.now())) {
            throw new NegocioException("La fecha de alta no puede ser futura.");
        }
        Empresa empresa = Empresa.desdeDTO(dto);
        em.persist(empresa);
        em.flush();
        return empresa.aDTO();
    }

    @Override
    public EmpresaDTO modificar(Long id, EmpresaDTO dto) throws NegocioException {
        if (id == null) throw new NegocioException("La empresa es obligatoria.");
        validar(dto);
        Empresa empresa = em.find(Empresa.class, id);
        if (empresa == null) throw new NegocioException("La empresa no existe.");
        if (empresa.getNumeroEmpresa() != dto.getNumeroEmpresa() && existeNumero(dto.getNumeroEmpresa()))
            throw new NegocioException("Ya existe una empresa con numero " + dto.getNumeroEmpresa() + ".");
        empresa.setNombre(dto.getNombre().trim());
        empresa.setRazonSocial(dto.getRazonSocial());
        empresa.setNumeroEmpresa(dto.getNumeroEmpresa());
        empresa.setFechaAlta(dto.getFechaAlta());
        return empresa.aDTO();
    }

    @Override
    public void darDeBaja(Long id) throws NegocioException {
        Empresa empresa = em.find(Empresa.class, id);
        if (empresa == null) throw new NegocioException("La empresa no existe.");
        empresa.setActiva(false);
    }

    @Override
    public List<EmpresaDTO> listar() {
        return em.createQuery("select e from Empresa e order by e.id", Empresa.class)
                .getResultList().stream().map(Empresa::aDTO).toList();
    }

    @Override
    public List<EmpresaDTO> buscarPorNombre(String nombre) {
        return em.createQuery(
                        "select e from Empresa e where lower(e.nombre) like lower(concat('%', :n, '%')) order by e.id",
                        Empresa.class)
                .setParameter("n", nombre == null ? "" : nombre)
                .getResultList().stream().map(Empresa::aDTO).toList();
    }

    private boolean existeNumero(long numeroEmpresa) {
        Long cantidad = em.createQuery(
                        "select count(e) from Empresa e where e.numeroEmpresa = :num", Long.class)
                .setParameter("num", numeroEmpresa)
                .getSingleResult();
        return cantidad != null && cantidad > 0;
    }

    private void validar(EmpresaDTO dto) throws NegocioException {
        if (dto == null || dto.getNombre() == null || dto.getNombre().isBlank())
            throw new NegocioException("El nombre de la empresa es obligatorio.");
        if (dto.getNumeroEmpresa() <= 0)
            throw new NegocioException("El numero de empresa debe ser positivo.");
        if (dto.getFechaAlta() == null) dto.setFechaAlta(LocalDate.now());
        if (dto.getFechaAlta().isAfter(LocalDate.now()))
            throw new NegocioException("La fecha de alta no puede ser futura.");
    }
}
