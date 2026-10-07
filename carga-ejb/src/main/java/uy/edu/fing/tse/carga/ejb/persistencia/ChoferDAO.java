package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;

import java.util.Optional;

/** Acceso puntual a la vinculacion cedula--chofer usada al iniciar sesion. */
@Stateless
public class ChoferDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public Optional<Chofer> buscarPorCedula(String cedula) {
        return em.createQuery("select c from Chofer c where c.cedula = :cedula and c.activo = true", Chofer.class)
                .setParameter("cedula", cedula)
                .getResultStream()
                .findFirst();
    }
}
