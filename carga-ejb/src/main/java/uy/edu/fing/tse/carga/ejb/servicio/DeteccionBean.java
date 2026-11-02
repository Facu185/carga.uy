package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.DeteccionLocal;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CasoFiscalizacion;
import uy.edu.fing.tse.carga.ejb.modelo.ParametroRegla;
import uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO.Actividad;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO.Recorrido;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Deteccion automatica de incumplimientos (CU5, frente P5, ADR-0022).
 *
 * Asincronica respecto de los eventos que la originan: nunca la llama la ingesta, sino los consumidores
 * de la cola de pesadas y del fin de viaje, y una agenda que revisa la actividad de tracking. Asi la
 * recepcion de posiciones y de pesajes no espera a las reglas.
 *
 * Idempotente: cada caso lleva un "hecho" que lo identifica (una pesada, o un vehiculo en un dia) y la
 * base no admite dos casos con el mismo hecho. Reprocesar un evento no genera casos nuevos.
 *
 * Al crear un caso lo notifica en el acto: queda NOTIFICADA con su plazo de descargo, la empresa lo ve
 * en su bandeja del frontoffice, y la creacion y la notificacion quedan en la auditoria.
 */
@Stateless
public class DeteccionBean implements DeteccionLocal {

    static final ZoneId ZONA = ZoneId.of("America/Montevideo");
    static final String USUARIO_SISTEMA = "sistema";
    static final String METRICA_CASOS = "carga_fiscalizacion_casos_total";
    static final String CANAL_NOTIFICACION = "BANDEJA_FRONTOFFICE";

    private static final Logger LOG = Logger.getLogger(DeteccionBean.class.getName());

    @EJB
    FiscalizacionDAO dao;

    @EJB
    AuditoriaLocal auditoria;

    @EJB
    MetricasLocal metricas;

    @Override
    public int evaluarPesada(PesadaRegistradaDTO pesada) {
        Optional<Vehiculo> vehiculo = dao.vehiculo(pesada.getMatricula());
        if (vehiculo.isEmpty()) {
            // Un vehiculo que no esta registrado no se puede atribuir a una empresa: no genera caso
            metricas.contar(METRICA_CASOS, "tipo", "ninguno", "resultado", "vehiculo_no_registrado");
            return 0;
        }
        Vehiculo v = vehiculo.get();
        OffsetDateTime instante;
        try {
            instante = OffsetDateTime.parse(pesada.getInstante());
        } catch (DateTimeParseException | NullPointerException e) {
            LOG.warning("Pesada sin instante valido, no se evalua: " + pesada.getIdPesada());
            return 0;
        }
        LocalDate dia = instante.atZoneSameInstant(ZONA).toLocalDate();
        String lugar = "Pesada " + pesada.getIdPesada() + " en " + pesada.getPeaje() + " (" + dia + "). ";
        int creados = 0;

        Optional<String> exceso = ReglasFiscalizacion.exceso(pesada.getPesoKg(), v.getPesoKg(), v.getCapacidadCargaKg(),
                (int) numero("exceso_carga.tolerancia_kg", 500));
        if (exceso.isPresent()) {
            creados += crear(TipoInfraccion.EXCESO_CARGA, "EXCESO_CARGA:pesada:" + pesada.getIdPesada(), v,
                    pesada.getGuiaId(), lugar + exceso.get());
        }
        creados += evaluarHabilitacion(v, dia, lugar, pesada.getGuiaId());

        if (pesada.getGuiaId() == null) {
            creados += crear(TipoInfraccion.VIAJE_SIN_GUIA, hechoDelDia(TipoInfraccion.VIAJE_SIN_GUIA, v, dia), v, null,
                    lugar + "El vehículo fue pesado sin una guía de viaje vigente.");
        } else if ("ASIGNADA".equals(dao.estadoGuia(pesada.getGuiaId()).orElse(null))) {
            creados += crear(TipoInfraccion.VIAJE_SIN_GUIA, hechoDelDia(TipoInfraccion.VIAJE_SIN_GUIA, v, dia), v,
                    pesada.getGuiaId(), lugar + "La guía " + pesada.getGuiaId()
                            + " está asignada pero el chofer no registró el inicio del viaje: no había un viaje en curso.");
        }
        return creados;
    }

    @Override
    public int evaluarFinDeViaje(Long guiaId) {
        Optional<Vehiculo> vehiculo = dao.vehiculoDeGuia(guiaId);
        if (vehiculo.isEmpty()) {
            return 0;
        }
        double toleranciaM = numero("desvio_ruta.tolerancia_m", 5000);
        double relativaPct = numero("desvio_ruta.tolerancia_relativa_pct", 15);
        Optional<Recorrido> recorrido = dao.recorrido(guiaId, toleranciaM, relativaPct);
        if (recorrido.isEmpty()) {
            LOG.info("La guia " + guiaId + " no tiene coordenadas de origen y destino: no se evalua el desvio");
            return 0;
        }
        Recorrido r = recorrido.get();
        double corredor = ReglasFiscalizacion.corredorMetros(r.distanciaRectaM(), toleranciaM, relativaPct);
        Optional<String> desvio = ReglasFiscalizacion.desvio(r.distanciaRectaM(), corredor, r.posiciones(),
                r.fueraDelCorredor(), r.maximaDistanciaM(), numero("desvio_ruta.porcentaje_puntos", 20));
        return desvio.map(evidencia -> crear(TipoInfraccion.DESVIO_RUTA, "DESVIO_RUTA:guia:" + guiaId, vehiculo.get(),
                guiaId, "Guía " + guiaId + ". " + evidencia)).orElse(0);
    }

