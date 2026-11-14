package uy.edu.fing.tse.carga.ejb.integracion;

import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.dto.planificacion.PuntoMapaDTO;
import uy.edu.fing.tse.carga.api.dto.planificacion.SugerenciaCalleDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;
import com.nimbusds.jose.util.JSONObjectUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Fixtures sinteticos: no usa claves ni realiza llamadas reales. */
class PlanificacionGeograficaTest {
    private static final String RUTA = """
        {"features":[{"geometry":{"type":"LineString","coordinates":[[-56.2,-34.9],[-55.2,-34.4]]},
          "properties":{"summary":{"distance":120000,"duration":6500}}}]}
        """;

    @Test void candidatosSonSelectoresNoPuntosYNoIncluyenLocalidades() throws Exception {
        var http = new Transporte();
        http.cuerpo = """
            [{"type":"CALLEyPORTAL","address":"SUCRE 10, MONTEVIDEO","idCalle":9080,
              "portalNumber":10,"lat":0,"lng":0,"letra":null},
             {"type":"LOCALIDAD","address":"MONTEVIDEO","lat":-34.8,"lng":-56.2}]
            """;
        var resultado = ide(http).sugerir("  sucre  ");
        assertEquals(1, resultado.size());
        assertEquals(9080L, resultado.getFirst().getIdCalle());
        assertEquals(10, resultado.getFirst().getNumeroPuerta());
        assertTrue(http.uri.toString().contains("candidates?q=sucre&limit=5"));
        assertNull(http.clave);
    }

    @Test void resuelveIdentificadorYPortalSinUsarTextoComoUrl() throws Exception {
        var http = new Transporte();
        http.cuerpo = """
            [{"type":"CALLEyPORTAL","address":"SUCRE 10, MONTEVIDEO","lat":-34.9,"lng":-56.2,
              "state":1,"stateMsg":""}]
            """;
        var seleccion = calle(); seleccion.setTipo("CALLEyPORTAL"); seleccion.setNumeroPuerta(10);
        var direccion = ide(http).resolver(seleccion);
        assertTrue(http.uri.toString().contains("find?type=calle&idcalle=9080&portal=10"));
        assertEquals(-34.9, direccion.getPunto().getLatitud());
        assertEquals(-56.2, direccion.getPunto().getLongitud());
        assertFalse(direccion.isAproximada());
    }

    @Test void calleSinPortalSiempreAdvierteUbicacionAproximada() throws Exception {
        var http = new Transporte();
        http.cuerpo = "[{\"address\":\"SUCRE\",\"lat\":-34.9,\"lng\":-56.2,\"state\":1}]";
        assertTrue(ide(http).resolver(calle()).isAproximada());
    }

    @Test void nuncaDevuelveCeroCeroNiEligeEntreResultadosAmbiguos() {
        for (String cuerpo : List.of("[]", "[{\"address\":\"SUCRE\",\"lat\":0,\"lng\":0}]",
                "[{\"address\":\"A\",\"lat\":-34,\"lng\":-56},{\"address\":\"B\",\"lat\":-35,\"lng\":-56}]")) {
            var http = new Transporte(); http.cuerpo = cuerpo;
            assertThrows(NegocioException.class, () -> ide(http).resolver(calle()));
        }
    }

    @Test void alternativasConservanTodosLosPortalesSinElegirElPrimero() throws Exception {
        var http = new Transporte();
        http.cuerpo = """
            [{"type":"CALLEyPORTAL","address":"SUCRE 10 B","lat":-34.9,"lng":-56.2,"state":2,"stateMsg":"Aproximado"},
             {"type":"CALLEyPORTAL","address":"SUCRE 10 C","lat":-34.8,"lng":-56.1,"state":1}]
            """;
        var seleccion = calle(); seleccion.setTipo("CALLEyPORTAL"); seleccion.setNumeroPuerta(10);
        var resultado = ide(http).resolverAlternativas(seleccion);
        assertEquals(List.of("SUCRE 10 B", "SUCRE 10 C"), resultado.stream().map(d -> d.getDireccion()).toList());
        assertTrue(resultado.getFirst().isAproximada());
        assertFalse(resultado.get(1).isAproximada());
        assertEquals(-34.9, resultado.getFirst().getPunto().getLatitud());
        assertEquals(-56.1, resultado.get(1).getPunto().getLongitud());
        assertTrue(http.uri.toString().endsWith("find?type=calle&idcalle=9080&portal=10"));
        assertEquals(1, http.llamadas);
        assertNull(http.clave);
        assertThrows(NegocioException.class, () -> ide(http).resolver(seleccion));
    }

