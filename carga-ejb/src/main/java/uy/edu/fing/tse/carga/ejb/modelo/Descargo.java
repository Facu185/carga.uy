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

import java.time.Instant;

/** Descargo de la empresa sobre un caso (uno por caso: indice unico en V12). */
@Entity
@Table(name = "descargo")
public class Descargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caso_id", nullable = false)
    private Long casoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fundamento;

    @Column(name = "presentado_en", nullable = false)
    private Instant presentadoEn;

    protected Descargo() {
    }

    public Descargo(Long casoId, Usuario usuario, String fundamento, Instant presentadoEn) {
        this.casoId = casoId;
        this.usuario = usuario;
        this.fundamento = fundamento;
        this.presentadoEn = presentadoEn;
    }

    public Long getId() { return id; }
    public Long getCasoId() { return casoId; }
    public Usuario getUsuario() { return usuario; }
    public String getFundamento() { return fundamento; }
    public Instant getPresentadoEn() { return presentadoEn; }
}
