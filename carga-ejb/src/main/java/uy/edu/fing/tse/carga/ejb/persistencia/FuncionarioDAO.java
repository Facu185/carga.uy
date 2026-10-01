package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.Funcionario;

import java.util.List;
import java.util.Optional;

@Stateless
public class FuncionarioDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public Optional<Funcionario> buscarPorCedula(String cedula) {
        return em.createQuery("select f from Funcionario f where f.usuario.cedula = :c", Funcionario.class)
                .setParameter("c", cedula)
                .getResultStream()
                .findFirst();
    }

    public Optional<Funcionario> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(Funcionario.class, id));
    }

    public List<Funcionario> listar() {
        return em.createQuery("select f from Funcionario f order by f.usuario.nombre", Funcionario.class)
                .getResultList();
    }

    public Funcionario guardar(Funcionario funcionario) {
        em.persist(funcionario);
        return funcionario;
    }
}
