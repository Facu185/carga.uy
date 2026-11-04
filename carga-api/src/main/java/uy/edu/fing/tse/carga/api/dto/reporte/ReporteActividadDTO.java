package uy.edu.fing.tse.carga.api.dto.reporte;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Agregados de guias por creacion o por FIN aceptado segun criterioTemporal; no fiscalizacion completa. */
public class ReporteActividadDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String desde;
    private String hasta;
    private String zonaHoraria;
    private String criterioTemporal;
    private Long tipoCargaId;
    private String tipoCargaNombre;
    private long totalGuias;
    private long empresasConGuias;
    private List<ConteoActividadDTO> porDia = new ArrayList<>();
    private List<ConteoActividadDTO> porEstado = new ArrayList<>();
    private List<ConteoActividadDTO> porRubro = new ArrayList<>();

    public ReporteActividadDTO() { }

    public String getDesde() { return desde; }
    public void setDesde(String desde) { this.desde = desde; }
    public String getHasta() { return hasta; }
    public void setHasta(String hasta) { this.hasta = hasta; }
    public String getZonaHoraria() { return zonaHoraria; }
    public void setZonaHoraria(String zonaHoraria) { this.zonaHoraria = zonaHoraria; }
    public String getCriterioTemporal() { return criterioTemporal; }
    public void setCriterioTemporal(String criterioTemporal) { this.criterioTemporal = criterioTemporal; }
    public Long getTipoCargaId() { return tipoCargaId; }
    public void setTipoCargaId(Long tipoCargaId) { this.tipoCargaId = tipoCargaId; }
    public String getTipoCargaNombre() { return tipoCargaNombre; }
    public void setTipoCargaNombre(String tipoCargaNombre) { this.tipoCargaNombre = tipoCargaNombre; }
    public long getTotalGuias() { return totalGuias; }
    public void setTotalGuias(long totalGuias) { this.totalGuias = totalGuias; }
    public long getEmpresasConGuias() { return empresasConGuias; }
    public void setEmpresasConGuias(long empresasConGuias) { this.empresasConGuias = empresasConGuias; }
    public List<ConteoActividadDTO> getPorDia() { return porDia; }
    public void setPorDia(List<ConteoActividadDTO> porDia) { this.porDia = porDia; }
    public List<ConteoActividadDTO> getPorEstado() { return porEstado; }
    public void setPorEstado(List<ConteoActividadDTO> porEstado) { this.porEstado = porEstado; }
    public List<ConteoActividadDTO> getPorRubro() { return porRubro; }
    public void setPorRubro(List<ConteoActividadDTO> porRubro) { this.porRubro = porRubro; }
}
