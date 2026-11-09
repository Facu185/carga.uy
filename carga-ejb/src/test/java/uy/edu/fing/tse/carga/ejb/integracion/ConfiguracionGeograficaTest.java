package uy.edu.fing.tse.carga.ejb.integracion;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionGeograficaTest {
    @Test void nuncaAceptaUnEndpointDeAutomovilComoRutaPorPeso() {
        assertDoesNotThrow(() -> ConfiguracionGeografica.validarPerfilOrs(
                URI.create("https://api.heigit.org/openrouteservice/v2/directions/driving-hgv/geojson")));
        assertThrows(ServicioNoDisponibleException.class, () -> ConfiguracionGeografica.validarPerfilOrs(
                URI.create("https://api.heigit.org/openrouteservice/v2/directions/driving-car/geojson")));
    }
}
