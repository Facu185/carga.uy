package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/**
 * Una pagina de la pista de auditoria. pagina empieza en 0; total es la cantidad de registros del filtro.
 */
public class PaginaAuditoriaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private java.util.List<RegistroAuditoriaDTO> registros;
    private int pagina;
    private int tamano;
    private long total;

    public PaginaAuditoriaDTO() {
    }

    public PaginaAuditoriaDTO(java.util.List<RegistroAuditoriaDTO> registros, int pagina, int tamano, long total) {
        this.registros = registros;
        this.pagina = pagina;
        this.tamano = tamano;
        this.total = total;
    }

    public java.util.List<RegistroAuditoriaDTO> getRegistros() { return registros; }
    public void setRegistros(java.util.List<RegistroAuditoriaDTO> registros) { this.registros = registros; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

}
