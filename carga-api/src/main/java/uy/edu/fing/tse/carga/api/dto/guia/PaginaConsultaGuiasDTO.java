package uy.edu.fing.tse.carga.api.dto.guia;

import java.io.Serializable;
import java.util.List;

/** Pagina C14: estado persistido y periodo previsto, sin fuentes P3/P4 supuestas. */
public class PaginaConsultaGuiasDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ConsultaGuiaDTO> registros = List.of();
    private int pagina;
    private int tamano;
    private long total;

    public PaginaConsultaGuiasDTO() { }

    public List<ConsultaGuiaDTO> getRegistros() { return registros; }
    public void setRegistros(List<ConsultaGuiaDTO> registros) { this.registros = registros; }
    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }
    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}
