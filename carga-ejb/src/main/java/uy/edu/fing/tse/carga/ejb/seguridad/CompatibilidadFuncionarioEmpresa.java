package uy.edu.fing.tse.carga.ejb.seguridad;

import jakarta.ejb.EJBAccessException;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.TipoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;

/** Politica interna P1/P2 (ADR-0015), ejecutada dentro de la transaccion del servicio. */
@Dependent
public class CompatibilidadFuncionarioEmpresa {
    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    public void verificarPuedeSerResponsable(Usuario persona) throws NegocioException {
        bloquearUsuario(persona);
        if (esFuncionarioActivo(persona.getCedula())) {
            throw new NegocioException("Un funcionario activo no puede asumir responsabilidad empresarial.");
        }
    }

    public void verificarPuedeSerFuncionario(Usuario persona) throws NegocioException {
        bloquearUsuario(persona);
        if (tieneResponsabilidadActiva(persona.getCedula())) {
            throw new NegocioException("Debe resolver su responsabilidad empresarial activa antes de habilitar el acceso como funcionario.");
        }
    }

    /** No revoca datos heredados ni retira permisos oficiales; solo bloquea el uso como responsable. */
    public void verificarEjercicioResponsabilidad(String cedula) {
        if (esFuncionarioActivo(cedula) && tieneResponsabilidadActiva(cedula)) {
            throw new EJBAccessException("No puede operar como responsable mientras sea funcionario activo.");
        }
    }

    /** Ambos sentidos bloquean la misma identidad antes de leer el estado opuesto. */
    public void bloquearUsuario(Usuario persona) {
        em.lock(persona, LockModeType.PESSIMISTIC_WRITE);
    }

    protected boolean esFuncionarioActivo(String cedula) {
        return em.createQuery("select count(f) from Funcionario f where f.usuario.cedula = :cedula and f.activo = true", Long.class)
                .setParameter("cedula", cedula).getSingleResult() > 0;
    }

    protected boolean tieneResponsabilidadActiva(String cedula) {
        // Un count no pierde responsabilidades por el limite de 200 del listado de vinculos.
        return em.createQuery("select count(v) from VinculoEmpresaUsuario v where v.usuario.cedula = :cedula "
                        + "and v.tipo = :tipo and v.estado = :estado", Long.class)
                .setParameter("cedula", cedula).setParameter("tipo", TipoVinculoEmpresa.RESPONSABLE)
                .setParameter("estado", EstadoVinculoEmpresa.ACTIVO).getSingleResult() > 0;
    }
}
