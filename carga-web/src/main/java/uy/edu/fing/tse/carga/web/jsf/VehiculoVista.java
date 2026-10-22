package uy.edu.fing.tse.carga.web.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.contract.service.SolicitudesEmpresaLocal;
import uy.edu.fing.tse.carga.api.contract.service.VehiculoServicioLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.empresa.VinculoEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.VehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Named("vehiculoVista")
@ViewScoped
public class VehiculoVista implements Serializable {
    private static final Logger LOG = Logger.getLogger(VehiculoVista.class.getName());

    @EJB
    private transient VehiculoServicioLocal servicio;

    @EJB
    private transient EmpresaServicioLocal empresaServicio;

    @EJB
    private transient SolicitudesEmpresaLocal vinculosServicio;

    @Inject
    private transient SecurityContext seguridad;

    private List<VehiculoDTO> vehiculos = List.of();
    private List<EmpresaDTO> empresas = List.of();
    private Long empresaConsultaId;
    private VehiculoDTO nuevo = new VehiculoDTO();

    @PostConstruct
    public void iniciar() {
        refrescar();
    }

    public void refrescar() {
        vehiculos = List.of();
        empresas = List.of();
        nuevo.setEmpresaId(null);
        try {
            boolean administrador = seguridad.isCallerInRole(Roles.ADMINISTRADOR);
            empresas = empresaServicio.listar().stream().filter(EmpresaDTO::isActiva).toList();
            if (!administrador) {
                Set<Long> propias = vinculosServicio.misVinculos().stream()
                        .filter(v -> "RESPONSABLE".equals(v.getTipo()) && "ACTIVO".equals(v.getEstado()))
                        .map(VinculoEmpresaDTO::getEmpresaId).collect(Collectors.toSet());
                empresas = empresas.stream().filter(e -> propias.contains(e.getId())).toList();
                if (empresaConsultaId == null && !empresas.isEmpty()) empresaConsultaId = empresas.getFirst().getId();
            }
            if ((!administrador && empresaConsultaId == null)
                    || (empresaConsultaId != null && empresas.stream().noneMatch(e -> e.getId().equals(empresaConsultaId)))) return;
            nuevo.setEmpresaId(empresaConsultaId);
            // El filtro coordina la vista; el EJB comprueba nuevamente la autorizacion empresarial.
            vehiculos = servicio.listar(empresaConsultaId);
        } catch (EJBAccessException e) {
            accesoDenegado("consultar");
        }
    }

    public void agregar() {
        try {
            servicio.agregar(nuevo);
            nuevo = new VehiculoDTO();
            mensaje(FacesMessage.SEVERITY_INFO, "Vehiculo registrado.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (EJBAccessException e) {
            accesoDenegado("registrar");
        }
    }

    public void modificar(VehiculoDTO vehiculo) {
        try {
            servicio.modificar(vehiculo.getId(), vehiculo);
            mensaje(FacesMessage.SEVERITY_INFO, "Vehiculo actualizado.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (EJBAccessException e) {
            accesoDenegado("modificar");
        }
    }

    public void darDeBaja(VehiculoDTO vehiculo) {
        try {
            servicio.darDeBaja(vehiculo.getId());
            mensaje(FacesMessage.SEVERITY_INFO, "Vehiculo dado de baja.");
            refrescar();
        } catch (NegocioException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (EJBAccessException e) {
            accesoDenegado("dar de baja");
        }
    }

    private void accesoDenegado(String operacion) {
        vehiculos = List.of();
        empresas = List.of();
        empresaConsultaId = null;
        nuevo = new VehiculoDTO();
        LOG.warning("Acceso denegado al " + operacion + " vehiculos en JSF.");
        mensaje(FacesMessage.SEVERITY_ERROR, "No tienes permiso para gestionar estos vehiculos. Revisa tu sesion y tus vinculos empresariales.");
    }

    private void mensaje(FacesMessage.Severity severidad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, null));
    }

    public List<VehiculoDTO> getVehiculos() { return vehiculos; }
    public List<EmpresaDTO> getEmpresas() { return empresas; }
    public Long getEmpresaConsultaId() { return empresaConsultaId; }
    public void setEmpresaConsultaId(Long empresaConsultaId) { this.empresaConsultaId = empresaConsultaId; }
    public VehiculoDTO getNuevo() { return nuevo; }
    public void setNuevo(VehiculoDTO nuevo) { this.nuevo = nuevo; }
}
