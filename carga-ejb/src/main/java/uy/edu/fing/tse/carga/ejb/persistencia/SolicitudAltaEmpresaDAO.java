package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoSolicitudEmpresa;
import uy.edu.fing.tse.carga.ejb.modelo.SolicitudAltaEmpresa;
import java.util.List;
import java.util.Optional;

@Stateless
public class SolicitudAltaEmpresaDAO {
    private static final int MAXIMO_LISTADO = 200;
    @PersistenceContext(unitName = "cargaPU") EntityManager em;
    public SolicitudAltaEmpresa guardar(SolicitudAltaEmpresa solicitud) { em.persist(solicitud); return solicitud; }
    public Optional<SolicitudAltaEmpresa> buscarPorId(Long id) { return Optional.ofNullable(em.find(SolicitudAltaEmpresa.class, id)); }
    public Optional<SolicitudAltaEmpresa> pendientePorUsuario(String cedula) {
        return em.createQuery("select s from SolicitudAltaEmpresa s where s.solicitante.cedula = :cedula and s.estado = :estado", SolicitudAltaEmpresa.class)
                .setParameter("cedula", cedula).setParameter("estado", EstadoSolicitudEmpresa.PENDIENTE).setMaxResults(1).getResultStream().findFirst();
    }
    public Optional<SolicitudAltaEmpresa> ultimaPorUsuario(String cedula) {
        return em.createQuery("select s from SolicitudAltaEmpresa s where s.solicitante.cedula = :cedula order by s.creadaEn desc, s.id desc", SolicitudAltaEmpresa.class)
                .setParameter("cedula", cedula).setMaxResults(1).getResultStream().findFirst();
    }
    public List<SolicitudAltaEmpresa> listar(EstadoSolicitudEmpresa estado) {
        String jpql = estado == null ? "select s from SolicitudAltaEmpresa s order by s.creadaEn desc" : "select s from SolicitudAltaEmpresa s where s.estado = :estado order by s.creadaEn";
        var q = em.createQuery(jpql, SolicitudAltaEmpresa.class).setMaxResults(MAXIMO_LISTADO);
        if (estado != null) q.setParameter("estado", estado);
        return q.getResultList();
    }
}
