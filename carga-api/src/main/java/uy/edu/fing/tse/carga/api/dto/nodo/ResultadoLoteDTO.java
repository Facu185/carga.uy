package uy.edu.fing.tse.carga.api.dto.nodo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ResultadoLoteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int recibidas;
    private int aceptadas;
    private List<Rechazo> rechazadas = new ArrayList<>();

    public ResultadoLoteDTO() {
    }

    public void agregarRechazo(int indice, String idEvento, String motivo) {
        rechazadas.add(new Rechazo(indice, idEvento, motivo));
    }

    public int getRecibidas() { return recibidas; }
    public void setRecibidas(int recibidas) { this.recibidas = recibidas; }

    public int getAceptadas() { return aceptadas; }
    public void setAceptadas(int aceptadas) { this.aceptadas = aceptadas; }

    public List<Rechazo> getRechazadas() { return rechazadas; }
    public void setRechazadas(List<Rechazo> rechazadas) { this.rechazadas = rechazadas; }

    public static class Rechazo implements Serializable {

        private static final long serialVersionUID = 1L;
        private int indice;
        private String idEvento;
        private String motivo;

        public Rechazo() {
        }

        public Rechazo(int indice, String idEvento, String motivo) {
            this.indice = indice;
            this.idEvento = idEvento;
            this.motivo = motivo;
        }

        public int getIndice() { return indice; }
        public void setIndice(int indice) { this.indice = indice; }

        public String getIdEvento() { return idEvento; }
        public void setIdEvento(String idEvento) { this.idEvento = idEvento; }

        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }
}
