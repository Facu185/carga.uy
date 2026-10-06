package uy.edu.fing.tse.carga.web.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** Backing bean de ejemplo para las paginas JSF + PrimeFaces. */
@Named("empresaVista")
@ViewScoped
public class EmpresaVista implements Serializable {

    @EJB
    private transient EmpresaServicioLocal servicio;

    private List<EmpresaDTO> empresas;
    private EmpresaDTO nueva = new EmpresaDTO();

    @PostConstruct
    public void iniciar() {
        nueva.setFechaAlta(LocalDate.now());
        refrescar();
    }

    public void refrescar() {
        empresas = servicio.listar();
    }

    public void agregar() {
        try {
            servicio.agregar(nueva);
            nueva = new EmpresaDTO();
            nueva.setFechaAlta(LocalDate.now());
            mensaje(FacesMessage.SEVERITY_INFO, "Empresa registrada.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void mensaje(FacesMessage.Severity severidad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, null));
    }

    public List<EmpresaDTO> getEmpresas() { return empresas; }
    public EmpresaDTO getNueva() { return nueva; }
    public void setNueva(EmpresaDTO nueva) { this.nueva = nueva; }
}