    @Test void portalAusenteNoSeReemplazaPorElCentroDeLaCalle() throws Exception {
        for (String cuerpo : List.of("[]",
                "[{\"type\":\"CALLE\",\"address\":\"SUCRE\",\"lat\":-34.9,\"lng\":-56.2,\"state\":2}]")) {
            var http = new Transporte(); http.cuerpo = cuerpo;
            var seleccion = calle(); seleccion.setTipo("CALLEyPORTAL"); seleccion.setNumeroPuerta(10);
            assertTrue(ide(http).resolverAlternativas(seleccion).isEmpty());
            assertEquals(1, http.llamadas);
            assertTrue(http.uri.toString().contains("&portal=10"));
        }
    }

    @Test void alternativaUnicaMantieneCoordenadasAproximacionYLetra() throws Exception {
        var http = new Transporte();
        http.cuerpo = "[{\"type\":\"CALLEyPORTAL\",\"address\":\"SUCRE 10 B\",\"lat\":-34.9,\"lng\":-56.2,\"state\":1}]";
        var seleccion = calle(); seleccion.setTipo("CALLEyPORTAL"); seleccion.setNumeroPuerta(10); seleccion.setLetra("B");
        var alternativas = ide(http).resolverAlternativas(seleccion);
        assertEquals(1, alternativas.size());
        assertFalse(alternativas.getFirst().isAproximada());
        assertTrue(http.uri.toString().endsWith("&portal=10&letra=B"));
        var unica = ide(http).resolver(seleccion);
        assertEquals(unica.getDireccion(), alternativas.getFirst().getDireccion());
        assertEquals(unica.getDetalle(), alternativas.getFirst().getDetalle());
        assertEquals(unica.getPunto().getLatitud(), alternativas.getFirst().getPunto().getLatitud());
        assertEquals(unica.getPunto().getLongitud(), alternativas.getFirst().getPunto().getLongitud());
        http.cuerpo = "[{\"type\":\"CALLE\",\"address\":\"SUCRE\",\"lat\":-34.9,\"lng\":-56.2,\"state\":1}]";
        assertTrue(ide(http).resolverAlternativas(calle()).getFirst().isAproximada());
    }

    @Test void alternativasNoConfundenFallosTecnicosNiPuntosInvalidosConPortalAusente() {
        for (String cuerpo : List.of("{}", "null", "no-json", "[{}]")) {
            var http = new Transporte(); http.cuerpo = cuerpo;
            assertThrows(ServicioNoDisponibleException.class, () -> ide(http).resolverAlternativas(calle()));
        }
        var falloHttp = new Transporte(); falloHttp.estado = 503;
        assertThrows(ServicioNoDisponibleException.class, () -> ide(falloHttp).resolverAlternativas(calle()));
        var puntoInvalido = new Transporte(); puntoInvalido.cuerpo = "[{\"address\":\"SUCRE\",\"lat\":0,\"lng\":0}]";
        var cliente = ide(puntoInvalido);
        assertThrows(NegocioException.class, () -> cliente.resolverAlternativas(calle()));
    }

    @Test void entradasInvalidasNoLleganAlProveedor() {
        var http = new Transporte(); var cliente = ide(http);
        for (String texto : new String[]{null, "ab", "x".repeat(201)})
            assertThrows(NegocioException.class, () -> cliente.sugerir(texto));
        var seleccion = calle(); seleccion.setIdCalle(-1L);
        assertThrows(NegocioException.class, () -> cliente.resolver(seleccion));
        assertThrows(NegocioException.class, () -> cliente.resolverAlternativas(seleccion));
        assertEquals(0, http.llamadas);
    }

    @Test void ideRechazaRespuestaQueNoEsUnaListaJsonCompleta() {
        for (String cuerpo : List.of("{}", "null", "[],\"datos\":[]", "no-json", "[{\"address\":\"SIN TIPO\"}]")) {
            var http = new Transporte(); http.cuerpo = cuerpo;
            assertThrows(ServicioNoDisponibleException.class, () -> ide(http).sugerir("sucre"));
        }
    }

    @Test void advertenciasOrsNoSePierdenNiExponenCuerpoCrudo() throws Exception {
        var http = new Transporte();
        http.cuerpo = RUTA.replace("\"summary\":", "\"warnings\":[{\"message\":\"sintetico-no-publicar\"}],\"summary\":");
        var ruta = ors(http).calcular(punto(-34.9,-56.2), punto(-34.4,-55.2), BigDecimal.TEN);
        assertTrue(ruta.isAdvertenciasProveedor());
    }

