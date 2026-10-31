package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.*;
import uy.edu.fing.tse.carga.api.dto.guia.GuiaDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "guia")
public class Guia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "empresa_id") private Empresa empresa;
    @ManyToOne(optional = false) @JoinColumn(name = "vehiculo_id") private Vehiculo vehiculo;
    @ManyToOne(optional = false) @JoinColumn(name = "chofer_id") private Chofer chofer;
    @ManyToOne @JoinColumn(name = "rubro_id") private Rubro rubro;
    @ManyToOne @JoinColumn(name = "tipo_carga_id") private TipoCarga tipoCarga;
    /** Peso declarado de la carga, en kilogramos. */
    @Column(name = "peso_kg", nullable = false, precision = 12, scale = 2) private BigDecimal pesoKg;
    /** Volumen declarado de la carga, en metros cubicos. Se persiste en la columna historica volumen. */
    @Column(name = "volumen", nullable = false, precision = 12, scale = 2) private BigDecimal volumenM3;
    @Column(name = "inicio", nullable = false) private Instant inicio;
    @Column(name = "fin", nullable = false) private Instant fin;
    /** Columna heredada usada por P3/P4; siempre se deriva del inicio UTC. */
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false, length = 200) private String origen;
    @Column(name = "origen_lat") private Double origenLat;
    @Column(name = "origen_lon") private Double origenLon;
    @Column(nullable = false, length = 200) private String destino;
    @Column(name = "destino_lat") private Double destinoLat;
    @Column(name = "destino_lon") private Double destinoLon;
    @Column(nullable = false, length = 30) private String estado;
    @Column(name = "creada_en", nullable = false) private Instant creadaEn;

    protected Guia() { }

    public Guia(Empresa empresa, Vehiculo vehiculo, Chofer chofer, Rubro rubro, TipoCarga tipoCarga,
                BigDecimal pesoKg, BigDecimal volumenM3, Instant inicio, Instant fin, String origen, Double origenLat, Double origenLon,
                String destino, Double destinoLat, Double destinoLon) {
        validarDatos(pesoKg, volumenM3, inicio, fin, origen, destino);
        this.empresa = empresa; this.vehiculo = vehiculo; this.chofer = chofer; this.rubro = rubro;
        this.tipoCarga = tipoCarga; this.pesoKg = pesoKg; this.volumenM3 = volumenM3; this.inicio = inicio; this.fin = fin;
        this.fecha = inicio.atZone(java.time.ZoneOffset.UTC).toLocalDate(); this.origen = origen.trim();
        this.origenLat = origenLat; this.origenLon = origenLon; this.destino = destino.trim();
        this.destinoLat = destinoLat; this.destinoLon = destinoLon; this.estado = "ASIGNADA"; this.creadaEn = Instant.now();
    }

    public static void validarDatos(BigDecimal pesoKg, BigDecimal volumenM3, Instant inicio, Instant fin, String origen, String destino) {
        if (pesoKg == null || pesoKg.signum() <= 0) throw new IllegalArgumentException("El peso en kg debe ser positivo.");
        if (volumenM3 == null || volumenM3.signum() <= 0) throw new IllegalArgumentException("El volumen en m3 debe ser positivo.");
        validarPeriodo(inicio, fin);
        if (origen == null || origen.isBlank() || destino == null || destino.isBlank())
            throw new IllegalArgumentException("El origen y el destino son obligatorios.");
        if (origen.trim().equalsIgnoreCase(destino.trim()))
            throw new IllegalArgumentException("El origen y el destino deben ser distintos.");
    }
    public static void validarPeriodo(Instant inicio, Instant fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) throw new IllegalArgumentException("El inicio debe ser anterior al fin del viaje.");
    }
    /** Intervalos semiabiertos [inicio, fin): compartir un limite no es conflicto. */
    public static boolean seSolapan(Instant inicioA, Instant finA, Instant inicioB, Instant finB) {
        return inicioA.isBefore(finB) && inicioB.isBefore(finA);
    }

    /** Valida independientemente los limites fisicos de peso y de volumen. */
    public static void validarCapacidades(Vehiculo vehiculo, BigDecimal pesoKg, BigDecimal volumenM3) {
        if (vehiculo == null) throw new IllegalArgumentException("El vehiculo es obligatorio.");
        if (pesoKg.compareTo(BigDecimal.valueOf(vehiculo.getCapacidadCargaKg())) > 0)
            throw new IllegalArgumentException("El peso de la carga supera la capacidad del vehiculo.");
        if (vehiculo.getCapacidadVolumenM3() == null)
            throw new IllegalArgumentException("La capacidad volumetrica del vehiculo no esta configurada.");
        if (volumenM3.compareTo(vehiculo.getCapacidadVolumenM3()) > 0)
            throw new IllegalArgumentException("El volumen de la carga supera la capacidad del vehiculo.");
    }

    /** P3: avanza el estado del viaje segun el evento del chofer (ver ReglasEventoViaje). */
    public void aplicarEvento(TipoEventoViaje tipo) {
        this.estado = ReglasEventoViaje.estadoTrasEvento(estado, tipo);
    }

    public GuiaDTO aDTO() {
        GuiaDTO dto = new GuiaDTO();
        dto.setId(id); dto.setEmpresaId(empresa.getId());
        // El esquema historico admite guias CREADA sin vehiculo o chofer asignado.
        dto.setVehiculoId(vehiculo == null ? null : vehiculo.getId()); dto.setChoferId(chofer == null ? null : chofer.getId());
        dto.setRubroId(rubro == null ? null : rubro.getId()); dto.setTipoCargaId(tipoCarga == null ? null : tipoCarga.getId());
        dto.setPesoKg(pesoKg); dto.setVolumenM3(volumenM3); dto.setInicio(inicio.toString()); dto.setFin(fin.toString()); dto.setOrigen(origen); dto.setOrigenLat(origenLat); dto.setOrigenLon(origenLon);
        dto.setDestino(destino); dto.setDestinoLat(destinoLat); dto.setDestinoLon(destinoLon); dto.setEstado(estado);
        dto.setCreadaEn(creadaEn.toString()); return dto;
    }
    public Long getId() { return id; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public Chofer getChofer() { return chofer; }
    public Instant getInicio() { return inicio; }
    public Instant getFin() { return fin; }
    public String getEstado() { return estado; }
}
