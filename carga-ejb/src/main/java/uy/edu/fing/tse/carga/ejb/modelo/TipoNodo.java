package uy.edu.fing.tse.carga.ejb.modelo;

/** Los dos nodos perifericos de la letra (Seccion 3.7). */
public enum TipoNodo {
    /** El central lo consulta (request-response): tiene URL. */
    BALANZAS,
    /** Empuja posiciones al central: se identifica con su token (ADR-0007). */
    TRACKING
}
