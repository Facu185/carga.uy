package uy.edu.fing.tse.carga.api.dto.vehiculo;

import java.io.Serializable;
import java.util.List;

/** Pagina C13 con fecha de servidor comun a las vigencias de todos sus registros. */
public class PaginaConsultaVehiculosDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ConsultaVehiculoDTO> registros = List.of();
    private int pagina;
    private int tamano;
    private long total;
    private String fechaReferencia;
    private String zonaHoraria;

    public PaginaConsultaVehiculosDTO() { }

    public List<ConsultaVehiculoDTO> getRegistros() { return registros; }
    public void setRegistros(List<ConsultaVehiculoDTO> registros) { this.registros = registros; }
    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }
    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public String getFechaReferencia() { return fechaReferencia; }
    public void setFechaReferencia(String fechaReferencia) { this.fechaReferencia = fechaReferencia; }
    public String getZonaHoraria() { return zonaHoraria; }
    public void setZonaHoraria(String zonaHoraria) { this.zonaHoraria = zonaHoraria; }
}
