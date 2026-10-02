package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoSolicitud;
import uy.edu.fing.tse.carga.ejb.modelo.SolicitudFuncionario;

import java.util.List;
import java.util.Optional;

@Stateless
public class SolicitudFuncionarioDAO {

    private static final int MAXIMO_LISTADO = 200;

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public SolicitudFuncionario guardar(SolicitudFuncionario solicitud) {
        em.persist(solicitud);
        return solicitud;
    }

    public Optional<SolicitudFuncionario> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(SolicitudFuncionario.class, id));
    }

    public Optional<SolicitudFuncionario> ultimaPorCedula(String cedula) {
        return em.createQuery("select s from SolicitudFuncionario s where s.solicitante.cedula = :c "
                        + "order by s.creadaEn desc, s.id desc", SolicitudFuncionario.class)
                .setParameter("c", cedula)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    /** estado null = todas, de la mas nueva a la mas vieja; con estado, por orden de llegada. */
    public List<SolicitudFuncionario> listar(EstadoSolicitud estado) {
        if (estado == null) {
            return em.createQuery("select s from SolicitudFuncionario s order by s.creadaEn desc",
                    SolicitudFuncionario.class).setMaxResults(MAXIMO_LISTADO).getResultList();
        }
        return em.createQuery("select s from SolicitudFuncionario s where s.estado = :e order by s.creadaEn",
                        SolicitudFuncionario.class)
                .setParameter("e", estado)
                .setMaxResults(MAXIMO_LISTADO)
                .getResultList();
    }
}
