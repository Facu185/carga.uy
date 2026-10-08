package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Pesada obtenida del nodo Balanzas (tabla creada en la V1). */
@Entity
@Table(name = "pesada")
public class Pesada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador que genera la balanza: unico, es lo que impide guardar dos veces la misma. */
    @Column(name = "id_pesada", nullable = false, unique = true, length = 80)
    private String idPesada;

    /**
     * Solo el id y no una relacion con Guia: la guia es del frente P2 y una pesada no necesita
     * cargar el grafo de guia, vehiculo, chofer y empresa.
     */
    @Column(name = "guia_id")
    private Long guiaId;

    @Column(nullable = false, length = 20)
    private String matricula;

    @Column(length = 100)
    private String peaje;

    @Column(name = "peso_kg", nullable = false)
    private int pesoKg;

    @Column(nullable = false)
    private OffsetDateTime instante;

    @Column(name = "obtenida_en", nullable = false)
    private OffsetDateTime obtenidaEn;

    protected Pesada() {
    }

    /**
     * Valida lo que manda el nodo antes de guardarlo: un nodo externo puede mandar datos
     * incompletos, y una pesada sin identificador o sin instante no se puede deduplicar ni ubicar.
     *
     * @throws IllegalArgumentException si la pesada viene incompleta o mal formada
     */
    public static Pesada desde(PesadaDTO dto, Long guiaId, OffsetDateTime obtenidaEn) {
        if (dto == null || vacio(dto.getIdPesada()) || vacio(dto.getMatricula()) || vacio(dto.getInstante())) {
            throw new IllegalArgumentException("pesada sin identificador, matricula o instante");
        }
        if (dto.getPesoKg() <= 0) {
            throw new IllegalArgumentException("peso no positivo en la pesada " + dto.getIdPesada());
        }
        Pesada p = new Pesada();
        p.idPesada = dto.getIdPesada().trim();
        p.matricula = dto.getMatricula().trim();
        p.peaje = dto.getPeaje();
        p.pesoKg = dto.getPesoKg();
        p.instante = instanteDe(dto);
        p.guiaId = guiaId;
        p.obtenidaEn = obtenidaEn;
        return p;
    }

    /** @throws IllegalArgumentException si el instante no es ISO-8601 con zona */
    public static OffsetDateTime instanteDe(PesadaDTO dto) {
        try {
            return OffsetDateTime.parse(dto.getInstante().trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("instante invalido en la pesada " + dto.getIdPesada(), e);
        }
    }

    public PesadaRegistradaDTO aDTO() {
        PesadaRegistradaDTO dto = new PesadaRegistradaDTO();
        dto.setId(id);
        dto.setIdPesada(idPesada);
        dto.setGuiaId(guiaId);
        dto.setMatricula(matricula);
        dto.setPeaje(peaje);
        dto.setPesoKg(pesoKg);
        dto.setInstante(instante.toString());
        dto.setObtenidaEn(obtenidaEn.toString());
        return dto;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    public Long getId() { return id; }
    public String getIdPesada() { return idPesada; }
    public Long getGuiaId() { return guiaId; }
    public String getMatricula() { return matricula; }
    public String getPeaje() { return peaje; }
    public int getPesoKg() { return pesoKg; }
    public OffsetDateTime getInstante() { return instante; }
    public OffsetDateTime getObtenidaEn() { return obtenidaEn; }
}
