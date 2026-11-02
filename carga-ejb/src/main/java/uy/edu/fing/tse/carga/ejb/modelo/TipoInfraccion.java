package uy.edu.fing.tse.carga.ejb.modelo;

/** Situaciones que la plataforma detecta sola (letra, Fiscalizacion Automatica). */
public enum TipoInfraccion {
    EXCESO_CARGA("Exceso de carga"),
    SIN_HABILITACION("Vehiculo sin habilitacion vigente"),
    VIAJE_SIN_GUIA("Viaje sin guia"),
    DESVIO_RUTA("Desvio de ruta");

    private final String descripcion;

    TipoInfraccion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
