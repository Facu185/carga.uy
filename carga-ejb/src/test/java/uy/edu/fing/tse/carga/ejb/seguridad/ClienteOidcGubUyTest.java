package uy.edu.fing.tse.carga.ejb.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteOidcGubUyTest {

    @Test
    void desafioPkceCoincideConElEjemploDelRfc7636() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
                ClienteOidcGubUy.desafioPkce("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }

    @Test
    void aleatorioEsLargoYNoSeRepite() {
        String a = ClienteOidcGubUy.aleatorio();
        assertTrue(a.length() >= 43);
        assertNotEquals(a, ClienteOidcGubUy.aleatorio());
    }
}
