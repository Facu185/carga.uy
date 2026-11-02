package uy.edu.fing.tse.carga.api.dto.fiscalizacion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Una pagina de casos, del mas reciente al mas antiguo. */
public class PaginaCasosDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CasoDTO> registros = new ArrayList<>();
    private long total;
    private int pagina;
    private int tamano;

    public List<CasoDTO> getRegistros() { return registros; }
    public void setRegistros(List<CasoDTO> registros) { this.registros = registros; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }

}
