package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uy.edu.fing.tse.carga.api.dto.comun.PosicionDTO;

import java.time.OffsetDateTime;

/**
 * Posicion recibida del nodo Tracking.
 * La columna geometrica "geom" la calcula PostGIS a partir de latitud y longitud
 * (columna generada, ver la migracion V1), por eso no se mapea en la entidad.
 */
@Entity
@Table(name = "posicion_tracking")
public class PosicionTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador generado en el origen. Unico: garantiza idempotencia ante reenvios. */
    @Column(name = "id_evento", nullable = false, unique = true, length = 80)
    private String idEvento;

    @Column(nullable = false, length = 20)
    private String matricula;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    /** Marca de tiempo del origen: la ruta se ordena por esta y no por el orden de llegada. */
    @Column(nullable = false)
    private OffsetDateTime instante;

    @Column(name = "recibido_en", nullable = false)
    private OffsetDateTime recibidoEn = OffsetDateTime.now();

    public static PosicionTracking desde(PosicionDTO dto, OffsetDateTime recibidoEn) {
        PosicionTracking p = new PosicionTracking();
        p.setIdEvento(dto.getIdEvento().trim());
        p.setMatricula(dto.getMatricula().trim());
        p.setLatitud(dto.getLatitud());
        p.setLongitud(dto.getLongitud());
        p.setInstante(OffsetDateTime.parse(dto.getInstante().trim()));
        p.setRecibidoEn(recibidoEn);
        return p;
    }

    public Long getId() { return id; }
    public String getIdEvento() { return idEvento; }
    public void setIdEvento(String idEvento) { this.idEvento = idEvento; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }
    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
    public OffsetDateTime getInstante() { return instante; }
    public void setInstante(OffsetDateTime instante) { this.instante = instante; }
    public OffsetDateTime getRecibidoEn() { return recibidoEn; }
    public void setRecibidoEn(OffsetDateTime recibidoEn) { this.recibidoEn = recibidoEn; }
}