    @Test void orsEnviaSoloPesoTotalEnToneladasYLonLat() throws Exception {
        var http = new Transporte(); http.cuerpo = RUTA;
        var cliente = ors(http);
        var ruta = cliente.calcular(punto(-34.9, -56.2), punto(-34.4, -55.2), new BigDecimal("20500"));
        Map<String, Object> doc = JSONObjectUtils.parse(http.enviado);
        assertEquals(List.of(List.of(-56.2, -34.9), List.of(-55.2, -34.4)), doc.get("coordinates"));
        Map<?, ?> opciones = (Map<?, ?>) doc.get("options");
        assertEquals("hgv", opciones.get("vehicle_type"));
        Map<?, ?> restricciones = (Map<?, ?>) ((Map<?, ?>) opciones.get("profile_params")).get("restrictions");
        assertEquals(1, restricciones.size());
        assertEquals(20.5, ((Number) restricciones.get("weight")).doubleValue());
        assertEquals("clave-sintetica", http.clave);
        assertFalse(http.uri.toString().contains("clave"));
        assertEquals(new BigDecimal("20500"), ruta.getPesoTotalKg());
        assertEquals(-34.9, ruta.getPuntos().getFirst().getLatitud());
        assertEquals(120000, ruta.getDistanciaMetros().intValue());
    }

    @Test void sinClaveNoHaceLlamadaNiDevuelveElSecretoEnError() {
        var http = new Transporte(); var cliente = ors(http);
        cliente.configuracion = config("");
        assertThrows(ServicioNoDisponibleException.class,
                () -> cliente.calcular(punto(-34.9,-56.2), punto(-34.4,-55.2), BigDecimal.TEN));
        assertEquals(0, http.llamadas);
    }

    @Test void sinRutaEsNegocioPeroCuotaRedireccionYJsonInvalidoSonIndisponibilidad() {
        var http = new Transporte(); http.estado = 404; http.cuerpo = "{\"error\":{\"code\":2009}}";
        var sinRuta = ors(http);
        assertThrows(NegocioException.class, () -> calcular(sinRuta));
        for (int estado : new int[]{302, 403, 429, 500}) {
            http = new Transporte(); http.estado = estado; http.cuerpo = "clave-sintetica";
            var cliente = ors(http);
            String mensaje = assertThrows(ServicioNoDisponibleException.class, () -> calcular(cliente)).getMessage();
            assertFalse(mensaje.contains("clave-sintetica"));
        }
        http = new Transporte(); http.cuerpo = "{respuesta truncada";
        var cliente = ors(http);
        assertThrows(ServicioNoDisponibleException.class, () -> calcular(cliente));
    }

    @Test void circuitoOrsNoAbreElDeIdeYRespuestaMalformadaNoEsRutaVacia() throws Exception {
        var http = new Transporte(); http.estado = 503; var cliente = ors(http);
        for (int i = 0; i < 4; i++) assertThrows(ServicioNoDisponibleException.class, () -> calcular(cliente));
        assertEquals(3, http.llamadas);
        http.estado = 200; http.cuerpo = "[]";
        assertTrue(ide(http).sugerir("sucre").isEmpty());
        http.cuerpo = "{\"features\":[{\"geometry\":{\"type\":\"Point\",\"coordinates\":[0,0]}}]}";
        var otro = ors(http);
        assertThrows(ServicioNoDisponibleException.class, () -> calcular(otro));
    }

    private static void calcular(ClienteRutasOrs c) throws NegocioException {
        c.calcular(punto(-34.9,-56.2), punto(-34.4,-55.2), BigDecimal.TEN);
    }
    static PuntoMapaDTO punto(double lat, double lon) {
        var p = new PuntoMapaDTO(); p.setLatitud(lat); p.setLongitud(lon); return p;
    }
    private static SugerenciaCalleDTO calle() {
        var s = new SugerenciaCalleDTO(); s.setTipo("CALLE"); s.setDireccion("SUCRE"); s.setIdCalle(9080L); return s;
    }
    private static ClienteDireccionesIde ide(Transporte http) {
        var c = new ClienteDireccionesIde(); c.http = http; c.configuracion = config("clave-sintetica"); return c;
    }
    private static ClienteRutasOrs ors(Transporte http) {
        var c = new ClienteRutasOrs(); c.http = http; c.configuracion = config("clave-sintetica"); return c;
    }
    private static ConfiguracionGeografica config(String clave) {
        return new ConfiguracionGeografica() {
            @Override public URI ideBase() { return URI.create("https://ide.test/geocode"); }
            @Override public URI orsRuta() { return URI.create("https://ors.test/driving-hgv/geojson"); }
            @Override public String claveOrs() { return clave; }
        };
    }
    private static class Transporte extends ClienteGeograficoHttp {
        URI uri; String enviado; String clave; int estado = 200; int llamadas; String cuerpo = "[]";
        @Override public Respuesta solicitar(URI destino, String metodo, String body, String apiKey) {
            llamadas++; uri = destino; enviado = body; clave = apiKey; return new Respuesta(estado, cuerpo);
        }
    }
}
