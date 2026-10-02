package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {

    @Test
    void asignarRolNoLoDuplica() {
        Usuario u = new Usuario("12345672", "Ana Perez", null);
        u.asignarRol(new RolCatalogo("FUNCIONARIO"));
        u.asignarRol(new RolCatalogo("FUNCIONARIO"));
        assertTrue(u.tieneRol(Rol.FUNCIONARIO));
        assertEquals(1, u.getRoles().size());
    }

    @Test
    void quitarRolLoSaca() {
        Usuario u = new Usuario("12345672", "Ana Perez", null);
        u.asignarRol(new RolCatalogo("CIUDADANO"));
        u.quitarRol(Rol.CIUDADANO);
        assertFalse(u.tieneRol(Rol.CIUDADANO));
    }

    @Test
    void sincronizarActualizaLoQueCambioEnGubUy() {
        Usuario u = new Usuario("12345672", "Ana Perez", "vieja@example.com");
        u.sincronizarDatos("Ana M. Perez", "nueva@example.com");
        assertEquals("Ana M. Perez", u.getNombre());
        assertEquals("nueva@example.com", u.getCorreo());
    }

    @Test
    void sincronizarIgnoraDatosVacios() {
        Usuario u = new Usuario("12345672", "Ana Perez", "ana@example.com");
        u.sincronizarDatos("  ", null);
        assertEquals("Ana Perez", u.getNombre());
        assertEquals("ana@example.com", u.getCorreo());
    }
}
