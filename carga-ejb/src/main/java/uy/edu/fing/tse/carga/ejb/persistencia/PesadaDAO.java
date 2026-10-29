package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.ejb.modelo.Pesada;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Acceso a las pesadas (P4) y la unica lectura que P4 hace sobre las guias de P2. */
@Stateless
public class PesadaDAO {

    /** Las fechas que manda el usuario son dias de Uruguay, no de UTC. */
    public static final ZoneId ZONA = ZoneId.of("America/Montevideo");

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    /**
     * Guarda la pesada solo si no estaba. La deduplicacion la hace la base con ON CONFLICT y no un
     * "select y despues insert": dos sincronizaciones simultaneas (la agenda y un pedido manual)
     * pasarian las dos el select y la segunda fallaria al insertar.
     *
     * @return true si era nueva, false si ya estaba registrada
     */
    public boolean guardarSiEsNueva(Pesada p) {
        int insertadas = em.createNativeQuery(
                        "INSERT INTO pesada (id_pesada, guia_id, matricula, peaje, peso_kg, instante, obtenida_en) "
                                + "VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7) ON CONFLICT (id_pesada) DO NOTHING")
                .setParameter(1, p.getIdPesada())
                .setParameter(2, p.getGuiaId())
                .setParameter(3, p.getMatricula())
                .setParameter(4, p.getPeaje())
                .setParameter(5, p.getPesoKg())
                .setParameter(6, p.getInstante())
                .setParameter(7, p.getObtenidaEn())
                .executeUpdate();
        return insertadas == 1;
    }

    public Optional<Pesada> buscarPorIdPesada(String idPesada) {
        return em.createQuery("select p from Pesada p where p.idPesada = :id", Pesada.class)
                .setParameter("id", idPesada)
                .getResultStream()
                .findFirst();
    }

    /** Pesadas de una matricula en un dia de Uruguay, por orden de la balanza. */
    public List<Pesada> buscar(String matricula, LocalDate dia) {
        return em.createQuery("select p from Pesada p where p.matricula = :m"
                        + " and p.instante >= :desde and p.instante < :hasta order by p.instante", Pesada.class)
                .setParameter("m", matricula)
                .setParameter("desde", inicioDe(dia))
                .setParameter("hasta", inicioDe(dia.plusDays(1)))
                .getResultList();
    }

    /** Pesadas vinculadas a una guia, por orden de la balanza, hasta maximo filas. */
    public List<Pesada> buscarDeGuia(Long guiaId, int maximo) {
        return em.createQuery("select p from Pesada p where p.guiaId = :g order by p.instante, p.id", Pesada.class)
                .setParameter("g", guiaId)
                .setMaxResults(maximo)
                .getResultList();
    }

    /**
     * Guia del vehiculo vigente en el instante del pesaje. Los periodos de guia son semiabiertos
     * [inicio, fin), igual que en P2 (ADR-0005). Solo lectura sobre la tabla compartida guia.
     */
    public Optional<Long> guiaVigente(String matricula, Instant instante) {
        return em.createQuery("select g.id from Guia g where g.vehiculo.matricula = :m"
                        + " and g.inicio <= :i and g.fin > :i order by g.inicio desc", Long.class)
                .setParameter("m", matricula)
                .setParameter("i", instante)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    /** Matriculas con alguna guia cuyo periodo se cruza con [desde, hasta): las que hay que sincronizar. */
    public List<String> matriculasConGuiaEntre(Instant desde, Instant hasta) {
        return em.createQuery("select distinct g.vehiculo.matricula from Guia g"
                        + " where g.inicio < :hasta and g.fin > :desde", String.class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }

    private static OffsetDateTime inicioDe(LocalDate dia) {
        return dia.atStartOfDay(ZONA).toOffsetDateTime();
    }
}
