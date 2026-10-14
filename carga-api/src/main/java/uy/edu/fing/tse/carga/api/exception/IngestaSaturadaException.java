package uy.edu.fing.tse.carga.api.exception;


public class IngestaSaturadaException extends ServicioNoDisponibleException {

    private static final long serialVersionUID = 1L;

    private final int reintentarEnSegundos;

    public IngestaSaturadaException(String mensaje, int reintentarEnSegundos) {
        super(mensaje);
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public int getReintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
