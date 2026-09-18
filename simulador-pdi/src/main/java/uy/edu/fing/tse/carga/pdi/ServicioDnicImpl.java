package uy.edu.fing.tse.carga.pdi;

import jakarta.jws.WebService;
import uy.edu.fing.tse.carga.api.dto.PersonaDTO;
import uy.edu.fing.tse.carga.api.soap.ServicioDnic;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Simula el servicio de DNIC que en produccion se consume a traves de la PDI.
 * Solo devuelve datos de un padron ficticio; no representa a personas reales.
 */
@WebService(serviceName = "ServicioDnicService", portName = "ServicioDnicPort",
        targetNamespace = "http://pdi.carga.uy/",
        endpointInterface = "uy.edu.fing.tse.carga.api.soap.ServicioDnic")
public class ServicioDnicImpl implements ServicioDnic {

    private static final Logger LOG = Logger.getLogger(ServicioDnicImpl.class.getName());

    private static final Map<String, PersonaDTO> PADRON = Map.of(
            "11111111", new PersonaDTO("11111111", "Ana Perez", "1985-04-12"),
            "22222222", new PersonaDTO("22222222", "Luis Gomez", "1979-09-03"),
            "33333333", new PersonaDTO("33333333", "Marta Silva", "1990-01-27"),
            "44444444", new PersonaDTO("44444444", "Jorge Rodriguez", "1968-11-08")
    );

    @Override
    public PersonaDTO obtenerPersona(String cedula) {
        LOG.info(() -> "Consulta a DNIC por cedula " + cedula);
        return PADRON.get(cedula == null ? "" : cedula.trim());
    }
}
