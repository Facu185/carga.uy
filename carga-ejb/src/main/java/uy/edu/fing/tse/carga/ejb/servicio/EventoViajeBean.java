package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import uy.edu.fing.tse.carga.api.contract.service.AvisoFinDeViajeLocal;
import uy.edu.fing.tse.carga.api.contract.service.EventoViajeLocal;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.dto.comun.ViajeFinalizadoDTO;
import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;
import uy.edu.fing.tse.carga.api.dto.viaje.ResultadoEventoDTO;
import uy.edu.fing.tse.carga.api.exception.ConflictoException;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.RecursoAjenoException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.Chofer;
import uy.edu.fing.tse.carga.ejb.modelo.EventoViaje;
import uy.edu.fing.tse.carga.ejb.modelo.Guia;
import uy.edu.fing.tse.carga.ejb.modelo.ReglasEventoViaje;
import uy.edu.fing.tse.carga.ejb.modelo.TipoEventoViaje;
import uy.edu.fing.tse.carga.ejb.persistencia.ChoferDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.EventoViajeDAO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Caso de uso critico 3, "Registrar evento de viaje" (frente P3). Procesamiento sincronico: la app
 * se entera en el acto si el evento fue aceptado o rechazado (ADR-0019).
 *
 * Idempotente por idEvento: el telefono reenvia lo que no sabe si llego, y un reenvio devuelve el
 * evento ya registrado sin duplicarlo ni volver a cambiar el estado de la guia.
 */
@Stateless
@DeclareRoles({Roles.CHOFER})
@RolesAllowed(Roles.CHOFER)
public class EventoViajeBean implements EventoViajeLocal {

    private static final Logger LOG = Logger.getLogger(EventoViajeBean.class.getName());
    static final String METRICA = "carga_eventos_viaje_total";
    static final int FOTO_MAX_BYTES = entero("EVENTOS_FOTO_MAX_BYTES", 2_000_000);
    static final String GUIA_AJENA = "La gu\u00eda no existe o no est\u00e1 asignada a vos.";

    @PersistenceContext(unitName = "cargaPU")
    EntityManager em;

    @EJB
    EventoViajeDAO eventos;

    @EJB
    ChoferDAO choferes;

    @EJB
    AvisoFinDeViajeLocal avisoFinDeViaje;

    @EJB
    MetricasLocal metricas;

    @Resource
    SessionContext contexto;

    @Override
    public ResultadoEventoDTO registrar(EventoViajeDTO dto) throws NegocioException {
        Optional<String> problema = ReglasEventoViaje.problemaDeFormato(dto, Instant.now());
        if (problema.isPresent()) {
            metricas.contar(METRICA, "tipo", "desconocido", "resultado", "invalido");
            throw new NegocioException(problema.get());
        }
        TipoEventoViaje tipo = ReglasEventoViaje.tipo(dto.getTipo()).orElseThrow();
        byte[] foto = foto(dto.getFotoBase64());
        Chofer chofer = choferAutenticado();

        // Bloquear la guia serializa los eventos de un mismo viaje: dos reenvios simultaneos no
        // pueden registrar el mismo evento dos veces ni pisarse el cambio de estado.
        Guia guia = guiaBloqueada(dto.getGuiaId());
        if (guia == null || guia.getChofer() == null || !guia.getChofer().getId().equals(chofer.getId())) {
            metricas.contar(METRICA, "tipo", tipo.name(), "resultado", "ajena");
            throw new RecursoAjenoException(GUIA_AJENA);
        }

        Optional<EventoViaje> previo = eventos.buscarPorIdEvento(dto.getIdEvento());
        if (previo.isPresent()) {
            EventoViaje evento = previo.get();
            if (!evento.getGuia().getId().equals(guia.getId()) || !evento.getTipo().equals(tipo.name())) {
                throw new ConflictoException("Ese idEvento ya se us\u00f3 para otro evento.");
            }
            metricas.contar(METRICA, "tipo", tipo.name(), "resultado", "repetido");
            return new ResultadoEventoDTO(false, guia.getEstado(), evento.aDTO());
        }

        Optional<String> rechazo = ReglasEventoViaje.motivoDeRechazo(guia.getEstado(), tipo);
        if (rechazo.isPresent()) {
            metricas.contar(METRICA, "tipo", tipo.name(), "resultado", "rechazado");
            throw new ConflictoException(rechazo.get());
        }

        Instant instante = ReglasEventoViaje.instante(dto.getInstante()).orElseThrow();
        EventoViaje evento = new EventoViaje(guia, dto.getIdEvento(), tipo, instante, dto.getDetalle(),
                dto.getLatitud(), dto.getLongitud(), foto);
        eventos.guardar(evento);
        guia.aplicarEvento(tipo);
        if (tipo == TipoEventoViaje.FIN) {
            avisarFinDeViaje(guia, instante);
        }
        metricas.contar(METRICA, "tipo", tipo.name(), "resultado", "nuevo");
        LOG.info(() -> "Evento de viaje " + tipo + " registrado en la guia " + guia.getId());
        return new ResultadoEventoDTO(true, guia.getEstado(), evento.aDTO());
    }

    @Override
    public List<EventoViajeDTO> eventosDeGuia(Long guiaId) throws NegocioException {
        if (guiaId == null) throw new NegocioException("La gu\u00eda es obligatoria.");
        Chofer chofer = choferAutenticado();
        Guia guia = em.find(Guia.class, guiaId);
        if (guia == null || guia.getChofer() == null || !guia.getChofer().getId().equals(chofer.getId())) {
            throw new RecursoAjenoException(GUIA_AJENA);
        }
        return eventos.deGuia(guiaId);
    }

    /** Dentro de la misma transaccion: si algo falla despues, el aviso no sale (ADR-0010). */
    private void avisarFinDeViaje(Guia guia, Instant fin) throws NegocioException {
        Instant inicio = eventos.instanteDeInicio(guia.getId()).orElse(guia.getInicio());
        avisoFinDeViaje.avisar(new ViajeFinalizadoDTO(guia.getId(), guia.getVehiculo().getMatricula(),
                inicio.toString(), fin.toString()));
    }

    private static byte[] foto(String base64) throws NegocioException {
        try {
            return ReglasEventoViaje.decodificarFoto(base64, FOTO_MAX_BYTES);
        } catch (IllegalArgumentException e) {
            throw new NegocioException(e.getMessage());
        }
    }

    /** Package-private para reemplazarlo en las pruebas. */
    Chofer choferAutenticado() throws NegocioException {
        if (contexto == null || contexto.getCallerPrincipal() == null) {
            throw new NegocioException("No hay un usuario autenticado.");
        }
        return choferes.buscarPorCedula(contexto.getCallerPrincipal().getName())
                .orElseThrow(() -> new RecursoAjenoException("No est\u00e1s registrado como chofer."));
    }

    /** Package-private para reemplazarlo en las pruebas. */
    Guia guiaBloqueada(Long guiaId) {
        return em.find(Guia.class, guiaId, LockModeType.PESSIMISTIC_WRITE);
    }

    private static int entero(String variable, int porDefecto) {
        String valor = System.getenv(variable);
        try {
            return valor == null || valor.isBlank() ? porDefecto : Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
