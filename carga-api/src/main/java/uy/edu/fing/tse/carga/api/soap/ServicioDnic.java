package uy.edu.fing.tse.carga.api.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import uy.edu.fing.tse.carga.api.dto.PersonaDTO;

/**
 * Contrato SOAP del Servicio Basico de Informacion de DNIC.
 * Los servicios de la PDI no son publicos, asi que el modulo simulador-pdi
 * implementa esta misma interfaz (Seccion 3.7.1 de la letra).
 */
@WebService(name = "ServicioDnic", targetNamespace = "http://pdi.carga.uy/")
public interface ServicioDnic {

    @WebMethod
    PersonaDTO obtenerPersona(@WebParam(name = "cedula") String cedula);
}
