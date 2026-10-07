package uy.edu.fing.tse.carga.web.soap;

import jakarta.ejb.EJB;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import uy.edu.fing.tse.carga.api.contract.service.EmpresaServicioLocal;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;

import java.util.List;

/**
 * Endpoint SOAP de ejemplo. La letra pide exponer servicios tanto REST como SOAP
 * (Seccion 4.1); este modulo muestra como publicar los dos sobre el mismo EJB.
 */
@WebService(serviceName = "EmpresaService", portName = "EmpresaPort",
        targetNamespace = "http://web.carga.uy/")
public class EmpresaSoap {

    @EJB
    private EmpresaServicioLocal servicio;

    @WebMethod
    public List<EmpresaDTO> listarEmpresas() {
        return servicio.listar();
    }

    @WebMethod
    public List<EmpresaDTO> buscarEmpresas(@WebParam(name = "nombre") String nombre) {
        return servicio.buscarPorNombre(nombre);
    }
}
