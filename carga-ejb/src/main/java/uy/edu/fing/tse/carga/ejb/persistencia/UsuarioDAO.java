package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;
import uy.edu.fing.tse.carga.ejb.modelo.RolCatalogo;
import uy.edu.fing.tse.carga.ejb.modelo.Usuario;

import java.util.Optional;

@Stateless
public class UsuarioDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public Optional<Usuario> buscarPorCedula(String cedula) {
        return em.createQuery("select u from Usuario u where u.cedula = :c", Usuario.class)
                .setParameter("c", cedula)
                .getResultStream()
                .findFirst();
    }

    public Usuario guardar(Usuario usuario) {
        em.persist(usuario);
        return usuario;
    }

    /** Los roles son un catalogo cargado en V2: si falta uno es un error de despliegue, no de negocio. */
    public RolCatalogo rol(Rol rol) {
        return em.createQuery("select r from RolCatalogo r where r.nombre = :n", RolCatalogo.class)
                .setParameter("n", rol.name())
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + rol + " en la tabla rol"));
    }
}
