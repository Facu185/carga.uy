package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CapacidadGuiaTest {

    @Test
    void aceptaCargaEnLosLimitesDePesoYVolumen() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setCapacidadCargaKg(1000);
        vehiculo.setCapacidadVolumenM3(new BigDecimal("12.50"));

        assertDoesNotThrow(() -> Guia.validarCapacidades(vehiculo, new BigDecimal("1000"), new BigDecimal("12.50")));
    }

    @Test
    void rechazaPesoQueSuperaLaCapacidadDelVehiculo() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setCapacidadCargaKg(1000);
        vehiculo.setCapacidadVolumenM3(new BigDecimal("12.50"));

        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarCapacidades(vehiculo, new BigDecimal("1000.01"), new BigDecimal("12.50")));
    }

    @Test
    void rechazaVolumenQueSuperaLaCapacidadDelVehiculo() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setCapacidadCargaKg(1000);
        vehiculo.setCapacidadVolumenM3(new BigDecimal("12.50"));

        assertThrows(IllegalArgumentException.class,
                () -> Guia.validarCapacidades(vehiculo, new BigDecimal("1000"), new BigDecimal("12.51")));
    }

    @Test
    void rechazaAsignacionCuandoLaCapacidadVolumetricaNoEstaConfigurada() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setCapacidadCargaKg(1000);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Guia.validarCapacidades(vehiculo, new BigDecimal("1000"), new BigDecimal("1.00")));

        assertEquals("La capacidad volumetrica del vehiculo no esta configurada.", error.getMessage());
    }
}
