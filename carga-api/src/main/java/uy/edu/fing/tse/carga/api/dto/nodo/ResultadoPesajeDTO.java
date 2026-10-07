package uy.edu.fing.tse.carga.api.dto.nodo;

import uy.edu.fing.tse.carga.api.dto.comun.PesadaDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de consultar al nodo Balanzas.
 *
 * Una lista vacia es ambigua: no distingue "no hubo pesadas" de "el nodo no
 * respondio". Quien consume necesita saber cual de las dos cosas paso para
 * decidir si degrada o si concluye que no hay infraccion, asi que el resultado
 * lo dice explicitamente.
 */
public class ResultadoPesajeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean nodoDisponible;
    private String mensaje;
    private long latenciaMs;
    private List<PesadaDTO> pesadas = new ArrayList<>();

    public static ResultadoPesajeDTO disponible(List<PesadaDTO> pesadas, long latenciaMs) {
        ResultadoPesajeDTO r = new ResultadoPesajeDTO();
        r.nodoDisponible = true;
        r.pesadas = pesadas;
        r.latenciaMs = latenciaMs;
        r.mensaje = "El nodo respondio " + pesadas.size() + " pesada(s) en " + latenciaMs + " ms.";
        return r;
    }

    public static ResultadoPesajeDTO noDisponible(String motivo, long latenciaMs) {
        ResultadoPesajeDTO r = new ResultadoPesajeDTO();
        r.nodoDisponible = false;
        r.latenciaMs = latenciaMs;
        r.mensaje = motivo;
        return r;
    }

    public boolean isNodoDisponible() { return nodoDisponible; }
    public void setNodoDisponible(boolean v) { this.nodoDisponible = v; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String v) { this.mensaje = v; }

    public long getLatenciaMs() { return latenciaMs; }
    public void setLatenciaMs(long v) { this.latenciaMs = v; }

    public List<PesadaDTO> getPesadas() { return pesadas; }
    public void setPesadas(List<PesadaDTO> v) { this.pesadas = v; }
}
