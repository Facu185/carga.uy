package uy.edu.fing.tse.carga.web.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import uy.edu.fing.tse.carga.api.contract.service.ChoferServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.SolicitudesEmpresaLocal;
import uy.edu.fing.tse.carga.api.dto.empresa.VinculoEmpresaDTO;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.ChoferDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Named("choferVista")
@ViewScoped
public class ChoferVista implements Serializable {

    @EJB
    private transient ChoferServicioLocal servicio;

    @EJB
    private transient EmpresaServicioLocal empresaServicio;

    @EJB
    private transient SolicitudesEmpresaLocal vinculosServicio;

    @Inject
    private transient SecurityContext seguridad;

    private List<ChoferDTO> choferes = List.of();
    private List<EmpresaDTO> empresas = List.of();
    private Long empresaConsultaId;
    private ChoferDTO nuevo = new ChoferDTO();

    @PostConstruct
    public void iniciar() {
        refrescar();
    }

    public void refrescar() {
        boolean administrador = seguridad.isCallerInRole(Roles.ADMINISTRADOR);
        empresas = empresaServicio.listar().stream().filter(EmpresaDTO::isActiva).toList();
        if (!administrador) {
            Set<Long> propias = vinculosServicio.misVinculos().stream()
                    .filter(v -> "RESPONSABLE".equals(v.getTipo()) && "ACTIVO".equals(v.getEstado()))
                    .map(VinculoEmpresaDTO::getEmpresaId).collect(Collectors.toSet());
            empresas = empresas.stream().filter(e -> propias.contains(e.getId())).toList();
            if (empresaConsultaId == null && !empresas.isEmpty()) empresaConsultaId = empresas.getFirst().getId();
        }
        nuevo.setEmpresaId(empresaConsultaId);
        choferes = List.of();
        if ((!administrador && empresaConsultaId == null)
                || (empresaConsultaId != null && empresas.stream().noneMatch(e -> e.getId().equals(empresaConsultaId)))) return;
        try {
            // La vista coordina el filtro; el EJB vuelve a comprobar pertenencia y estado del vinculo.
            choferes = servicio.listar(empresaConsultaId);
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
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
    public Long getEmpresaConsultaId() { return empresaConsultaId; }
    public void setEmpresaConsultaId(Long empresaConsultaId) { this.empresaConsultaId = empresaConsultaId; }
    public ChoferDTO getNuevo() { return nuevo; }
    public void setNuevo(ChoferDTO nuevo) { this.nuevo = nuevo; }
}
