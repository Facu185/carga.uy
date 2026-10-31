package uy.edu.fing.tse.carga.ejb.persistencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;
import uy.edu.fing.tse.carga.ejb.modelo.EventoViaje;
import uy.edu.fing.tse.carga.ejb.modelo.TipoEventoViaje;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Stateless
public class EventoViajeDAO {

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    public Optional<EventoViaje> buscarPorIdEvento(String idEvento) {
        return em.createQuery("select e from EventoViaje e where e.idEvento = :id", EventoViaje.class)
                .setParameter("id", idEvento)
                .getResultStream().findFirst();
    }

    public void guardar(EventoViaje evento) {
        em.persist(evento);
    }

    /** Sin los bytes de la foto: el historial solo necesita saber si hay. */
    public List<EventoViajeDTO> deGuia(Long guiaId) {
        return em.createQuery("select e.idEvento, e.tipo, e.instante, e.detalle, e.latitud, e.longitud, "
                        + "case when e.foto is null then false else true end, e.recibidoEn "
                        + "from EventoViaje e where e.guia.id = :guia order by e.instante, e.id", Object[].class)
                .setParameter("guia", guiaId)
                .getResultStream()
                .map(f -> EventoViaje.dto((String) f[0], guiaId, (String) f[1], (Instant) f[2], (String) f[3],
                        (Double) f[4], (Double) f[5], (Boolean) f[6], (Instant) f[7]))
                .toList();
    }

    public Optional<Instant> instanteDeInicio(Long guiaId) {
        return em.createQuery("select e.instante from EventoViaje e where e.guia.id = :guia and e.tipo = :tipo",
                        Instant.class)
                .setParameter("guia", guiaId)
                .setParameter("tipo", TipoEventoViaje.INICIO.name())
                .getResultStream().findFirst();
    }
}
