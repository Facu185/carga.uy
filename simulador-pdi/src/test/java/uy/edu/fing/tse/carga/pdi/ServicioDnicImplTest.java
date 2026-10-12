package uy.edu.fing.tse.carga.pdi;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.PersonaDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServicioDnicImplTest {

    @Test
    void devuelveDatosFicticiosParaLaCedulaDePruebaSolicitada() {
        PersonaDTO persona = new ServicioDnicImpl().obtenerPersona("63224239");

        assertNotNull(persona);
        assertEquals("63224239", persona.getCedula());
    }
}
