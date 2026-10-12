package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.TipoVinculoEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.VinculoEmpresaUsuario;
import java.util.List;
import java.util.Optional;

@Stateless
public class VinculoEmpresaUsuarioDAO {
    private static final int MAXIMO_LISTADO = 200;
    @PersistenceContext(unitName = "cargaPU") EntityManager em;
    public VinculoEmpresaUsuario guardar(VinculoEmpresaUsuario vinculo) { em.persist(vinculo); return vinculo; }
    public Optional<VinculoEmpresaUsuario> buscarPorId(Long id) { return Optional.ofNullable(em.find(VinculoEmpresaUsuario.class, id)); }
    public Optional<VinculoEmpresaUsuario> pendiente(Long usuarioId, Long empresaId) {
        return em.createQuery("select v from VinculoEmpresaUsuario v where v.usuario.id = :usuario and v.empresa.id = :empresa and v.estado = :estado", VinculoEmpresaUsuario.class)
                .setParameter("usuario", usuarioId).setParameter("empresa", empresaId).setParameter("estado", EstadoVinculoEmpresa.PENDIENTE).setMaxResults(1).getResultStream().findFirst();
    }
    public List<VinculoEmpresaUsuario> deUsuario(String cedula) {
        return em.createQuery("select v from VinculoEmpresaUsuario v join fetch v.empresa where v.usuario.cedula = :cedula order by v.solicitadoEn desc", VinculoEmpresaUsuario.class)
                .setParameter("cedula", cedula).setMaxResults(MAXIMO_LISTADO).getResultList();
    }
    public List<VinculoEmpresaUsuario> activosDeEmpresa(Long empresaId) {
        return em.createQuery("select v from VinculoEmpresaUsuario v where v.empresa.id = :empresa and v.estado = :estado", VinculoEmpresaUsuario.class)
                .setParameter("empresa", empresaId).setParameter("estado", EstadoVinculoEmpresa.ACTIVO).getResultList();
    }
    /** La autorización de operaciones empresariales se resuelve en servidor, no desde un empresaId del cliente. */
    public boolean esResponsableActivo(String cedula, Long empresaId) {
        return em.createQuery("select count(v) from VinculoEmpresaUsuario v where v.usuario.cedula = :cedula and v.empresa.id = :empresa and v.tipo = :tipo and v.estado = :estado", Long.class)
                .setParameter("cedula", cedula).setParameter("empresa", empresaId)
                .setParameter("tipo", TipoVinculoEmpresa.RESPONSABLE).setParameter("estado", EstadoVinculoEmpresa.ACTIVO)
                .getSingleResult() > 0;
    }
    public List<VinculoEmpresaUsuario> listar(EstadoVinculoEmpresa estado) {
        String jpql = estado == null ? "select v from VinculoEmpresaUsuario v join fetch v.empresa order by v.solicitadoEn desc"
                : "select v from VinculoEmpresaUsuario v join fetch v.empresa where v.estado = :estado order by v.solicitadoEn";
        var consulta = em.createQuery(jpql, VinculoEmpresaUsuario.class).setMaxResults(MAXIMO_LISTADO);
        if (estado != null) consulta.setParameter("estado", estado);
        return consulta.getResultList();
    }
}
