package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VehiculoServicioBeanSeguridadTest {

    @Test
    void declaraLosRolesDelAbmDeVehiculos() {
        DeclareRoles rolesDeclarados = VehiculoServicioBean.class.getAnnotation(DeclareRoles.class);
        RolesAllowed rolesPermitidos = VehiculoServicioBean.class.getAnnotation(RolesAllowed.class);

        assertNotNull(rolesDeclarados);
        assertNotNull(rolesPermitidos);
        assertEquals(Set.of(Roles.ADMINISTRADOR, Roles.CIUDADANO, Roles.CHOFER), Set.of(rolesPermitidos.value()));
    }
}
