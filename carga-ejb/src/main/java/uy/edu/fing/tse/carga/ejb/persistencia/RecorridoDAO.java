package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.PosicionTracking;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Lecturas del recorrido de un vehiculo (frente P4). Solo lectura sobre posicion_tracking y guia. */
@Stateless
public class RecorridoDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    /** Hasta {@code maximo} posiciones del periodo [desde, hasta), por hora del dispositivo. */
    public List<PosicionTracking> posiciones(String matricula, OffsetDateTime desde, OffsetDateTime hasta, int maximo) {
        return em.createQuery("select p from PosicionTracking p where p.matricula = :m"
                        + " and p.instante >= :desde and p.instante < :hasta order by p.instante, p.id",
                        PosicionTracking.class)
                .setParameter("m", matricula)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .setMaxResults(maximo)
                .getResultList();
    }

    /**
     * Largo del recorrido en metros, calculado por PostGIS sobre todas las posiciones del periodo:
     * une los puntos en orden y mide sobre la superficie de la Tierra (geography), no en grados.
     */
    public double distanciaMetros(String matricula, OffsetDateTime desde, OffsetDateTime hasta) {
        Object r = em.createNativeQuery(
                        "SELECT COALESCE(ST_Length(ST_MakeLine(geom ORDER BY instante, id)::geography), 0) "
                                + "FROM posicion_tracking WHERE matricula = ?1 AND instante >= ?2 AND instante < ?3")
                .setParameter(1, matricula)
                .setParameter(2, desde)
                .setParameter(3, hasta)
                .getSingleResult();
        return r instanceof Number n ? n.doubleValue() : 0;
    }

    /** Matricula del vehiculo y periodo [inicio, fin) de la guia. Lectura de la tabla de P2. */
    public Optional<PeriodoDeGuia> periodoDeGuia(Long guiaId) {
        return em.createQuery("select g.vehiculo.matricula, g.inicio, g.fin from Guia g where g.id = :id", Object[].class)
                .setParameter("id", guiaId)
                .getResultStream()
                .findFirst()
                .map(f -> new PeriodoDeGuia((String) f[0], (Instant) f[1], (Instant) f[2]));
    }

    public record PeriodoDeGuia(String matricula, Instant inicio, Instant fin) {
    }
}
