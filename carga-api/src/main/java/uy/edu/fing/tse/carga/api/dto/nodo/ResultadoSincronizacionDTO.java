package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;

/** Que paso al traer las pesadas de una matricula desde el nodo Balanzas y guardarlas. */
public class ResultadoSincronizacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean nodoDisponible;
    private String mensaje;
    /** Las que mando el nodo. */
    private int recibidas;
    /** Las que no estaban y se guardaron. */
    private int nuevas;
    /** Las que ya estaban de una sincronizacion anterior: no se duplican. */
    private int duplicadas;
    /** De las nuevas, cuantas quedaron asociadas a una guia vigente. */
    private int vinculadasAGuia;
    /** Las que el nodo mando incompletas o mal formadas y no se guardaron. */
    private int descartadas;

    public static ResultadoSincronizacionDTO nodoNoDisponible(String motivo) {
        ResultadoSincronizacionDTO r = new ResultadoSincronizacionDTO();
        r.nodoDisponible = false;
        r.mensaje = motivo + " No se guardo nada; se vuelve a intentar en la proxima sincronizacion.";
        return r;
    }

    public static ResultadoSincronizacionDTO completada(int recibidas, int nuevas, int duplicadas,
                                                        int vinculadasAGuia, int descartadas) {
        ResultadoSincronizacionDTO r = new ResultadoSincronizacionDTO();
        r.nodoDisponible = true;
        r.recibidas = recibidas;
        r.nuevas = nuevas;
        r.duplicadas = duplicadas;
        r.vinculadasAGuia = vinculadasAGuia;
        r.descartadas = descartadas;
        r.mensaje = recibidas + " recibida(s): " + nuevas + " nueva(s), " + duplicadas + " ya registrada(s)"
                + (descartadas > 0 ? ", " + descartadas + " descartada(s) por datos incompletos" : "") + ".";
        return r;
    }

    public boolean isNodoDisponible() { return nodoDisponible; }
    public void setNodoDisponible(boolean v) { this.nodoDisponible = v; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String v) { this.mensaje = v; }

    public int getRecibidas() { return recibidas; }
    public void setRecibidas(int v) { this.recibidas = v; }

    public int getNuevas() { return nuevas; }
    public void setNuevas(int v) { this.nuevas = v; }

    public int getDuplicadas() { return duplicadas; }
    public void setDuplicadas(int v) { this.duplicadas = v; }

    public int getVinculadasAGuia() { return vinculadasAGuia; }
    public void setVinculadasAGuia(int v) { this.vinculadasAGuia = v; }

    public int getDescartadas() { return descartadas; }
    public void setDescartadas(int v) { this.descartadas = v; }
}
