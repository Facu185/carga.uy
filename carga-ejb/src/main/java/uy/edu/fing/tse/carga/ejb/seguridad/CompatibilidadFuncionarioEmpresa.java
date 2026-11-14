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

/** Politica interna P1/P2 (ADR-0015/0025), dentro de la transaccion del servicio. */
@Dependent
public class CompatibilidadFuncionarioEmpresa {
    private static final int ESPACIO_CHOFER_RESPONSABLE = 0x43485253;
    @PersistenceContext(unitName = "cargaPU")
    private EntityManager em;

    public void verificarPuedeSerResponsable(Usuario persona) throws NegocioException {
        bloquearUsuario(persona);
        bloquearIdentidadChofer(persona.getCedula());
        if (esFuncionarioActivo(persona.getCedula())) {
            throw new NegocioException("Un funcionario activo no puede asumir responsabilidad empresarial.");
        }
        if (esChoferActivo(persona.getCedula())) {
            throw new NegocioException("Un chofer activo no puede asumir responsabilidad empresarial.");
        }
    }

    public void verificarPuedeSerChofer(String cedula) throws NegocioException {
        bloquearIdentidadChofer(cedula);
        if (tieneResponsabilidadActiva(cedula)) {
            throw new NegocioException("Debe resolver su responsabilidad empresarial activa antes de habilitarse como chofer.");
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
        if (esChoferActivo(cedula) && tieneResponsabilidadActiva(cedula)) {
            throw new EJBAccessException("No puede operar como responsable mientras sea chofer activo.");
        }
    }

    /** Ambos sentidos bloquean la misma identidad antes de leer el estado opuesto. */
    public void bloquearUsuario(Usuario persona) {
        em.lock(persona, LockModeType.PESSIMISTIC_WRITE);
    }

    /** Protege tambien cedulas sin Usuario previo; una colision solo agrega espera. */
    protected void bloquearIdentidadChofer(String cedula) {
        em.createNativeQuery("select count(*) from pg_advisory_xact_lock(?1, ?2)")
                .setParameter(1, ESPACIO_CHOFER_RESPONSABLE)
                .setParameter(2, cedula.hashCode()).getSingleResult();
    }

    protected boolean esChoferActivo(String cedula) {
        return em.createQuery("select count(c) from Chofer c where c.cedula = :cedula and c.activo = true", Long.class)
                .setParameter("cedula", cedula).getSingleResult() > 0;
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
