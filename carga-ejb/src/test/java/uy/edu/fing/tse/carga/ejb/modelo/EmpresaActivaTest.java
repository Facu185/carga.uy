package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpresaActivaTest {

    @Test
    void lasEmpresasNuevasSonActivasHastaQueSeDaLaBajaLogica() {
        Empresa empresa = new Empresa();

        assertTrue(empresa.isActiva());
        empresa.setActiva(false);
        assertFalse(empresa.isActiva());
    }
}
