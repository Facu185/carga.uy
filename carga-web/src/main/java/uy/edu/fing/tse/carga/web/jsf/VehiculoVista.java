package uy.edu.fing.tse.carga.web.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.VehiculoServicioLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.io.Serializable;
import java.util.List;

@Named("vehiculoVista")
@ViewScoped
public class VehiculoVista implements Serializable {

    @EJB
    private transient VehiculoServicioLocal servicio;

    @EJB
    private transient EmpresaServicioLocal empresaServicio;

    private List<VehiculoDTO> vehiculos;
    private List<EmpresaDTO> empresas;
    private VehiculoDTO nuevo = new VehiculoDTO();

    @PostConstruct
    public void iniciar() {
        refrescar();
    }

    public void refrescar() {
        vehiculos = servicio.listar(null);
        empresas = empresaServicio.listar().stream().filter(EmpresaDTO::isActiva).toList();
    }

    public void agregar() {
        try {
            servicio.agregar(nuevo);
            nuevo = new VehiculoDTO();
            mensaje(FacesMessage.SEVERITY_INFO, "Vehiculo registrado.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void modificar(VehiculoDTO vehiculo) {
        try {
            servicio.modificar(vehiculo.getId(), vehiculo);
            mensaje(FacesMessage.SEVERITY_INFO, "Vehiculo actualizado.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void darDeBaja(VehiculoDTO vehiculo) {
        try {
            servicio.darDeBaja(vehiculo.getId());
            mensaje(FacesMessage.SEVERITY_INFO, "Vehiculo dado de baja.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void mensaje(FacesMessage.Severity severidad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, null));
    }

    public List<VehiculoDTO> getVehiculos() { return vehiculos; }
    public List<EmpresaDTO> getEmpresas() { return empresas; }
    public VehiculoDTO getNuevo() { return nuevo; }
    public void setNuevo(VehiculoDTO nuevo) { this.nuevo = nuevo; }
}
