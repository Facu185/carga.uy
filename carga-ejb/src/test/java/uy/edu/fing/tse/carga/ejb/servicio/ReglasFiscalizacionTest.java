package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.ejb.servicio.ReglasFiscalizacion.Habilitacion;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasFiscalizacionTest {

    private static final LocalDate DIA = LocalDate.parse("2026-10-15");

    @Test
    void elExcesoSeCuentaSobrePesoMasCapacidadMasTolerancia() {
        // maximo 9.000 + 21.000 = 30.000 kg, tolerancia 500
        assertTrue(ReglasFiscalizacion.exceso(30_500, 9000, 21000, 500).isEmpty());
        String evidencia = ReglasFiscalizacion.exceso(30_501, 9000, 21000, 500).orElseThrow();
        assertTrue(evidencia.contains("30.000 kg"), evidencia);
        assertTrue(evidencia.contains("Exceso: 501 kg"), evidencia);
    }

    @Test
    void unVehiculoAlDiaEstaHabilitado() {
        assertTrue(ReglasFiscalizacion.sinHabilitacion(habilitacion("2026-12-31", "2027-01-31"), DIA).isEmpty());
    }

    @Test
    void permisoOItvVencidosAlDiaDeLaActividadNoEstanHabilitados() {
        String permiso = ReglasFiscalizacion.sinHabilitacion(habilitacion("2026-10-14", "2027-01-31"), DIA).orElseThrow();
        assertTrue(permiso.contains("Permiso Nacional de Circulación PNC-1 no estaba vigente"), permiso);
        String itv = ReglasFiscalizacion.sinHabilitacion(habilitacion("2026-12-31", "2026-10-14"), DIA).orElseThrow();
        assertTrue(itv.contains("venció el 2026-10-14"), itv);
        // el dia de vencimiento todavia vale
        assertTrue(ReglasFiscalizacion.sinHabilitacion(habilitacion("2026-10-15", "2026-10-15"), DIA).isEmpty());
    }

    @Test
    void sinDatosDePermisoOConElVehiculoDeBajaTampoco() {
        Habilitacion sinPermiso = new Habilitacion(true, true, null, null, null, LocalDate.parse("2027-01-31"));
        assertTrue(ReglasFiscalizacion.sinHabilitacion(sinPermiso, DIA).orElseThrow().contains("no tiene Permiso"));
        Habilitacion deBaja = new Habilitacion(false, true, "PNC-1", LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"), LocalDate.parse("2027-01-31"));
        assertTrue(ReglasFiscalizacion.sinHabilitacion(deBaja, DIA).orElseThrow().contains("dado de baja"));
    }

    @Test
    void elCorredorEsElMayorEntreElAbsolutoYElRelativo() {
        // viaje corto: manda el absoluto (5 km); viaje de 200 km: el 15 % (30 km)
        assertEquals(5000, ReglasFiscalizacion.corredorMetros(20_000, 5000, 15));
        assertEquals(30_000, ReglasFiscalizacion.corredorMetros(200_000, 5000, 15));
    }

    @Test
    void hayDesvioSoloSiMasDelPorcentajeDePosicionesQuedanFuera() {
        assertTrue(ReglasFiscalizacion.desvio(200_000, 30_000, 100, 20, 45_000, 20).isEmpty());
        String evidencia = ReglasFiscalizacion.desvio(200_000, 30_000, 100, 21, 45_000, 20).orElseThrow();
        assertTrue(evidencia.contains("21 de 100 posiciones"), evidencia);
    }

    @Test
    void conPocasPosicionesNoSeConcluyeDesvio() {
        // 4 posiciones, todas fuera: puede ser ruido del GPS o un viaje sin tracking
        assertTrue(ReglasFiscalizacion.desvio(200_000, 30_000, 4, 4, 90_000, 20).isEmpty());
    }

    private static Habilitacion habilitacion(String permisoHasta, String itv) {
        return new Habilitacion(true, true, "PNC-1", LocalDate.parse("2026-01-01"), LocalDate.parse(permisoHasta),
                LocalDate.parse(itv));
    }
}
