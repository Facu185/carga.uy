package uy.edu.fing.tse.carga.web.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import uy.edu.fing.tse.carga.api.contract.service.MonitorLocal;
import uy.edu.fing.tse.carga.api.contract.service.SincronizacionPesajesLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.EstadoNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.MetricasDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.ResultadoSincronizacionDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.time.LocalDate;
import java.util.List;

/**
 * Backing bean del panel de operacion de nodos perifericos (frente P4).
 *
 * Por pedido y no por vista: la pagina es sin estado (f:view transient) para que no expire tras
 * un reinicio del central. No hace falta recordar nada entre pedidos: los contadores se recalculan
 * en cada refresco y la sincronizacion se resuelve en el mismo pedido del boton.
 */
@Named("panelVista")
@RequestScoped
public class PanelVista {

    @EJB
    private MonitorLocal monitor;

    @EJB
    private SincronizacionPesajesLocal sincronizacion;

    private MetricasDTO metricas;
    private List<EstadoNodoDTO> nodos;

    private String matricula = "STZ1234";
    private String fecha = LocalDate.now().toString();
    private ResultadoSincronizacionDTO resultado;
    private List<PesadaRegistradaDTO> registradas = List.of();
    private String error;

    @PostConstruct
    public void iniciar() {
        refrescar();
    }

    /** Se llama en cada pedido, incluido el auto-refresco de la pagina. */
    public void refrescar() {
        metricas = monitor.metricas();
        nodos = monitor.estadoNodos();
    }

    /**
     * Caso de uso critico "Obtener pesajes del nodo Balanzas", disparado a mano. Despues muestra lo
     * que quedo guardado: con el nodo caido igual se ven las pesadas de sincronizaciones anteriores.
     */
    public void sincronizar() {
        error = null;
        resultado = null;
        try {
            resultado = sincronizacion.sincronizar(matricula, fecha);
            registradas = sincronizacion.registradas(matricula, fecha);
        } catch (NegocioException e) {
            error = e.getMessage();
            registradas = List.of();
        }
    }

    public MetricasDTO getMetricas() { return metricas; }
    public List<EstadoNodoDTO> getNodos() { return nodos; }
    public ResultadoSincronizacionDTO getResultado() { return resultado; }
    public List<PesadaRegistradaDTO> getRegistradas() { return registradas; }
    public String getError() { return error; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
}
