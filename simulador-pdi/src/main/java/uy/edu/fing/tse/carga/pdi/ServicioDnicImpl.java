package uy.edu.fing.tse.carga.pdi;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import uy.edu.fing.tse.carga.api.dto.comun.PersonaDTO;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;
import uy.edu.fing.tse.carga.api.observabilidad.Traza;
import uy.edu.fing.tse.carga.api.contract.soap.ServicioDnic;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simula el servicio de DNIC que en produccion se consume a traves de la PDI.
 * Solo devuelve datos de un padron ficticio; no representa a personas reales.
 *
 * Continua la traza distribuida del central: lee el header traceparent y abre su propio tramo,
 * que en Jaeger aparece como otro servicio ("simulador-pdi") dentro de la misma traza.
 */
@WebService(serviceName = "ServicioDnicService", portName = "ServicioDnicPort",
        targetNamespace = "http://pdi.carga.uy/",
        endpointInterface = "uy.edu.fing.tse.carga.api.contract.soap.ServicioDnic")
public class ServicioDnicImpl implements ServicioDnic {

    private static final Logger LOG = Logger.getLogger(ServicioDnicImpl.class.getName());

    // Incluye a todos los usuarios de docker/keycloak/gubuy-realm.json, para que el ingreso con gub.uy
    // de cualquiera de ellos complete la fecha de nacimiento (una cedula que no esta aca queda
    // "pendiente de verificacion", igual que si la PDI no respondiera)
    private static final Map<String, PersonaDTO> PADRON = Map.ofEntries(
            persona("11111111", "Ana Perez", "1985-04-12"),
            persona("22222222", "Luis Gomez", "1979-09-03"),
            persona("33333333", "Marta Silva", "1990-01-27"),
            persona("44444444", "Jorge Rodriguez", "1968-11-08"),
            persona("63224239", "Roibeth Garcia", "1990-01-01"),
            persona("41000019", "Chofer Demo Uno", "1982-03-15"),
            persona("41000025", "Chofer Demo Dos", "1987-07-22"),
            persona("41000031", "Chofer Demo Tres", "1975-11-02"),
            persona("41000047", "Chofer Demo Cuatro", "1991-05-30"),
            persona("41000053", "Chofer Demo Cinco", "1984-09-18"),
            persona("31234565", "Funcionaria Demo", "1980-02-10"),
            persona("12345672", "Ciudadana De Prueba", "1995-06-25")
    );

    private static Map.Entry<String, PersonaDTO> persona(String cedula, String nombre, String nacimiento) {
        return Map.entry(cedula, new PersonaDTO(cedula, nombre, nacimiento));
    }

    static {
        Traza.usarServicio("simulador-pdi");
    }

    @Resource
    private WebServiceContext contexto;

    @Override
    public PersonaDTO obtenerPersona(String cedula) {
        try (Traza.Tramo tramo = Traza.iniciarDesde(header(Traza.HEADER), "DNIC obtenerPersona", Traza.Tipo.SERVIDOR)
                .atributo("idCorrelacion", header(Correlacion.HEADER))) {
            LOG.info(() -> "Consulta a DNIC por cedula " + cedula);
            PersonaDTO persona = PADRON.get(cedula == null ? "" : cedula.trim());
            tramo.atributo("encontrada", persona != null);
            return persona;
        }
    }

    /** Header HTTP del pedido SOAP (null si no vino o si no hay contexto, por ejemplo en una prueba). */
    @SuppressWarnings("unchecked")
    private String header(String nombre) {
        if (contexto == null) {
            return null;
        }
        Map<String, List<String>> headers =
                (Map<String, List<String>>) contexto.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS);
        if (headers == null) {
            return null;
        }
        // Los nombres de header no distinguen mayusculas
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(nombre) && !e.getValue().isEmpty())
                .map(e -> e.getValue().get(0))
                .findFirst().orElse(null);
    }
}
