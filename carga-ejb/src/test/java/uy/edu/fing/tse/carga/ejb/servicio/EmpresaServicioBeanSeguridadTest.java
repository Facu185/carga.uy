package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.comun.EmpresaDTO;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmpresaServicioBeanSeguridadTest {

    private static final Set<String> ROLES_DE_LECTURA = Set.of(
            Roles.CIUDADANO, Roles.CHOFER, Roles.FUNCIONARIO, Roles.AUTORIDAD, Roles.ADMINISTRADOR);

    @Test
    void permiteLeerEmpresasAUsuariosAutenticados() throws NoSuchMethodException {
        assertRolesDeLectura(EmpresaServicioBean.class.getMethod("listar"));
        assertRolesDeLectura(EmpresaServicioBean.class.getMethod("buscarPorNombre", String.class));
    }

    @Test
    void mantieneMutacionesSoloParaFuncionarioYAdministrador() throws NoSuchMethodException {
        for (var metodo : new java.lang.reflect.Method[]{
                EmpresaServicioBean.class.getMethod("agregar", EmpresaDTO.class),
                EmpresaServicioBean.class.getMethod("modificar", Long.class, EmpresaDTO.class),
                EmpresaServicioBean.class.getMethod("darDeBaja", Long.class)}) {
            RolesAllowed roles = metodo.getAnnotation(RolesAllowed.class);
            assertNotNull(roles);
            assertEquals(Set.of(Roles.FUNCIONARIO, Roles.ADMINISTRADOR), Set.of(roles.value()));
        }
    }

    private static void assertRolesDeLectura(java.lang.reflect.Method metodo) {
        RolesAllowed roles = metodo.getAnnotation(RolesAllowed.class);
        assertNotNull(roles, () -> metodo.getName() + " debe declarar los roles autorizados");
        assertEquals(ROLES_DE_LECTURA, Set.of(roles.value()));
    }
}
