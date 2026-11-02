package uy.edu.fing.tse.carga.ejb.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;
import uy.edu.fing.tse.carga.ejb.modelo.CasoFiscalizacion;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoCaso;
import uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion;
import uy.edu.fing.tse.carga.ejb.observabilidad.Metricas;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO.Actividad;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO.Recorrido;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeteccionBeanTest {

    private CasosEnMemoria casos;
    private Falsos.AuditoriaFalsa auditoria;
    private DeteccionBean deteccion;

    @BeforeEach
    void preparar() {
        casos = new CasosEnMemoria();
        auditoria = new Falsos.AuditoriaFalsa();
        deteccion = new DeteccionBean();
        deteccion.dao = casos;
        deteccion.auditoria = auditoria;
        deteccion.metricas = new Metricas();
        // al dia en la fecha de las pesadas de prueba (octubre de 2026)
        casos.vehiculos.put("STZ1234", CasosEnMemoria.camion("STZ1234", 1, "2026-12-31", "2027-03-31"));
        casos.estadosGuia.put(77L, "EN_CURSO");
    }

    @Test
    void unaPesadaConExcesoCreaElCasoLoNotificaYLoAudita() {
        assertEquals(1, deteccion.evaluarPesada(pesada("P-1", 31_000, 77L)));

        CasoFiscalizacion c = casos.porHecho.get("EXCESO_CARGA:pesada:P-1");
        assertEquals(TipoInfraccion.EXCESO_CARGA, c.getTipo());
        assertEquals(EstadoCaso.NOTIFICADA, c.getEstado());
        assertNotNull(c.getVenceDescargo());
        assertTrue(c.getEvidencia().contains("Exceso: 1.000 kg"), c.getEvidencia());
        assertEquals(1, casos.notificaciones.size());
        assertEquals(List.of(AccionAuditada.CASO_CREADO, AccionAuditada.CASO_CAMBIO_ESTADO), auditoria.acciones());
        assertEquals(DeteccionBean.USUARIO_SISTEMA, auditoria.ultimo().usuario());
    }

    @Test
    void reprocesarLaMismaPesadaNoDuplicaElCaso() {
        deteccion.evaluarPesada(pesada("P-1", 31_000, 77L));

        assertEquals(0, deteccion.evaluarPesada(pesada("P-1", 31_000, 77L)));
        assertEquals(1, casos.porHecho.size());
        assertEquals(1, casos.notificaciones.size());
    }

    @Test
    void dentroDeLaToleranciaNoHayCaso() {
        assertEquals(0, deteccion.evaluarPesada(pesada("P-1", 30_400, 77L)));
        assertTrue(casos.porHecho.isEmpty());
    }

    @Test
    void cambiarLaToleranciaRigeEnLaSiguienteEvaluacion() {
        casos.parametros.put("exceso_carga.tolerancia_kg", "2000");
        assertEquals(0, deteccion.evaluarPesada(pesada("P-1", 31_000, 77L)));
        casos.parametros.put("exceso_carga.tolerancia_kg", "0");
        assertEquals(1, deteccion.evaluarPesada(pesada("P-2", 30_100, 77L)));
    }

    @Test
    void unaPesadaSinGuiaVigenteEsViajeSinGuiaYUnoPorDia() {
        assertEquals(1, deteccion.evaluarPesada(pesada("P-1", 20_000, null)));
        assertEquals(0, deteccion.evaluarPesada(pesada("P-2", 20_000, null)));

        assertTrue(casos.porHecho.containsKey("VIAJE_SIN_GUIA:STZ1234:2026-10-15"));
    }

    @Test
    void unaGuiaAsignadaSinInicioNoEsUnViajeEnCurso() {
        casos.estadosGuia.put(77L, "ASIGNADA");
        assertEquals(1, deteccion.evaluarPesada(pesada("P-1", 20_000, 77L)));
        assertEquals(TipoInfraccion.VIAJE_SIN_GUIA, casos.porHecho.values().iterator().next().getTipo());
    }

    @Test
    void conLaItvVencidaEsSinHabilitacion() {
        casos.vehiculos.put("STZ1234", CasosEnMemoria.camion("STZ1234", 1, "2026-12-31", "2026-09-30"));
        assertEquals(1, deteccion.evaluarPesada(pesada("P-1", 20_000, 77L)));
        assertTrue(casos.porHecho.containsKey("SIN_HABILITACION:STZ1234:2026-10-15"));
    }

    @Test
    void unVehiculoNoRegistradoNoSePuedeAtribuirYNoGeneraCaso() {
        PesadaRegistradaDTO p = pesada("P-1", 90_000, null);
        p.setMatricula("GEN00001");
        assertEquals(0, deteccion.evaluarPesada(p));
        assertTrue(casos.porHecho.isEmpty());
    }

    @Test
    void elDesvioSeEvaluaAlTerminarElViaje() {
        casos.vehiculoDeGuia = casos.vehiculos.get("STZ1234");
        casos.recorrido = new Recorrido(200_000, 100, 35, 48_000);
        assertEquals(1, deteccion.evaluarFinDeViaje(77L));
        assertEquals(TipoInfraccion.DESVIO_RUTA, casos.porHecho.get("DESVIO_RUTA:guia:77").getTipo());

        casos.recorrido = new Recorrido(200_000, 100, 10, 48_000);
        casos.porHecho.clear();
        assertEquals(0, deteccion.evaluarFinDeViaje(77L));
    }

    @Test
    void sinCoordenadasDeLaGuiaNoSeEvaluaElDesvio() {
        casos.vehiculoDeGuia = casos.vehiculos.get("STZ1234");
        assertEquals(0, deteccion.evaluarFinDeViaje(77L));
    }

    @Test
    void elTrackingSinViajeEnCursoEsViajeSinGuia() {
        OffsetDateTime desde = OffsetDateTime.parse("2026-10-15T10:00:00-03:00");
        casos.actividad.add(new Actividad(11L, "STZ1234", desde, desde.plusMinutes(9), 18, false));
        casos.actividad.add(new Actividad(12L, "NOREG01", desde, desde.plusMinutes(9), 18, false));

        assertEquals(1, deteccion.evaluarActividadTracking(10));
        assertTrue(casos.porHecho.containsKey("VIAJE_SIN_GUIA:STZ1234:2026-10-15"));
    }

    @Test
    void elTrackingConViajeEnCursoNoGeneraCaso() {
        OffsetDateTime desde = OffsetDateTime.parse("2026-10-15T10:00:00-03:00");
        casos.actividad.add(new Actividad(11L, "STZ1234", desde, desde.plusMinutes(9), 18, true));
        assertEquals(0, deteccion.evaluarActividadTracking(10));
    }

    @Test
    void elPlazoDeDescargoSaleDelParametro() {
        casos.parametros.put("descargo.plazo_dias", "3");
        Instant antes = Instant.now();
        deteccion.evaluarPesada(pesada("P-1", 31_000, 77L));
        CasoFiscalizacion c = casos.porHecho.get("EXCESO_CARGA:pesada:P-1");
        long dias = java.time.Duration.between(antes, c.getVenceDescargo()).toDays();
        assertEquals(3, dias);
    }

    private static PesadaRegistradaDTO pesada(String id, int pesoKg, Long guiaId) {
        PesadaRegistradaDTO p = new PesadaRegistradaDTO();
        p.setIdPesada(id);
        p.setMatricula("STZ1234");
        p.setPeaje("Peaje Pando");
        p.setPesoKg(pesoKg);
        p.setInstante("2026-10-15T10:15:00-03:00");
        p.setGuiaId(guiaId);
        return p;
    }
}
