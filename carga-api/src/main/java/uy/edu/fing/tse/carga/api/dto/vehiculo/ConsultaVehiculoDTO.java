package uy.edu.fing.tse.carga.api.dto.vehiculo;

import jakarta.json.bind.annotation.JsonbNillable;
import java.io.Serializable;

/** Lectura F13: estados de presentacion a la fecha de referencia, no estados de baja. */
@JsonbNillable
public class ConsultaVehiculoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long empresaId;
    private String matricula;
    private boolean habilitado;
    private String numeroPermiso;
    private String permisoDesde;
    private String permisoHasta;
    private String itvVencimiento;
    private String estadoPermiso;
    private String estadoItv;

    public ConsultaVehiculoDTO() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
    public String getNumeroPermiso() { return numeroPermiso; }
    public void setNumeroPermiso(String numeroPermiso) { this.numeroPermiso = numeroPermiso; }
    public String getPermisoDesde() { return permisoDesde; }
    public void setPermisoDesde(String permisoDesde) { this.permisoDesde = permisoDesde; }
    public String getPermisoHasta() { return permisoHasta; }
    public void setPermisoHasta(String permisoHasta) { this.permisoHasta = permisoHasta; }
    public String getItvVencimiento() { return itvVencimiento; }
    public void setItvVencimiento(String itvVencimiento) { this.itvVencimiento = itvVencimiento; }
    public String getEstadoPermiso() { return estadoPermiso; }
    public void setEstadoPermiso(String estadoPermiso) { this.estadoPermiso = estadoPermiso; }
    public String getEstadoItv() { return estadoItv; }
    public void setEstadoItv(String estadoItv) { this.estadoItv = estadoItv; }
}
