package uy.edu.fing.tse.carga.api.dto.publico;

import java.io.Serializable;

/**
 * Respuesta publica sobre un vehiculo. No dice a que empresa pertenece ni quien lo maneja:
 * solo si puede circular. Estados: VIGENTE, FUTURO, VENCIDO o SIN_DATOS (ITV: sin FUTURO).
 */
public class ValidacionVehiculoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String matricula;
    private boolean encontrado;
    /** true solo si el vehiculo y su empresa estan habilitados y el permiso y la ITV estan vigentes. */
    private boolean vigente;
    private boolean vehiculoHabilitado;
    private boolean empresaHabilitada;
    private String estadoPermiso;
    /** aaaa-mm-dd */
    private String permisoHasta;
    private String estadoItv;
    /** aaaa-mm-dd */
    private String itvVencimiento;
    /** Dia de Uruguay contra el que se evaluaron las vigencias, aaaa-mm-dd. */
    private String fechaReferencia;

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public boolean isEncontrado() { return encontrado; }
    public void setEncontrado(boolean encontrado) { this.encontrado = encontrado; }

    public boolean isVigente() { return vigente; }
    public void setVigente(boolean vigente) { this.vigente = vigente; }

    public boolean isVehiculoHabilitado() { return vehiculoHabilitado; }
    public void setVehiculoHabilitado(boolean vehiculoHabilitado) { this.vehiculoHabilitado = vehiculoHabilitado; }

    public boolean isEmpresaHabilitada() { return empresaHabilitada; }
    public void setEmpresaHabilitada(boolean empresaHabilitada) { this.empresaHabilitada = empresaHabilitada; }

    public String getEstadoPermiso() { return estadoPermiso; }
    public void setEstadoPermiso(String estadoPermiso) { this.estadoPermiso = estadoPermiso; }

    public String getPermisoHasta() { return permisoHasta; }
    public void setPermisoHasta(String permisoHasta) { this.permisoHasta = permisoHasta; }

    public String getEstadoItv() { return estadoItv; }
    public void setEstadoItv(String estadoItv) { this.estadoItv = estadoItv; }

    public String getItvVencimiento() { return itvVencimiento; }
    public void setItvVencimiento(String itvVencimiento) { this.itvVencimiento = itvVencimiento; }

    public String getFechaReferencia() { return fechaReferencia; }
    public void setFechaReferencia(String fechaReferencia) { this.fechaReferencia = fechaReferencia; }
}
