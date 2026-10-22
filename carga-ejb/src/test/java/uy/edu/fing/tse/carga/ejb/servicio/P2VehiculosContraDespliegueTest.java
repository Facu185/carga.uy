package uy.edu.fing.tse.carga.ejb.servicio;

import com.nimbusds.jose.util.JSONArrayUtils;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uy.edu.fing.tse.carga.api.security.Roles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** F11 HTTP opt-in sobre entorno desechable. No fabrica identidad ni estados de guia. */
@Tag("integracion")
@Execution(ExecutionMode.SAME_THREAD)
class P2VehiculosContraDespliegueTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static HttpClient http;
    private static URI api;
    private static String responsable, admin, revocado, funcionario;
    private static long empresaA, empresaB, heredado, enCurso, vehiculoB, guiaEnCurso;

    @BeforeAll static void configurar() throws Exception {
        assumeTrue("true".equals(System.getenv("P2_F11_PRUEBAS_HABILITADAS")),
                "Sin opt-in F11 no se hace ninguna llamada HTTP");
        assertEquals("true", requerida("P2_ENTORNO_AISLADO_CONFIRMADO"), "Confirmar entorno aislado");
        try { api = URI.create(requerida("P2_API_URL").replaceAll("/+$", "") + "/"); }
        catch (IllegalArgumentException e) { fail("P2_API_URL no es una URI valida (valor omitido)"); }
        assertTrue(Set.of("http", "https").contains(api.getScheme()), "URL HTTP(S) requerida");
        assertNotNull(api.getHost(), "Host requerido");
        assertTrue(api.getUserInfo() == null, "URL sin credenciales (valor omitido)");
        assertTrue(api.getQuery() == null, "URL sin query (valor omitido)");
        assertTrue(api.getFragment() == null, "URL sin fragmento (valor omitido)");
        assertTrue(api.getPath().endsWith("/carga/api/"), "Base terminada en /carga/api");
        responsable = token("P2_TOKEN_RESPONSABLE_A");
        admin = token("P2_TOKEN_ADMIN");
        revocado = token("P2_F11_TOKEN_REVOCADO_A");
        funcionario = token("P2_F11_TOKEN_FUNCIONARIO");
        empresaA = id("P2_EMPRESA_A_ID"); empresaB = id("P2_EMPRESA_B_ID");
        heredado = id("P2_F11_VEHICULO_HEREDADO_ID");
        enCurso = id("P2_F11_VEHICULO_EN_CURSO_ID");
        vehiculoB = id("P2_F11_VEHICULO_B_ID");
        guiaEnCurso = id("P2_F11_GUIA_EN_CURSO_ID");
        assertNotEquals(empresaA, empresaB, "Empresas distintas");
        assertEquals(3, Set.of(heredado, enCurso, vehiculoB).size(), "Vehiculos distintos");
        http = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();
        rolUnico(responsable, Roles.CIUDADANO);
        rolUnico(admin, Roles.ADMINISTRADOR);
        rolUnico(revocado, Roles.CIUDADANO);
        rolUnico(funcionario, Roles.FUNCIONARIO);
        vinculoResponsable(responsable, "ACTIVO");
        vinculoResponsable(revocado, "REVOCADO");
        Map<Long, Map<String, Object>> flotaA = flota(empresaA, responsable);
        validarHeredado(vehiculo(flotaA, heredado));
        assertEquals(Boolean.TRUE, vehiculo(flotaA, enCurso).get("habilitado"), "Fixture en curso habilitada");
        vehiculo(flota(empresaB, admin), vehiculoB);
        guiaEnCurso();
    }

    @Test void altaEdicionDuplicadoBajaYReactivacionPersistenSinRenovarVigencias() throws Exception {
        Map<Long, Map<String, Object>> antes = flota(empresaA, responsable);
        Map<String, Object> datos = datos(empresaA);
        HttpResponse<String> alta = enviar("POST", "vehiculos", responsable, datos);
        estado(201, alta);
        Map<String, Object> creada = objeto(alta.body());
        long id = numero(creada, "id");
        Map<Long, Map<String, Object>> despues = flota(empresaA, responsable);
        assertEquals(antes.size() + 1, despues.size(), "Una sola alta observable");
        assertTrue(despues.keySet().containsAll(antes.keySet()), "Conservar vehiculos anteriores");
        assertTrue(antes.entrySet().stream().allMatch(e -> e.getValue().equals(despues.get(e.getKey()))),
                "El alta no debe alterar filas anteriores (datos omitidos)");
        Map<String, Object> actual = vehiculo(despues, id);
        mismoVehiculo(creada, actual);
        assertEquals(((String) datos.get("matricula")).toUpperCase(java.util.Locale.ROOT), actual.get("matricula"));
        assertEquals(Boolean.TRUE, actual.get("habilitado"));

        rechazo(400, enviar("POST", "vehiculos", responsable, datos), "Ya existe un vehiculo");
        mismaFlota(despues, flota(empresaA, responsable));
        Map<String, Object> invalida = new HashMap<>(actual);
        invalida.put("pesoKg", -1);
        rechazo(400, enviar("PUT", "vehiculos/" + id, responsable, invalida), "peso");
        mismaFlota(despues, flota(empresaA, responsable));

        Map<String, Object> edicion = new HashMap<>(actual);
        edicion.put("modelo", "MODELO-EDITADO-F11");
        estado(200, enviar("PUT", "vehiculos/" + id, responsable, edicion));
        actual = vehiculo(flota(empresaA, responsable), id);
        assertEquals("MODELO-EDITADO-F11", actual.get("modelo"));
        assertEquals(Boolean.TRUE, actual.get("habilitado"));
        sinContenido(enviar("DELETE", "vehiculos/" + id, responsable, null));
        Map<String, Object> baja = vehiculo(flota(empresaA, responsable), id);
        soloHabilitacion(actual, baja, false);

        edicion = new HashMap<>(baja);
        edicion.put("modelo", "MODELO-INACTIVO-F11");
        estado(200, enviar("PUT", "vehiculos/" + id, responsable, edicion));
        Map<String, Object> inactivoEditado = vehiculo(flota(empresaA, responsable), id);
        assertEquals("MODELO-INACTIVO-F11", inactivoEditado.get("modelo"));
        assertEquals(Boolean.FALSE, inactivoEditado.get("habilitado"));
        for (int intento = 0; intento < 2; intento++) {
            sinContenido(enviar("PUT", "vehiculos/" + id + "/reactivar", responsable, null));
            soloHabilitacion(inactivoEditado, vehiculo(flota(empresaA, responsable), id), true);
        }
        // Las fechas fijas vencidas se conservan; reactivar no equivale a renovar permiso/ITV.
    }

    @Test void reactivarHeredadoConFechasNulasConservaTodosSusOtrosDatos() throws Exception {
        Map<String, Object> antes = vehiculo(flota(empresaA, responsable), heredado);
        validarHeredado(antes);
        for (int intento = 0; intento < 2; intento++) {
            sinContenido(enviar("PUT", "vehiculos/" + heredado + "/reactivar", responsable, null));
            soloHabilitacion(antes, vehiculo(flota(empresaA, responsable), heredado), true);
        }
    }

    @Test void guiaPersistidaEnCursoImpideDeleteYPutInclusoAlAdministrador() throws Exception {
        Map<String, Object> guiaAntes = guiaEnCurso();
        Map<String, Object> antes = vehiculo(flota(empresaA, responsable), enCurso);
        assertEquals(Boolean.TRUE, antes.get("habilitado"), "Fixture en curso habilitada");
        Map<String, Object> cambio = new HashMap<>(antes);
        cambio.put("habilitado", false);
        cambio.put("modelo", "NO-DEBE-PERSISTIR-F11");
        for (String identidad : List.of(responsable, admin)) {
            rechazo(400, enviar("DELETE", "vehiculos/" + enCurso, identidad, null), "guia en curso");
            mismoVehiculo(antes, vehiculo(flota(empresaA, responsable), enCurso));
            rechazo(400, enviar("PUT", "vehiculos/" + enCurso, identidad, cambio), "guia en curso");
            mismoVehiculo(antes, vehiculo(flota(empresaA, responsable), enCurso));
            assertTrue(guiaAntes.equals(guiaEnCurso()), "No alterar la guia para simular fin (datos omitidos)");
        }
    }

    @Test void empresaAjenaYSinSesionRechazanAccesoDirectoSinMutaciones() throws Exception {
        operacionesDenegadas(responsable, empresaB, vehiculoB, 403);
        operacionesDenegadas(null, empresaB, vehiculoB, 401);
        estado(403, enviar("GET", "vehiculos", responsable, null));
        estado(200, enviar("GET", "vehiculos", admin, null));

        Map<Long, Map<String, Object>> antes = flota(empresaA, responsable);
        Map<String, Object> traslado = new HashMap<>(vehiculo(antes, enCurso));
        traslado.put("empresaId", empresaB);
        rechazo(403, enviar("PUT", "vehiculos/" + enCurso, responsable, traslado), "");
        mismaFlota(antes, flota(empresaA, responsable));
    }

    @Test void vinculoRevocadoYRolFuncionarioNoHabilitanLecturaNiMutacionesVehiculares() throws Exception {
        operacionesDenegadas(revocado, empresaA, enCurso, 403);
        operacionesDenegadas(funcionario, empresaA, enCurso, 403);
    }

    private static void operacionesDenegadas(String identidad, long empresa, long id, int codigo) throws Exception {
        Map<Long, Map<String, Object>> antes = flota(empresa, admin);
        Map<String, Object> cambio = new HashMap<>(vehiculo(antes, id));
        cambio.put("modelo", "NO-DEBE-PERSISTIR-F11");
        rechazo(codigo, enviar("GET", "vehiculos?empresaId=" + empresa, identidad, null), "");
        rechazo(codigo, enviar("POST", "vehiculos", identidad, datos(empresa)), "");
        mismaFlota(antes, flota(empresa, admin));
        rechazo(codigo, enviar("PUT", "vehiculos/" + id, identidad, cambio), "");
        mismaFlota(antes, flota(empresa, admin));
        rechazo(codigo, enviar("DELETE", "vehiculos/" + id, identidad, null), "");
        mismaFlota(antes, flota(empresa, admin));
        rechazo(codigo, enviar("PUT", "vehiculos/" + id + "/reactivar", identidad, null), "");
        mismaFlota(antes, flota(empresa, admin));
    }

    private static Map<String, Object> datos(long empresa) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("empresaId", empresa);
        datos.put("matricula", "f11" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        datos.put("marca", "MARCA-F11"); datos.put("modelo", "MODELO-F11");
        datos.put("pesoKg", 1000); datos.put("capacidadCargaKg", 2000); datos.put("capacidadVolumenM3", 10);
        datos.put("numeroPermiso", "PERMISO-F11"); datos.put("habilitado", true);
        datos.put("permisoDesde", "2020-01-01"); datos.put("permisoHasta", "2020-12-31");
        datos.put("itvVencimiento", "2020-12-31");
        return datos;
    }

    private static Map<Long, Map<String, Object>> flota(long empresa, String identidad) throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "vehiculos?empresaId=" + empresa, identidad, null);
        estado(200, respuesta);
        Map<Long, Map<String, Object>> resultado = new HashMap<>();
        for (Object valor : lista(respuesta.body())) {
            Map<String, Object> vehiculo = mapa(valor);
            assertEquals(empresa, numero(vehiculo, "empresaId"), "Listado aislado por empresa");
            assertTrue(resultado.put(numero(vehiculo, "id"), vehiculo) == null, "Sin IDs duplicados en la flota");
        }
        return resultado;
    }

    private static Map<String, Object> vehiculo(Map<Long, Map<String, Object>> flota, long id) {
        assertTrue(flota.containsKey(id), "Fixture vehicular presente en su empresa (datos omitidos)");
        return flota.get(id);
    }

    private static void validarHeredado(Map<String, Object> vehiculo) {
        assertEquals(Boolean.FALSE, vehiculo.get("habilitado"), "Fixture heredada inicialmente dada de baja");
        for (String fecha : List.of("permisoDesde", "permisoHasta", "itvVencimiento")) {
            assertTrue(vehiculo.get(fecha) == null, "Fecha heredada ausente, no texto null (datos omitidos)");
        }
        assertTrue(vehiculo.get("capacidadVolumenM3") == null, "Fixture sin capacidad volumetrica configurada");
    }

    private static Map<String, Object> guiaEnCurso() throws Exception {
        long total = -1, leidos = 0;
        for (int pagina = 0; pagina < 10000; pagina++) {
            HttpResponse<String> respuesta = enviar("GET", "guias/empresa?empresaId=" + empresaA
                    + "&pagina=" + pagina + "&tamano=100", responsable, null);
            estado(200, respuesta);
            Map<String, Object> doc = objeto(respuesta.body());
            long recibido = numero(doc, "total");
            assertTrue(recibido >= 0, "Total no negativo");
            if (total == -1) total = recibido;
            assertEquals(total, recibido, "Fixture sin escritores concurrentes");
            assertEquals(pagina, numero(doc, "pagina"));
            assertEquals(100, numero(doc, "tamano"));
            assertTrue(doc.get("registros") instanceof List<?>, "Pagina con registros");
            List<?> registros = (List<?>) doc.get("registros");
            assertTrue(registros.size() <= 100, "Pagina dentro del limite");
            for (Object registro : registros) {
                Map<String, Object> guia = mapa(mapa(registro).get("guia"));
                if (numero(guia, "id") == guiaEnCurso) {
                    assertEquals(empresaA, numero(guia, "empresaId"));
                    assertEquals(enCurso, numero(guia, "vehiculoId"));
                    assertEquals("EN_CURSO", guia.get("estado"), "Estado persistido, no inferido de fechas");
                    return guia;
                }
            }
            leidos += registros.size();
            if (leidos >= total || registros.isEmpty()) break;
        }
        return fail("Falta guia EN_CURSO acordada; no fabricarla desde la prueba");
    }

    private static void rolUnico(String identidad, String rol) throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "auth/yo", identidad, null);
        estado(200, respuesta);
        assertTrue(List.of(rol).equals(objeto(respuesta.body()).get("roles")),
                "Identidad vigente con solo el rol previsto (identidad omitida)");
    }

    private static void vinculoResponsable(String identidad, String estado) throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "solicitudes-empresa/vinculos/mios", identidad, null);
        estado(200, respuesta);
        List<Map<String, Object>> vinculos = lista(respuesta.body()).stream().map(P2VehiculosContraDespliegueTest::mapa)
                .filter(v -> numero(v, "empresaId") == empresaA && "RESPONSABLE".equals(v.get("tipo"))).toList();
        assertTrue(vinculos.stream().anyMatch(v -> estado.equals(v.get("estado"))), "Vinculo de fixture requerido");
        if ("REVOCADO".equals(estado)) {
            assertTrue(vinculos.stream().noneMatch(v -> "ACTIVO".equals(v.get("estado"))),
                    "Identidad revocada sin otro vinculo responsable activo con A");
        }
    }

    private static void soloHabilitacion(Map<String, Object> antes, Map<String, Object> despues, boolean habilitado) {
        Map<String, Object> esperado = new HashMap<>(antes);
        esperado.put("habilitado", habilitado);
        mismoVehiculo(esperado, despues);
    }

    private static void mismoVehiculo(Map<String, Object> esperado, Map<String, Object> actual) {
        assertTrue(numerosNormalizados(esperado).equals(numerosNormalizados(actual)),
                "Todos los campos vehiculares deben conservarse (datos omitidos)");
    }

    /** JSON 10 y 10.00 representan la misma capacidad; la escala puede cambiar al persistir. */
    private static Map<String, Object> numerosNormalizados(Map<String, Object> datos) {
        Map<String, Object> resultado = new HashMap<>();
        datos.forEach((clave, valor) -> resultado.put(clave, valor instanceof Number numero
                ? new BigDecimal(numero.toString()).stripTrailingZeros() : valor));
        return resultado;
    }

    private static void mismaFlota(Map<Long, Map<String, Object>> esperada, Map<Long, Map<String, Object>> actual) {
        assertTrue(esperada.equals(actual), "Rechazo sin altas, bajas ni cambios observables (datos omitidos)");
    }

    private static HttpResponse<String> enviar(String metodo, String ruta, String identidad, Map<String, Object> cuerpo)
            throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(api.resolve(ruta)).timeout(TIMEOUT)
                .header("Accept", "application/json").header("X-Correlation-Id", "P2F11-" + UUID.randomUUID());
        if (identidad != null) pedido.header("Authorization", "Bearer " + identidad);
        HttpRequest.BodyPublisher contenido = HttpRequest.BodyPublishers.noBody();
        if (cuerpo != null) {
            pedido.header("Content-Type", "application/json");
            contenido = HttpRequest.BodyPublishers.ofString(JSONObjectUtils.toJSONString(cuerpo));
        }
        return http.send(pedido.method(metodo, contenido).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void estado(int esperado, HttpResponse<?> respuesta) {
        assertEquals(esperado, respuesta.statusCode(), "HTTP inesperado; cuerpo y credenciales omitidos");
    }

    private static void sinContenido(HttpResponse<String> respuesta) {
        estado(204, respuesta);
        assertTrue(respuesta.body().isEmpty(), "204 sin cuerpo");
    }

    private static void rechazo(int codigo, HttpResponse<String> respuesta, String motivo) {
        estado(codigo, respuesta);
        Object error = objeto(respuesta.body()).get("error");
        assertTrue(error instanceof String mensaje && !mensaje.isBlank() && mensaje.contains(motivo),
                "Error contractual esperado, sin imprimir cuerpo");
    }

    private static Map<String, Object> objeto(String cuerpo) {
        try { return JSONObjectUtils.parse(cuerpo); }
        catch (ParseException e) { return fail("Respuesta no es objeto JSON valido (cuerpo omitido)"); }
    }

    private static List<Object> lista(String cuerpo) {
        try { return JSONArrayUtils.parse(cuerpo); }
        catch (ParseException e) { return fail("Respuesta no es lista JSON valida (cuerpo omitido)"); }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        assertTrue(valor instanceof Map<?, ?>, "Objeto JSON requerido");
        return (Map<String, Object>) valor;
    }

    private static long numero(Map<String, Object> doc, String clave) {
        Object valor = doc.get(clave);
        assertTrue(valor instanceof Number, "Campo numerico requerido: " + clave);
        return ((Number) valor).longValue();
    }

    private static String requerida(String nombre) {
        String valor = System.getenv(nombre);
        assertTrue(valor != null && !valor.isBlank(), "Falta configurar " + nombre);
        return valor;
    }

    private static String token(String nombre) {
        String valor = requerida(nombre);
        assertTrue(valor.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c)),
                "Token sin espacios ni caracteres de control (valor omitido)");
        return valor;
    }

    private static long id(String nombre) {
        try {
            long valor = Long.parseLong(requerida(nombre));
            assertTrue(valor > 0, "ID positivo requerido: " + nombre);
            return valor;
        } catch (NumberFormatException e) {
            return fail("ID invalido en " + nombre + " (valor omitido)");
        }
    }
}
