package uy.edu.fing.tse.carga.ejb.servicio;

import com.nimbusds.jose.util.JSONArrayUtils;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in destructivo SOLO sobre fixtures sinteticos aislados. No provisiona identidad ni servidor. */
@Tag("integracion")
@Execution(ExecutionMode.SAME_THREAD)
class P2ContraDespliegueTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String MARCA = "P2-" + UUID.randomUUID();
    private static HttpClient http;
    private static URI api;
    private static String responsable, chofer1Token, chofer2Token, admin;
    private static long empresaA, empresaB, vehiculo1, vehiculo2, chofer1, chofer2, rubro;
    private static Instant base;

    @BeforeAll static void configurar() {
        assumeTrue("true".equals(System.getenv("P2_PRUEBAS_HABILITADAS")),
                "Sin opt-in P2 no se hace ninguna llamada HTTP");
        assertEquals("true", requerida("P2_ENTORNO_AISLADO_CONFIRMADO"), "Confirmar entorno aislado");
        try { api = URI.create(requerida("P2_API_URL").replaceAll("/+$", "") + "/"); }
        catch (IllegalArgumentException e) { fail("P2_API_URL no es una URI valida (valor omitido)"); }
        assertTrue(Set.of("http", "https").contains(api.getScheme()), "URL HTTP(S) requerida");
        assertNotNull(api.getHost(), "Host requerido");
        assertTrue(api.getUserInfo() == null, "La URL no debe contener credenciales");
        assertTrue(api.getQuery() == null, "La URL no debe contener query");
        assertTrue(api.getFragment() == null, "La URL no debe contener fragmento");
        assertTrue(api.getPath().endsWith("/carga/api/"), "URL base debe terminar en /carga/api");
        responsable = token("P2_TOKEN_RESPONSABLE_A");
        chofer1Token = token("P2_TOKEN_CHOFER_A1");
        chofer2Token = token("P2_TOKEN_CHOFER_A2");
        admin = System.getenv("P2_TOKEN_ADMIN");
        if (admin != null && !admin.isBlank()) admin = token("P2_TOKEN_ADMIN");
        empresaA = id("P2_EMPRESA_A_ID"); empresaB = id("P2_EMPRESA_B_ID");
        vehiculo1 = id("P2_VEHICULO_A1_ID"); vehiculo2 = id("P2_VEHICULO_A2_ID");
        chofer1 = id("P2_CHOFER_A1_ID"); chofer2 = id("P2_CHOFER_A2_ID"); rubro = id("P2_RUBRO_ID");
        assertNotEquals(empresaA, empresaB); assertNotEquals(vehiculo1, vehiculo2); assertNotEquals(chofer1, chofer2);
        base = Instant.parse(requerida("P2_INICIO_BASE"));
        Instant finVentana = Instant.parse(requerida("P2_FIN_VENTANA"));
        assertFalse(finVentana.isBefore(base.plus(Duration.ofHours(4))), "Reservar al menos cuatro horas libres");
        http = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Test void responsableNoLeeFlotaNiGuiasDeOtraEmpresa() throws Exception {
        estado(403, get("vehiculos?empresaId=" + empresaB, responsable));
        estado(403, get("guias/empresa?empresaId=" + empresaB, responsable));
    }

    @Test void listadoGlobalProhibidoSalvoAdministradorOpcional() throws Exception {
        estado(403, get("vehiculos", responsable));
        if (admin != null && !admin.isBlank()) estado(200, get("vehiculos", admin));
        // Sin token ADMIN se verifica SOLO el rechazo no administrativo; documentar cobertura pendiente.
    }

    @Test void guiaValidaDisponibleSoloAlChoferAsignadoYResponsable() throws Exception {
        Instant inicio = base, fin = base.plusSeconds(1800);
        Foto antes = listarEmpresa(); libre(antes, inicio, fin);
        Map<String, Object> comando = comando(vehiculo1, chofer1, inicio, fin);
        HttpResponse<String> respuesta = post(comando); estado(201, respuesta);
        Map<String, Object> creada = objeto(respuesta.body()); long guiaId = numero(creada, "id");
        assertEquals(empresaA, numero(creada, "empresaId"));
        assertEquals("ASIGNADA", creada.get("estado"));
        assertEquals(-34.9, ((Number) creada.get("origenLat")).doubleValue());
        Foto despues = listarEmpresa(); unaEscritura(antes, despues, guiaId, inicio, fin);
        assertTrue(idsMisGuias(chofer1Token).contains(guiaId), "Chofer asignado debe descargar la guia");
        assertFalse(idsMisGuias(chofer2Token).contains(guiaId), "Otro chofer no debe descargar esa guia");
    }

    @Test void concurrenciaMismoVehiculoDistintosChoferes() throws Exception {
        carrera(vehiculo1, chofer1, vehiculo1, chofer2, base.plusSeconds(3600));
    }

    @Test void concurrenciaMismoChoferDistintosVehiculos() throws Exception {
        carrera(vehiculo1, chofer1, vehiculo2, chofer1, base.plusSeconds(7200));
    }

    @Test void periodosConsecutivosConfirmanAmbasGuias() throws Exception {
        Instant inicio = base.plusSeconds(10800), limite = inicio.plusSeconds(1800), fin = limite.plusSeconds(1800);
        Foto antes = listarEmpresa(); libre(antes, inicio, fin);
        HttpResponse<String> a = post(comando(vehiculo1, chofer1, inicio, limite)); estado(201, a);
        long idA = numero(objeto(a.body()), "id");
        Foto intermedia = listarEmpresa(); unaEscritura(antes, intermedia, idA, inicio, limite);
        HttpResponse<String> b = post(comando(vehiculo1, chofer1, limite, fin)); estado(201, b);
        long idB = numero(objeto(b.body()), "id");
        Foto despues = listarEmpresa(); unaEscritura(intermedia, despues, idB, limite, fin);
        assertNotEquals(idA, idB); assertEquals(antes.total() + 2, despues.total());
    }

    private static void carrera(long v1, long c1, long v2, long c2, Instant inicio) throws Exception {
        Instant fin = inicio.plusSeconds(1800);
        Foto antes = listarEmpresa(); libre(antes, inicio, fin);
        CyclicBarrier salida = new CyclicBarrier(3);
        var ejecutor = Executors.newFixedThreadPool(2);
        List<HttpResponse<String>> respuestas;
        try {
            var a = ejecutor.submit(() -> { salida.await(10, TimeUnit.SECONDS); return post(comando(v1, c1, inicio, fin)); });
            var b = ejecutor.submit(() -> { salida.await(10, TimeUnit.SECONDS); return post(comando(v2, c2, inicio, fin)); });
            salida.await(10, TimeUnit.SECONDS);
            respuestas = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            ejecutor.shutdownNow();
            assertTrue(ejecutor.awaitTermination(25, TimeUnit.SECONDS), "Solicitudes no terminaron dentro del limite");
        }
        assertEquals(List.of(201, 400), respuestas.stream().map(HttpResponse::statusCode).sorted().toList(),
                "Debe confirmarse una solicitud y rechazarse la otra por negocio");
        HttpResponse<String> rechazada = respuestas.stream().filter(r -> r.statusCode() == 400).findFirst().orElseThrow();
        Object error = objeto(rechazada.body()).get("error");
        assertTrue(error instanceof String mensaje && mensaje.contains("ya tiene una guia asignada"),
                "El rechazo debe ser por solapamiento, no por datos o fixtures invalidos");
        HttpResponse<String> ganadora = respuestas.stream().filter(r -> r.statusCode() == 201).findFirst().orElseThrow();
        long guiaId = numero(objeto(ganadora.body()), "id");
        Foto despues = listarEmpresa(); unaEscritura(antes, despues, guiaId, inicio, fin);
        Map<String, Object> persistida = despues.guias().get(guiaId);
        long v = numero(persistida, "vehiculoId"), c = numero(persistida, "choferId");
        assertTrue((v == v1 && c == c1) || (v == v2 && c == c2), "Asignacion ganadora inconsistente");
    }

    private static Map<String, Object> comando(long vehiculo, long chofer, Instant inicio, Instant fin) {
        Map<String, Object> c = new HashMap<>();
        c.put("vehiculoId", vehiculo); c.put("choferId", chofer); c.put("rubroId", rubro);
        c.put("pesoKg", 1); c.put("volumenM3", 1); c.put("inicio", inicio.toString()); c.put("fin", fin.toString());
        c.put("origen", MARCA); c.put("destino", MARCA + "-destino");
        c.put("origenLat", -34.9); c.put("origenLon", -56.2); c.put("destinoLat", -34.8); c.put("destinoLon", -56.1);
        return c;
    }

    private static void unaEscritura(Foto antes, Foto despues, long nueva, Instant inicio, Instant fin) {
        assertEquals(antes.total() + 1, despues.total(), "El rechazo no debe dejar otra guia parcial");
        Set<Long> agregadas = new HashSet<>(despues.guias().keySet()); agregadas.removeAll(antes.guias().keySet());
        assertEquals(Set.of(nueva), agregadas, "Solo una guia nueva debe ser observable");
        assertTrue(despues.guias().keySet().containsAll(antes.guias().keySet()), "No deben desaparecer guias previas");
        List<Map<String, Object>> coincidentes = despues.guias().values().stream()
                .filter(g -> MARCA.equals(g.get("origen")))
                .filter(g -> inicio.equals(Instant.parse((String) g.get("inicio"))) && fin.equals(Instant.parse((String) g.get("fin"))))
                .toList();
        assertEquals(1, coincidentes.size(), "Solo una escritura en el periodo del escenario");
        Map<String, Object> guia = despues.guias().get(nueva);
        assertEquals(empresaA, numero(guia, "empresaId"));
        assertTrue(numero(guia, "vehiculoId") > 0); assertTrue(numero(guia, "choferId") > 0);
    }

    private static void libre(Foto foto, Instant inicio, Instant fin) {
        for (Map<String, Object> g : foto.guias().values()) {
            Object v = g.get("vehiculoId"), c = g.get("choferId");
            boolean recurso = v instanceof Number n && (n.longValue() == vehiculo1 || n.longValue() == vehiculo2);
            recurso |= c instanceof Number n && (n.longValue() == chofer1 || n.longValue() == chofer2);
            if (recurso && !"FINALIZADA".equals(g.get("estado"))) {
                Instant a = Instant.parse((String) g.get("inicio")), b = Instant.parse((String) g.get("fin"));
                assertFalse(a.isBefore(fin) && inicio.isBefore(b), "Fixture ocupada: reservar otra ventana antes de repetir");
            }
        }
    }

    /** Recorre todas las paginas, valida total estable y no acepta omisiones/IDs duplicados. */
    private static Foto listarEmpresa() throws Exception {
        Map<Long, Map<String, Object>> guias = new HashMap<>(); long total = -1;
        for (int pagina = 0; pagina < 10000; pagina++) {
            HttpResponse<String> respuesta = get("guias/empresa?empresaId=" + empresaA + "&pagina=" + pagina + "&tamano=100", responsable);
            estado(200, respuesta); Map<String, Object> doc = objeto(respuesta.body());
            long recibido = numero(doc, "total"); assertTrue(recibido >= 0);
            if (total == -1) total = recibido;
            assertEquals(total, recibido, "Empresa cambio durante la lectura: usar fixtures exclusivas sin otros escritores");
            assertEquals(pagina, numero(doc, "pagina")); assertEquals(100, numero(doc, "tamano"));
            Object registros = doc.get("registros"); assertTrue(registros instanceof List<?>, "Falta registros");
            List<?> lista = (List<?>) registros;
            assertTrue(lista.size() <= 100, "Pagina supera tamano");
            for (Object registro : lista) {
                Map<String, Object> g = mapa(mapa(registro).get("guia"));
                assertEquals(empresaA, numero(g, "empresaId"));
                assertTrue(guias.put(numero(g, "id"), g) == null, "ID duplicado entre paginas");
            }
            assertTrue(guias.size() <= total, "Se recibieron mas guias que el total");
            if (guias.size() == total) return new Foto(Map.copyOf(guias), total);
            assertFalse(lista.isEmpty(), "Paginacion incompleta antes de alcanzar el total");
        }
        return fail("Paginacion excede limite de seguridad: usar entorno aislado mas pequeno");
    }

    private static Set<Long> idsMisGuias(String token) throws Exception {
        HttpResponse<String> respuesta = get("guias/mis-guias", token); estado(200, respuesta);
        List<Object> lista;
        try { lista = JSONArrayUtils.parse(respuesta.body()); }
        catch (ParseException e) { return fail("Respuesta de mis-guias no es JSON valido (cuerpo omitido)"); }
        Set<Long> ids = new HashSet<>();
        for (Object elemento : lista) ids.add(numero(mapa(elemento), "id"));
        return ids;
    }

    private static HttpResponse<String> get(String ruta, String token) throws Exception {
        return http.send(pedido(ruta, token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(Map<String, Object> comando) throws Exception {
        return http.send(pedido("guias", responsable).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSONObjectUtils.toJSONString(comando))).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.Builder pedido(String ruta, String token) {
        return HttpRequest.newBuilder(api.resolve(ruta)).timeout(TIMEOUT)
                .header("Accept", "application/json").header("Authorization", "Bearer " + token);
    }

    private static void estado(int esperado, HttpResponse<?> respuesta) {
        assertEquals(esperado, respuesta.statusCode(), "HTTP inesperado; cuerpo y credenciales omitidos");
    }

    private static Map<String, Object> objeto(String cuerpo) {
        try { return JSONObjectUtils.parse(cuerpo); }
        catch (ParseException e) { return fail("Respuesta no es objeto JSON valido (cuerpo omitido)"); }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        assertTrue(valor instanceof Map<?, ?>, "Objeto JSON requerido"); return (Map<String, Object>) valor;
    }

    private static long numero(Map<String, Object> doc, String clave) {
        Object valor = doc.get(clave); assertTrue(valor instanceof Number, "Campo numerico requerido: " + clave);
        return ((Number) valor).longValue();
    }

    private static String requerida(String variable) {
        String valor = System.getenv(variable);
        assertTrue(valor != null && !valor.isBlank(), "Falta configurar " + variable); return valor;
    }

    private static String token(String variable) {
        String valor = requerida(variable);
        assertTrue(valor.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c)),
                "El token de " + variable + " no debe contener espacios ni caracteres de control (valor omitido)");
        return valor;
    }

    private static long id(String variable) {
        long valor = Long.parseLong(requerida(variable)); assertTrue(valor > 0, "ID positivo requerido: " + variable); return valor;
    }

    private record Foto(Map<Long, Map<String, Object>> guias, long total) { }
}
