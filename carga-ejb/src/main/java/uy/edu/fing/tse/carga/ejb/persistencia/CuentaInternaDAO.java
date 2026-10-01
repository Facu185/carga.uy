package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.CuentaInterna;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;

import java.util.List;
import java.util.Optional;

@Stateless
public class CuentaInternaDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public Optional<CuentaInterna> buscarPorUsuario(String nombreUsuario) {
        return em.createQuery("select c from CuentaInterna c where c.nombreUsuario = :u", CuentaInterna.class)
                .setParameter("u", nombreUsuario)
                .getResultStream()
                .findFirst();
    }

    public Optional<CuentaInterna> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(CuentaInterna.class, id));
    }

    public List<CuentaInterna> listar() {
        return em.createQuery("select c from CuentaInterna c order by c.nombreUsuario", CuentaInterna.class)
                .getResultList();
    }

    public long contarAdministradoresActivos() {
        return em.createQuery("select count(c) from CuentaInterna c where c.rol = :rol and c.activo = true", Long.class)
                .setParameter("rol", Rol.ADMINISTRADOR)
                .getSingleResult();
    }

    public long contar() {
        return em.createQuery("select count(c) from CuentaInterna c", Long.class).getSingleResult();
    }

    public CuentaInterna guardar(CuentaInterna cuenta) {
        em.persist(cuenta);
        return cuenta;
    }
}
