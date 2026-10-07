package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import uy.edu.fing.tse.carga.ejb.modelo.RegistroAuditoria;

import java.util.ArrayList;
import java.util.List;

/** Solo insertar y consultar. A proposito no hay metodos para modificar ni borrar. */
@Stateless
public class RegistroAuditoriaDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public void agregar(RegistroAuditoria registro) {
        em.persist(registro);
    }

    /** Una pagina de registros, del mas reciente al mas viejo. */
    public List<RegistroAuditoria> buscar(String usuario, String entidad, String entidadId, int desde, int cantidad) {
        Filtro filtro = filtro(usuario, entidad, entidadId);
        TypedQuery<RegistroAuditoria> consulta = em.createQuery(
                "select r from RegistroAuditoria r where 1 = 1" + filtro.condiciones()
                        + " order by r.instante desc, r.id desc", RegistroAuditoria.class);
        filtro.parametros().forEach(p -> consulta.setParameter((String) p[0], p[1]));
        return consulta.setFirstResult(desde).setMaxResults(cantidad).getResultList();
    }

    public long contar(String usuario, String entidad, String entidadId) {
        Filtro filtro = filtro(usuario, entidad, entidadId);
        TypedQuery<Long> consulta = em.createQuery(
                "select count(r) from RegistroAuditoria r where 1 = 1" + filtro.condiciones(), Long.class);
        filtro.parametros().forEach(p -> consulta.setParameter((String) p[0], p[1]));
        return consulta.getSingleResult();
    }

    private record Filtro(String condiciones, List<Object[]> parametros) {
    }

    /** Las mismas condiciones para la pagina y para el conteo, asi nunca se desalinean. */
    private static Filtro filtro(String usuario, String entidad, String entidadId) {
        StringBuilder condiciones = new StringBuilder();
        List<Object[]> parametros = new ArrayList<>();
        if (usuario != null && !usuario.isBlank()) {
            condiciones.append(" and r.usuario = :usuario");
            parametros.add(new Object[]{"usuario", usuario.trim()});
        }
        if (entidad != null && !entidad.isBlank()) {
            condiciones.append(" and r.entidad = :entidad");
            parametros.add(new Object[]{"entidad", entidad.trim()});
        }
        if (entidadId != null && !entidadId.isBlank()) {
            condiciones.append(" and r.entidadId = :entidadId");
            parametros.add(new Object[]{"entidadId", entidadId.trim()});
        }
        return new Filtro(condiciones.toString(), parametros);
    }
}
