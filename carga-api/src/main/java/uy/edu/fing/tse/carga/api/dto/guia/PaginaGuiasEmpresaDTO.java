package uy.edu.fing.tse.carga.api.dto.guia;

import java.io.Serializable;
import java.util.List;

/** Pagina de guias de una empresa autorizada; pagina empieza en cero. */
public class PaginaGuiasEmpresaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<GuiaEmpresaDTO> registros = List.of();
    private int pagina;
    private int tamano;
    private long total;

    public PaginaGuiasEmpresaDTO() { }

    public List<GuiaEmpresaDTO> getRegistros() { return registros; }
    public void setRegistros(List<GuiaEmpresaDTO> registros) { this.registros = registros; }
    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }
    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}
