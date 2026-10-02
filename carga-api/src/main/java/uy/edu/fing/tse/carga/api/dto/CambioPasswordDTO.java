package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

public class CambioPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String actual;
    private String nueva;

    public CambioPasswordDTO() {
    }

    public CambioPasswordDTO(String actual, String nueva) {
        this.actual = actual;
        this.nueva = nueva;
    }

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    public String getNueva() { return nueva; }
    public void setNueva(String nueva) { this.nueva = nueva; }

}
