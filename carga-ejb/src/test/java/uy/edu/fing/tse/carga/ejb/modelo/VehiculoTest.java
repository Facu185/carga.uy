package uy.edu.fing.tse.carga.ejb.modelo;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ejemplo de prueba unitaria: la letra pide ~80% de cobertura (Seccion 4.5). */
class VehiculoTest {

    private Vehiculo vehiculoCon(LocalDate permisoDesde, LocalDate permisoHasta, LocalDate itv) {
        Vehiculo v = new Vehiculo();
        v.setMatricula("STZ1234");
        v.setPesoKg(9000);
        v.setCapacidadCargaKg(21000);
        v.setPermisoDesde(permisoDesde);
        v.setPermisoHasta(permisoHasta);
        v.setItvVencimiento(itv);
        return v;
    }

    @Test
    void habilitadoCuandoPermisoEItvEstanVigentes() {
        Vehiculo v = vehiculoCon(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 11, 30));
        assertTrue(v.habilitadoEn(LocalDate.of(2026, 6, 1)));
    }

    @Test
    void noHabilitadoConItvVencida() {
        Vehiculo v = vehiculoCon(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 5, 31));
        assertFalse(v.habilitadoEn(LocalDate.of(2026, 6, 1)));
    }

    @Test
    void noHabilitadoFueraDelPeriodoDelPermiso() {
        Vehiculo v = vehiculoCon(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31), LocalDate.of(2026, 12, 31));
        assertFalse(v.habilitadoEn(LocalDate.of(2026, 6, 1)));
    }

    @Test
    void pesoMaximoEsPesoMasCapacidad() {
        Vehiculo v = vehiculoCon(LocalDate.now(), LocalDate.now(), LocalDate.now());
        assertEquals(30000, v.pesoMaximoAdmitidoKg());
    }

    @Test
    void exponeCapacidadDeVolumenEnMetrosCubicos() {
        Vehiculo v = vehiculoCon(LocalDate.now(), LocalDate.now(), LocalDate.now());
        v.setCapacidadVolumenM3(new BigDecimal("45.50"));

        assertEquals(new BigDecimal("45.50"), v.getCapacidadVolumenM3());
    }
}