    @Override
    public int evaluarActividadTracking(int minutos) {
        int creados = 0;
        for (Actividad a : dao.actividadDesde(Instant.now().minus(Duration.ofMinutes(minutos)))) {
            Optional<Vehiculo> vehiculo = dao.vehiculo(a.matricula());
            if (vehiculo.isEmpty()) {
                continue;
            }
            LocalDate dia = a.hasta().atZoneSameInstant(ZONA).toLocalDate();
            String lugar = "Tracking: " + a.posiciones() + " posiciones entre " + a.desde().atZoneSameInstant(ZONA).toLocalTime()
                    + " y " + a.hasta().atZoneSameInstant(ZONA).toLocalTime() + " del " + dia + ". ";
            if (!a.conViaje()) {
                creados += crear(TipoInfraccion.VIAJE_SIN_GUIA, hechoDelDia(TipoInfraccion.VIAJE_SIN_GUIA, vehiculo.get(), dia),
                        vehiculo.get(), null, lugar + "El vehículo circulaba sin un viaje asignado y en curso.");
            }
            creados += evaluarHabilitacion(vehiculo.get(), dia, lugar, null);
        }
        return creados;
    }

    private int evaluarHabilitacion(Vehiculo v, LocalDate dia, String lugar, Long guiaId) {
        ReglasFiscalizacion.Habilitacion h = new ReglasFiscalizacion.Habilitacion(v.isHabilitado(),
                v.getEmpresa().isActiva(), v.getNumeroPermiso(), v.getPermisoDesde(), v.getPermisoHasta(),
                v.getItvVencimiento());
        return ReglasFiscalizacion.sinHabilitacion(h, dia)
                .map(evidencia -> crear(TipoInfraccion.SIN_HABILITACION,
                        hechoDelDia(TipoInfraccion.SIN_HABILITACION, v, dia), v, guiaId, lugar + evidencia))
                .orElse(0);
    }

    /** Un caso por vehiculo y dia: muchas posiciones o pesadas del mismo dia son el mismo hecho. */
    static String hechoDelDia(TipoInfraccion tipo, Vehiculo v, LocalDate dia) {
        return tipo.name() + ":" + v.getMatricula() + ":" + dia;
    }

    /** @return 1 si el caso es nuevo, 0 si el hecho ya tenia caso */
    int crear(TipoInfraccion tipo, String hecho, Vehiculo v, Long guiaId, String evidencia) {
        Long empresaId = v.getEmpresa().getId();
        Optional<Long> nuevo = dao.crearSiEsNuevo(tipo, hecho, empresaId, v.getId(), v.getMatricula(), guiaId, evidencia);
        if (nuevo.isEmpty()) {
            metricas.contar(METRICA_CASOS, "tipo", tipo.name(), "resultado", "duplicado");
            return 0;
        }
        Long id = nuevo.get();
        auditoria.registrarComo(USUARIO_SISTEMA, "SISTEMA", AccionAuditada.CASO_CREADO, "CasoFiscalizacion", id,
                "Detección automática: " + tipo.descripcion(), evidencia);
        notificar(id, empresaId, tipo, v.getMatricula());
        metricas.contar(METRICA_CASOS, "tipo", tipo.name(), "resultado", "nuevo");
        return 1;
    }

    /** DETECTADA -> NOTIFICADA: aviso en la bandeja de la empresa (y correo simulado en el log). */
    private void notificar(Long casoId, Long empresaId, TipoInfraccion tipo, String matricula) {
        CasoFiscalizacion caso = dao.caso(casoId).orElseThrow();
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(Duration.ofDays((long) numero("descargo.plazo_dias", 10)));
        caso.notificar(ahora, vence);
        String mensaje = "Se detectó un posible incumplimiento (" + tipo.descripcion() + ") del vehículo " + matricula
                + ". Puede presentar su descargo hasta el " + vence.atZone(ZONA).toLocalDate() + ".";
        dao.notificar(empresaId, casoId, CANAL_NOTIFICACION, mensaje);
        LOG.info("Correo simulado a la empresa " + empresaId + " por el caso " + casoId + ": " + mensaje);
        auditoria.registrarComo(USUARIO_SISTEMA, "SISTEMA", AccionAuditada.CASO_CAMBIO_ESTADO, "CasoFiscalizacion",
                casoId, "Notificación automática a la empresa", "DETECTADA -> NOTIFICADA; vence el descargo " + vence);
    }

    /** Lee el parametro en cada evaluacion: un cambio del administrador rige sin redesplegar. */
    double numero(String clave, double porDefecto) {
        try {
            return dao.parametro(clave).map(ParametroRegla::getValor).map(Double::parseDouble).orElse(porDefecto);
        } catch (NumberFormatException e) {
            LOG.warning("Parametro " + clave + " no numerico; se usa " + porDefecto);
            return porDefecto;
        }
    }
}
