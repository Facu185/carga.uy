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

    private static final Map<String, PersonaDTO> PADRON = Map.of(
            "11111111", new PersonaDTO("11111111", "Ana Perez", "1985-04-12"),
            "22222222", new PersonaDTO("22222222", "Luis Gomez", "1979-09-03"),
            "33333333", new PersonaDTO("33333333", "Marta Silva", "1990-01-27"),
            "44444444", new PersonaDTO("44444444", "Jorge Rodriguez", "1968-11-08"),
            "63224239", new PersonaDTO("63224239", "Roibeth Garcia", "1990-01-01")
    );

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
