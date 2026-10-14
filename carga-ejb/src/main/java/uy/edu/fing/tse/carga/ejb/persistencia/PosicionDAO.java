package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.PosicionTracking;

@Stateless
public class PosicionDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public boolean guardarSiEsNueva(PosicionTracking p) {
        int insertadas = em.createNativeQuery(
                "INSERT INTO posicion_tracking (id_evento, matricula, latitud, longitud, instante, recibido_en) "
                    + "VALUES (?1, ?2, ?3, ?4, ?5, ?6) ON CONFLICT (id_evento) DO NOTHING")
            .setParameter(1, p.getIdEvento())
            .setParameter(2, p.getMatricula())
            .setParameter(3, p.getLatitud())
            .setParameter(4, p.getLongitud())
            .setParameter(5, p.getInstante())
            .setParameter(6, p.getRecibidoEn())
            .executeUpdate();
        return insertadas == 1;
    }
}
