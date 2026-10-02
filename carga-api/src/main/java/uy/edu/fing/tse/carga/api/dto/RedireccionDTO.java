package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/**
 * Paso del ingreso con gub.uy: a donde redirigir al navegador y que guardar en la cookie de la transaccion.
 * transaccion null = borrar la cookie.
 */
public class RedireccionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String url;
    private String transaccion;
    private int vigenciaSegundos;

    public RedireccionDTO() {
    }

    public RedireccionDTO(String url, String transaccion, int vigenciaSegundos) {
        this.url = url;
        this.transaccion = transaccion;
        this.vigenciaSegundos = vigenciaSegundos;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTransaccion() { return transaccion; }
    public void setTransaccion(String transaccion) { this.transaccion = transaccion; }

    public int getVigenciaSegundos() { return vigenciaSegundos; }
    public void setVigenciaSegundos(int vigenciaSegundos) { this.vigenciaSegundos = vigenciaSegundos; }

}
