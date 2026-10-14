package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.NodoPeriferico;
import uy.edu.fing.tse.carga.ejb.modelo.TipoNodo;

import java.util.List;
import java.util.Optional;

/** Acceso a los nodos perifericos registrados (frente P4). */
@Stateless
public class NodoPerifericoDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public List<NodoPeriferico> listar() {
        return em.createQuery("select n from NodoPeriferico n order by n.tipo, n.nombre", NodoPeriferico.class)
                .getResultList();
    }

    public Optional<NodoPeriferico> buscar(Long id) {
        return Optional.ofNullable(id == null ? null : em.find(NodoPeriferico.class, id));
    }

    public Optional<NodoPeriferico> buscarActivo(TipoNodo tipo) {
        return em.createQuery("select n from NodoPeriferico n where n.tipo = :t and n.activo = true",
                        NodoPeriferico.class)
                .setParameter("t", tipo)
                .getResultStream()
                .findFirst();
    }

    public boolean hayDeTipo(TipoNodo tipo) {
        return em.createQuery("select count(n) from NodoPeriferico n where n.tipo = :t", Long.class)
                .setParameter("t", tipo)
                .getSingleResult() > 0;
    }

    /** Si ya hay otro nodo con ese nombre (sin distinguir mayusculas), sin contar el que se esta editando. */
    public boolean existeNombre(String nombre, Long excepto) {
        return em.createQuery("select count(n) from NodoPeriferico n where lower(n.nombre) = lower(:nombre)"
                        + " and (:excepto is null or n.id <> :excepto)", Long.class)
                .setParameter("nombre", nombre)
                .setParameter("excepto", excepto)
                .getSingleResult() > 0;
    }

    public NodoPeriferico guardar(NodoPeriferico nodo) {
        em.persist(nodo);
        em.flush();
        return nodo;
    }
}
