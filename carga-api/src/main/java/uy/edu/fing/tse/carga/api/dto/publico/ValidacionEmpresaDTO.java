package uy.edu.fing.tse.carga.api.dto.publico;

import java.io.Serializable;

/** Respuesta publica sobre una empresa: solo su nombre publico y si esta habilitada. */
public class ValidacionEmpresaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long numeroEmpresa;
    private boolean encontrada;
    /** Nombre publico, para que quien consulta confirme que es la empresa que buscaba. */
    private String nombre;
    private boolean habilitada;
    /** aaaa-mm-dd */
    private String fechaReferencia;

    public long getNumeroEmpresa() { return numeroEmpresa; }
    public void setNumeroEmpresa(long numeroEmpresa) { this.numeroEmpresa = numeroEmpresa; }

    public boolean isEncontrada() { return encontrada; }
    public void setEncontrada(boolean encontrada) { this.encontrada = encontrada; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public boolean isHabilitada() { return habilitada; }
    public void setHabilitada(boolean habilitada) { this.habilitada = habilitada; }

    public String getFechaReferencia() { return fechaReferencia; }
    public void setFechaReferencia(String fechaReferencia) { this.fechaReferencia = fechaReferencia; }
}
