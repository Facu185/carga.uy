package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Umbral de una regla de fiscalizacion. Se lee en cada evaluacion: cambiarlo no requiere redesplegar. */
@Entity
@Table(name = "parametro_regla")
public class ParametroRegla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String clave;

    @Column(nullable = false, length = 200)
    private String valor;

    @Column(length = 300)
    private String descripcion;

    public String getClave() { return clave; }
    public String getValor() { return valor; }
    public String getDescripcion() { return descripcion; }

    public void setValor(String valor) { this.valor = valor; }
}
