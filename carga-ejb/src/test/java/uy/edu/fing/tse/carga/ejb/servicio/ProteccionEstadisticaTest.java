package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.publico.CifraPublicaDTO;
import uy.edu.fing.tse.carga.ejb.servicio.ProteccionEstadistica.Celda;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProteccionEstadisticaTest {

    @Test
    void publicaSoloConCincoGuiasDeTresEmpresasOCero() {
        assertTrue(ProteccionEstadistica.publicable(5, Set.of(1L, 2L, 3L)));
        assertTrue(ProteccionEstadistica.publicable(0, Set.of()));
        assertFalse(ProteccionEstadistica.publicable(4, Set.of(1L, 2L, 3L)));
        // muchas guias pero de dos empresas: cada una despeja la cifra de la otra
        assertFalse(ProteccionEstadistica.publicable(50, Set.of(1L, 2L)));
    }

    @Test
    void lasCategoriasChicasSeJuntanEnOtrosSiJuntasCumplen() {
        List<CifraPublicaDTO> r = ProteccionEstadistica.categorias(List.of(
                celda("1", 20, 1, 2, 3, 4), celda("2", 3, 1, 2), celda("3", 3, 3, 4)), "Otros rubros");

        assertEquals(List.of("1", "OTROS"), r.stream().map(CifraPublicaDTO::getClave).toList());
        CifraPublicaDTO otros = r.get(1);
        assertFalse(otros.isReservada());
        assertEquals(6L, otros.getGuias());
    }

    @Test
    void siSoloSeReservaUnaCategoriaSeSumaLaMasChicaParaQueNoSeDespejePorResta() {
        // sin la supresion secundaria, "Otros" seria exactamente la categoria 3 y el total la revelaria
        List<CifraPublicaDTO> r = ProteccionEstadistica.categorias(List.of(
                celda("1", 30, 1, 2, 3, 4), celda("2", 8, 1, 2, 3), celda("3", 2, 5)), "Otros");

        assertEquals(List.of("1", "OTROS"), r.stream().map(CifraPublicaDTO::getClave).toList());
        assertEquals(10L, r.get(1).getGuias());
    }

    @Test
    void siNiTodoJuntoCumpleNoSePublicaNada() {
        List<CifraPublicaDTO> r = ProteccionEstadistica.categorias(List.of(
                celda("1", 3, 1), celda("2", 2, 2)), "Otros");

        assertEquals(1, r.size());
        assertTrue(r.get(0).isReservada());
        assertNull(r.get(0).getGuias());
    }

    @Test
    void enLaSerieUnMesChicoArrastraAlMenorParaQueElTotalNoLoRevele() {
        List<CifraPublicaDTO> r = ProteccionEstadistica.serie(List.of(
                celda("2026-01", 12, 1, 2, 3), celda("2026-02", 0), celda("2026-03", 2, 1), celda("2026-04", 6, 1, 2, 4)));

        assertFalse(r.get(0).isReservada());
        assertFalse(r.get(1).isReservada());
        assertEquals(0L, r.get(1).getGuias());
        assertTrue(r.get(2).isReservada());
        // el menor publicable se reserva tambien: dos celdas reservadas que juntas cumplen el umbral
        assertTrue(r.get(3).isReservada());
    }

    @Test
    void redondeaLasToneladasAUnDecimal() {
        CifraPublicaDTO r = ProteccionEstadistica.categorias(List.of(
                new Celda("1", "Granos", 5, Set.of(1L, 2L, 3L), 123_456)), "Otros").get(0);
        assertEquals(123.5, r.getToneladas());
    }

    @Test
    void unTotalSueltoSoloSiCumple() {
        assertEquals(7L, ProteccionEstadistica.total(7, Set.of(1L, 2L, 3L), 5));
        assertNull(ProteccionEstadistica.total(7, Set.of(1L, 2L), 5));
        assertNull(ProteccionEstadistica.total(4, Set.of(1L, 2L, 3L), 5));
        assertEquals(0L, ProteccionEstadistica.total(0, Set.of(), 5));
    }

    private static Celda celda(String clave, long guias, long... empresas) {
        return new Celda(clave, "Etiqueta " + clave, guias,
                java.util.Arrays.stream(empresas).boxed().collect(java.util.stream.Collectors.toSet()), guias * 1000);
    }
}
