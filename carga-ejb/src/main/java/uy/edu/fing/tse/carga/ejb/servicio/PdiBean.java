package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.ejb.Stateless;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import uy.edu.fing.tse.carga.api.PdiLocal;
import uy.edu.fing.tse.carga.api.dto.PersonaDTO;
import uy.edu.fing.tse.carga.api.soap.ServicioDnic;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Cliente SOAP del servicio de DNIC publicado a traves de la PDI
 * (aca, del modulo simulador-pdi). La comunicacion con la PDI debe ser SOAP
 * segun la Seccion 4.1 de la letra.
 *
 * TODO (frente P1): si la PDI no responde el ingreso debe completarse igual con los
 * datos minimos (flujo alternativo 7a del caso de uso de autenticacion).
 */
@Stateless
public class PdiBean implements PdiLocal {

    private static final Logger LOG = Logger.getLogger(PdiBean.class.getName());

    private static final String WSDL = System.getenv().getOrDefault(
            "SIMULADOR_PDI_URL", "http://localhost:8080/simulador-pdi/ServicioDnicImpl?wsdl");
    private static final QName SERVICIO = new QName("http://pdi.carga.uy/", "ServicioDnicService");

    @Override
    public PersonaDTO obtenerPersona(String cedula) {
        try {
            ServicioDnic puerto = Service.create(URI.create(WSDL).toURL(), SERVICIO).getPort(ServicioDnic.class);
            Map<String, Object> ctx = ((BindingProvider) puerto).getRequestContext();
            ctx.put("jakarta.xml.ws.client.connectTimeout", 3000);
            ctx.put("jakarta.xml.ws.client.receiveTimeout", 3000);
            return puerto.obtenerPersona(cedula);
        } catch (Exception e) {
            LOG.warning("La PDI no respondio: " + e.getMessage());
            return null;
        }
    }
}
