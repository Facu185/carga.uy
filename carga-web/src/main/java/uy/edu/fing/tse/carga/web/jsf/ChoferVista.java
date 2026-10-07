package uy.edu.fing.tse.carga.web.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import uy.edu.fing.tse.carga.api.contract.service.ChoferServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.ChoferDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.io.Serializable;
import java.util.List;

@Named("choferVista")
@ViewScoped
public class ChoferVista implements Serializable {

    @EJB
    private transient ChoferServicioLocal servicio;

    @EJB
    private transient EmpresaServicioLocal empresaServicio;

    private List<ChoferDTO> choferes;
    private List<EmpresaDTO> empresas;
    private ChoferDTO nuevo = new ChoferDTO();

    @PostConstruct
    public void iniciar() {
        refrescar();
    }

    public void refrescar() {
        choferes = servicio.listar(null);
        empresas = empresaServicio.listar().stream().filter(EmpresaDTO::isActiva).toList();
    }

    public void agregar() {
        try {
            servicio.agregar(nuevo);
            nuevo = new ChoferDTO();
            mensaje(FacesMessage.SEVERITY_INFO, "Chofer registrado.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void modificar(ChoferDTO chofer) {
        try {
            servicio.modificar(chofer.getId(), chofer);
            mensaje(FacesMessage.SEVERITY_INFO, "Chofer actualizado.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void darDeAlta(ChoferDTO chofer) {
        try {
            servicio.darDeAlta(chofer.getId());
            mensaje(FacesMessage.SEVERITY_INFO, "Chofer dado de alta.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void darDeBaja(ChoferDTO chofer) {
        try {
            servicio.darDeBaja(chofer.getId());
            mensaje(FacesMessage.SEVERITY_INFO, "Chofer dado de baja.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void mensaje(FacesMessage.Severity severidad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, null));
    }

    public List<ChoferDTO> getChoferes() { return choferes; }
    public List<EmpresaDTO> getEmpresas() { return empresas; }
    public ChoferDTO getNuevo() { return nuevo; }
    public void setNuevo(ChoferDTO nuevo) { this.nuevo = nuevo; }
}
