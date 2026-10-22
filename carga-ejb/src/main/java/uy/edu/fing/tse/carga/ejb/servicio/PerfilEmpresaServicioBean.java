package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import uy.edu.fing.tse.carga.api.contract.service.PerfilEmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.EditarPerfilEmpresaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Empresa;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;

import java.security.Principal;
import java.sql.SQLException;

/** Perfil del responsable, sin ampliar las operaciones ni los roles del ABM administrativo. */
@Stateless
@DeclareRoles({Roles.CIUDADANO, Roles.CHOFER})
@RolesAllowed({Roles.CIUDADANO, Roles.CHOFER})
public class PerfilEmpresaServicioBean implements PerfilEmpresaServicioLocal {
    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    @Resource
    private SessionContext contexto;

    @EJB
    private VinculoEmpresaUsuarioDAO vinculos;

    @Override
    public EmpresaDTO consultar(Long id) throws NegocioException {
        return empresaAutorizada(id, null).aDTO();
    }

    @Override
    public EmpresaDTO modificar(Long id, EditarPerfilEmpresaDTO datos) throws NegocioException {
        Empresa empresa = empresaAutorizada(id, LockModeType.PESSIMISTIC_WRITE);
        validar(datos);
        Long duplicados = em.createQuery("select count(e) from Empresa e where e.numeroEmpresa = :numero and e.id <> :id", Long.class)
                .setParameter("numero", datos.getNumeroEmpresa()).setParameter("id", id).getSingleResult();
        if (duplicados > 0) throw numeroDuplicado();

        empresa.setNombre(datos.getNombre().trim());
        empresa.setRazonSocial(datos.getRazonSocial());
        empresa.setNumeroEmpresa(datos.getNumeroEmpresa());
        empresa.setDireccion(datos.getDireccion());
        // No merge ni campos administrativos; incluso una fechaAlta heredada nula se conserva.
        try {
            em.flush();
        } catch (PersistenceException e) {
            // La consulta previa no evita una carrera entre empresas por el mismo numero.
            for (Throwable causa = e; causa != null; causa = causa.getCause()) {
                if (causa instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                    throw numeroDuplicado();
                }
            }
            throw e;
        }
        return empresa.aDTO();
    }

    private Empresa empresaAutorizada(Long id, LockModeType bloqueo) throws NegocioException {
        if (id == null || id <= 0) throw new NegocioException("La empresa debe tener un identificador positivo.");
        Principal principal = contexto == null ? null : contexto.getCallerPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()
                || !vinculos.esResponsableActivo(principal.getName(), id)) throw sinAcceso();
        Empresa empresa = bloqueo == null ? em.find(Empresa.class, id) : em.find(Empresa.class, id, bloqueo);
        if (empresa == null || !empresa.isActiva()) throw sinAcceso();
        return empresa;
    }

    private static void validar(EditarPerfilEmpresaDTO datos) throws NegocioException {
        if (datos == null || datos.getNombre() == null || datos.getNombre().isBlank())
            throw new NegocioException("El nombre de la empresa es obligatorio.");
        if (datos.getNumeroEmpresa() <= 0) throw new NegocioException("El numero de empresa debe ser positivo.");
        if (datos.getNombre().trim().length() > 200
                || (datos.getRazonSocial() != null && datos.getRazonSocial().length() > 200)
                || (datos.getDireccion() != null && datos.getDireccion().length() > 300))
            throw new NegocioException("El nombre y la razon social admiten 200 caracteres y la direccion 300.");
    }

    private static EJBAccessException sinAcceso() {
        return new EJBAccessException("No tiene acceso al perfil de esta empresa.");
    }

    private static NegocioException numeroDuplicado() {
        return new NegocioException("Ya existe una empresa con ese numero.");
    }
}
