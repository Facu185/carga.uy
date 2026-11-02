package uy.edu.fing.tse.carga.api.dto.publico;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Listados de informacion publica: cifras agregadas de los ultimos 12 meses (por fecha de inicio
 * de la guia, en America/Montevideo). Los totales que no alcanzan el umbral vienen en null.
 */
public class EstadisticasPublicasDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Primer mes incluido, aaaa-mm. */
    private String desde;
    /** Ultimo mes incluido, aaaa-mm. */
    private String hasta;
    /** Una cifra se publica solo si reune al menos umbralGuias guias de umbralEmpresas empresas distintas. */
    private int umbralGuias;
    private int umbralEmpresas;
    private Long empresasHabilitadas;
    private Long vehiculosHabilitados;
    /** Vehiculos habilitados con permiso nacional e ITV vigentes hoy. */
    private Long vehiculosAlDia;
    private List<CifraPublicaDTO> porMes = new ArrayList<>();
    private List<CifraPublicaDTO> porTipoCarga = new ArrayList<>();
    private List<CifraPublicaDTO> porRubro = new ArrayList<>();
    /** ISO-8601. */
    private String generadoEn;

    public String getDesde() { return desde; }
    public void setDesde(String desde) { this.desde = desde; }

    public String getHasta() { return hasta; }
    public void setHasta(String hasta) { this.hasta = hasta; }

    public int getUmbralGuias() { return umbralGuias; }
    public void setUmbralGuias(int umbralGuias) { this.umbralGuias = umbralGuias; }

    public int getUmbralEmpresas() { return umbralEmpresas; }
    public void setUmbralEmpresas(int umbralEmpresas) { this.umbralEmpresas = umbralEmpresas; }

    public Long getEmpresasHabilitadas() { return empresasHabilitadas; }
    public void setEmpresasHabilitadas(Long empresasHabilitadas) { this.empresasHabilitadas = empresasHabilitadas; }

    public Long getVehiculosHabilitados() { return vehiculosHabilitados; }
    public void setVehiculosHabilitados(Long vehiculosHabilitados) { this.vehiculosHabilitados = vehiculosHabilitados; }

    public Long getVehiculosAlDia() { return vehiculosAlDia; }
    public void setVehiculosAlDia(Long vehiculosAlDia) { this.vehiculosAlDia = vehiculosAlDia; }

    public List<CifraPublicaDTO> getPorMes() { return porMes; }
    public void setPorMes(List<CifraPublicaDTO> porMes) { this.porMes = porMes; }

    public List<CifraPublicaDTO> getPorTipoCarga() { return porTipoCarga; }
    public void setPorTipoCarga(List<CifraPublicaDTO> porTipoCarga) { this.porTipoCarga = porTipoCarga; }

    public List<CifraPublicaDTO> getPorRubro() { return porRubro; }
    public void setPorRubro(List<CifraPublicaDTO> porRubro) { this.porRubro = porRubro; }

    public String getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(String generadoEn) { this.generadoEn = generadoEn; }
}
