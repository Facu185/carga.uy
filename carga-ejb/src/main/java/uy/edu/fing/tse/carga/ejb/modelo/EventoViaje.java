package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import uy.edu.fing.tse.carga.api.dto.viaje.EventoViajeDTO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;

/** Tabla evento_viaje (frente P3). La columna geom la genera la base a partir de latitud/longitud. */
@Entity
@Table(name = "evento_viaje")
public class EventoViaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guia_id")
    private Guia guia;

    @Column(name = "id_evento", nullable = false, unique = true, length = 80)
    private String idEvento;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false)
    private Instant instante;

    @Column(columnDefinition = "text")
    private String detalle;

    /** bytea; sin @Lob, que en PostgreSQL mapearia a oid. */
    @Column(name = "foto")
    private byte[] foto;

    private Double latitud;
    private Double longitud;

    @Column(name = "recibido_en", nullable = false)
    private Instant recibidoEn;

    protected EventoViaje() {
    }

    public EventoViaje(Guia guia, String idEvento, TipoEventoViaje tipo, Instant instante, String detalle,
                       Double latitud, Double longitud, byte[] foto) {
        this.guia = guia;
        this.idEvento = idEvento;
        this.tipo = tipo.name();
        this.instante = instante.truncatedTo(ChronoUnit.MICROS);
        this.detalle = detalle == null || detalle.isBlank() ? null : detalle.trim();
        this.latitud = latitud;
        this.longitud = longitud;
        this.foto = foto == null ? null : foto.clone();
        this.recibidoEn = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    /** Contenido inmutable del evento, no metadatos de recepcion ni representacion JSON. */
    public boolean mismoContenido(EventoViaje otro) {
        return otro != null && Objects.equals(idEvento, otro.idEvento)
                && Objects.equals(guia.getId(), otro.guia.getId()) && Objects.equals(tipo, otro.tipo)
                && Objects.equals(instante, otro.instante) && Objects.equals(detalle, otro.detalle)
                && Objects.equals(latitud, otro.latitud) && Objects.equals(longitud, otro.longitud)
                && Arrays.equals(foto, otro.foto);
    }

    public EventoViajeDTO aDTO() {
        return dto(idEvento, guia.getId(), tipo, instante, detalle, latitud, longitud, foto != null, recibidoEn);
    }

    /** Para armar el DTO desde una consulta que no trae los bytes de la foto. */
    public static EventoViajeDTO dto(String idEvento, Long guiaId, String tipo, Instant instante, String detalle,
                                     Double latitud, Double longitud, boolean tieneFoto, Instant recibidoEn) {
        EventoViajeDTO dto = new EventoViajeDTO();
        dto.setIdEvento(idEvento);
        dto.setGuiaId(guiaId);
        dto.setTipo(tipo);
        dto.setInstante(instante.toString());
        dto.setDetalle(detalle);
        dto.setLatitud(latitud);
        dto.setLongitud(longitud);
        dto.setTieneFoto(tieneFoto);
        dto.setRecibidoEn(recibidoEn.toString());
        return dto;
    }

    public Long getId() { return id; }
    public Guia getGuia() { return guia; }
    public String getIdEvento() { return idEvento; }
    public String getTipo() { return tipo; }
    public Instant getInstante() { return instante; }
    public String getDetalle() { return detalle; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public byte[] getFoto() { return foto == null ? null : foto.clone(); }
    public Instant getRecibidoEn() { return recibidoEn; }
}
