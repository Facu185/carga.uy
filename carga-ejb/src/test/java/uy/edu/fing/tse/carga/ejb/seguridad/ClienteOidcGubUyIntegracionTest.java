package uy.edu.fing.tse.carga.ejb.seguridad;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proveedor local con discovery, JWKS, token y UserInfo; no consulta ID Uruguay real.
 * Verifica el id_token y la vinculacion de los datos personales de UserInfo con su subject.
 */
class ClienteOidcGubUyIntegracionTest {

    private static final String CLIENT_ID = "carga-uy-test";
    private static final String REDIRECT = "http://localhost:3000/carga/api/auth/gubuy/callback";

    private HttpServer servidor;
    private String issuer;
    private RSAKey clave;
    private volatile RSAKey claveFirma;
    private volatile JWTClaimsSet claimsAEmitir;
    private volatile int estadoToken = 200;
    private volatile String cuerpoToken;
    private volatile String autorizacionToken;
    private volatile String accessToken = "acceso-sintetico";
    private volatile String tipoToken = "Bearer";
    private volatile int estadoUserInfo = 200;
    private volatile String respuestaUserInfo = "{\"sub\":\"uid-1\",\"numero_documento\":\"12345672\","
            + "\"email\":\"ana@example.test\",\"nombre_completo\":\"Ana Prueba\"}";
    private volatile String autorizacionUserInfo;
    private volatile String metodoUserInfo;
    private volatile String queryUserInfo;
    private volatile String endpointUserInfo;
    private final AtomicInteger consultasUserInfo = new AtomicInteger();

    @BeforeEach
    void levantarGubUyFalso() throws Exception {
        clave = new RSAKeyGenerator(2048).keyID("k1").generate();
        claveFirma = clave;
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        issuer = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/gubuy";
        endpointUserInfo = issuer + "/userinfo";

        servidor.createContext("/gubuy/.well-known/openid-configuration", ex -> responder(ex, 200,
                "{\"issuer\":\"" + issuer + "\",\"authorization_endpoint\":\"" + issuer + "/authorize\","
                        + "\"token_endpoint\":\"" + issuer + "/token\","
                        + (endpointUserInfo == null ? "" : "\"userinfo_endpoint\":\"" + endpointUserInfo + "\",")
                        + "\"jwks_uri\":\"" + issuer + "/jwks\"}"));
        servidor.createContext("/gubuy/jwks", ex -> responder(ex, 200, new JWKSet(clave.toPublicJWK()).toString()));
        servidor.createContext("/gubuy/token", ex -> {
            cuerpoToken = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            autorizacionToken = ex.getRequestHeaders().getFirst("Authorization");
            if (estadoToken != 200) {
                responder(ex, estadoToken, "{\"error\":\"invalid_grant\"}");
                return;
            }
            responder(ex, 200, "{\"id_token\":\"" + firmar(claimsAEmitir, claveFirma) + "\","
                    + (accessToken == null ? "" : "\"access_token\":\"" + accessToken + "\",")
                    + "\"token_type\":\"" + tipoToken + "\"}");
        });
        servidor.createContext("/gubuy/userinfo", ex -> {
            consultasUserInfo.incrementAndGet();
            autorizacionUserInfo = ex.getRequestHeaders().getFirst("Authorization");
            metodoUserInfo = ex.getRequestMethod();
            queryUserInfo = ex.getRequestURI().getRawQuery();
            responder(ex, estadoUserInfo, respuestaUserInfo);
        });
        servidor.start();
    }

    @AfterEach
    void bajar() {
        servidor.stop(0);
    }

    // ---------------- utilidades ----------------

    private static void responder(HttpExchange ex, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private static String firmar(JWTClaimsSet claims, RSAKey conClave) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(), claims);
            jwt.sign(new RSASSASigner(conClave));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private JWTClaimsSet claims(String audiencia, String nonce, long segundosHastaVencer) {
        Date ahora = new Date();
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("uid-1")
                .audience(audiencia)
                .issueTime(ahora)
                .expirationTime(new Date(ahora.getTime() + segundosHastaVencer * 1000))
                .claim("nonce", nonce)
                .build();
    }

    private ClienteOidcGubUy cliente(String issuerConfigurado, String endpointAutorizacion) {
        ConfiguracionGubUy config = new ConfiguracionGubUy();
        config.configurar(issuerConfigurado, CLIENT_ID, "secreto-cliente", REDIRECT, endpointAutorizacion,
                "openid personal_info email document", "http://localhost:3000", "cargauy://ingreso");
        ClienteOidcGubUy cliente = new ClienteOidcGubUy();
        cliente.config = config;
        return cliente;
    }

    // ---------------- canje del codigo ----------------

    @Test
    void canjeaElCodigoYDevuelveLaIdentidad() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);

        IdentidadGubUy identidad = cliente(issuer, null).canjearCodigo("codigo-1", "verifier-1", "nonce-1");

        assertEquals("12345672", identidad.documento());
        assertEquals("uid-1", identidad.sub());
        assertEquals("ana@example.test", identidad.email());
        assertEquals("Ana Prueba", identidad.nombre());
        assertTrue(cuerpoToken.contains("code=codigo-1"));
        assertTrue(cuerpoToken.contains("code_verifier=verifier-1"));
        assertTrue(autorizacionToken.startsWith("Basic "), "el secreto viaja en el header, no en la URL");
        assertEquals("Bearer acceso-sintetico", autorizacionUserInfo);
        assertEquals("GET", metodoUserInfo);
        assertNull(queryUserInfo, "el access_token no debe viajar en la URL");
        assertEquals(1, consultasUserInfo.get());
    }

    @Test
    void usaUserInfoAunqueElIdTokenTambienTengaDatosPersonales() {
        claimsAEmitir = new JWTClaimsSet.Builder(claims(CLIENT_ID, "nonce-1", 300))
                .claim("numero_documento", "99999999")
                .claim("email", "otro@example.test")
                .claim("nombre_completo", "Nombre del token")
                .build();

        IdentidadGubUy identidad = cliente(issuer, null).canjearCodigo("c", "v", "nonce-1");

        assertEquals("12345672", identidad.documento());
        assertEquals("ana@example.test", identidad.email());
        assertEquals("Ana Prueba", identidad.nombre());
    }

    @Test
    void rechazaUserInfoDeOtraPersona() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        respuestaUserInfo = "{\"sub\":\"otro-uid\",\"numero_documento\":\"12345672\"}";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void rechazaUserInfoSinSubject() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        respuestaUserInfo = "{\"numero_documento\":\"12345672\"}";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void rechazaUserInfoSinDocumentoSinRecurrirAlIdToken() {
        claimsAEmitir = new JWTClaimsSet.Builder(claims(CLIENT_ID, "nonce-1", 300))
                .claim("numero_documento", "12345672").build();
        respuestaUserInfo = "{\"sub\":\"uid-1\"}";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void conservaNombreYCorreoOpcionales() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        respuestaUserInfo = "{\"sub\":\"uid-1\",\"numero_documento\":\"12345672\"}";

        IdentidadGubUy identidad = cliente(issuer, null).canjearCodigo("c", "v", "nonce-1");

        assertEquals("12345672", identidad.documento());
        assertNull(identidad.nombre());
        assertNull(identidad.email());
    }

    @Test
    void rechazaUserInfoConJsonInvalidoSinExponerElCuerpo() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        respuestaUserInfo = "contenido-personal-no-json";

        GubUyException error = assertThrows(GubUyException.class,
                () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));

        assertFalse(error.toString().contains(respuestaUserInfo));
        assertNull(error.getCause(), "no registrar cuerpos personales en la causa de parseo");
    }

    @Test
    void rechazaUserInfoConClaimDeTipoIncorrecto() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        respuestaUserInfo = "{\"sub\":\"uid-1\",\"numero_documento\":12345672}";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void noCreaIdentidadSiUserInfoRechazaElAccessToken() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        estadoUserInfo = 401;

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void noCreaIdentidadSiUserInfoEstaCaido() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        estadoUserInfo = 503;

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void noSigueRedireccionesDeUserInfo() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        servidor.removeContext("/gubuy/userinfo");
        servidor.createContext("/gubuy/userinfo", ex -> {
            ex.getResponseHeaders().set("Location", issuer + "/otro-destino");
            responder(ex, 302, "{}");
        });
        servidor.createContext("/gubuy/otro-destino", ex -> {
            consultasUserInfo.incrementAndGet();
            responder(ex, 200, respuestaUserInfo);
        });

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertEquals(0, consultasUserInfo.get(), "no reenviar credenciales al destino de una redireccion");
    }

    @Test
    void rechazaCanjeSinAccessToken() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        accessToken = null;

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertEquals(0, consultasUserInfo.get());
    }

    @Test
    void rechazaCanjeConAccessTokenVacio() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        accessToken = "";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertEquals(0, consultasUserInfo.get());
    }

    @Test
    void rechazaTipoDeTokenNoBearer() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        tipoToken = "otro";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertEquals(0, consultasUserInfo.get());
    }

    @Test
    void requiereUserInfoEnDiscovery() {
        endpointUserInfo = null;

        assertThrows(GubUyException.class, () -> cliente(issuer, null).urlAutorizacion("e", "n", "v"));
    }

    @Test
    void rechazaEndpointUserInfoRelativo() {
        endpointUserInfo = "/userinfo";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).urlAutorizacion("e", "n", "v"));
    }

    @Test
    void rechazaEndpointUserInfoConEsquemaNoHttp() {
        endpointUserInfo = "file:///datos-personales";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).urlAutorizacion("e", "n", "v"));
    }

    @Test
    void usaElEndpointUserInfoPublicadoEnDiscovery() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        endpointUserInfo = issuer + "/datos-persona";
        servidor.removeContext("/gubuy/userinfo");
        servidor.createContext("/gubuy/datos-persona", ex -> responder(ex, 200, respuestaUserInfo));

        IdentidadGubUy identidad = cliente(issuer, null).canjearCodigo("c", "v", "nonce-1");

        assertEquals("12345672", identidad.documento());
    }

    @Test
    void aceptaTipoBearerSinDependerDeMayusculas() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        tipoToken = "bearer";

        assertEquals("12345672", cliente(issuer, null).canjearCodigo("c", "v", "nonce-1").documento());
    }

    @Test
    void rechazaIdTokenDeOtroEmisorAntesDeConsultarUserInfo() {
        claimsAEmitir = new JWTClaimsSet.Builder(claims(CLIENT_ID, "nonce-1", 300))
                .issuer("https://otro-emisor.example.test").build();

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertEquals(0, consultasUserInfo.get());
    }

    @Test
    void rechazaIdTokenSinSubjectAntesDeConsultarUserInfo() {
        claimsAEmitir = new JWTClaimsSet.Builder(claims(CLIENT_ID, "nonce-1", 300)).subject(null).build();

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertEquals(0, consultasUserInfo.get());
    }

    @Test
    void rechazaUnNonceDistinto() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-de-otro-ingreso", 300);
        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertEquals(0, consultasUserInfo.get(), "validar id_token antes de consultar datos personales");
    }

    @Test
    void rechazaUnTokenEmitidoParaOtraAplicacion() {
        claimsAEmitir = claims("otra-aplicacion", "nonce-1", 300);
        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void rechazaUnTokenVencido() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", -600);
        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() throws Exception {
        claimsAEmitir = claims(CLIENT_ID, "nonce-1", 300);
        claveFirma = new RSAKeyGenerator(2048).keyID("k1").generate();
        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertEquals(0, consultasUserInfo.get());
    }

    @Test
    void siGubUyRechazaElCodigoAvisa() {
        estadoToken = 400;
        GubUyException e = assertThrows(GubUyException.class,
                () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
        assertTrue(e.getMessage().contains("rechaz\u00f3"));
    }

    // ---------------- URL de autorizacion ----------------

    @Test
    void armaLaUrlDeAutorizacionConPkce() {
        String url = cliente(issuer, null).urlAutorizacion("estado-1", "nonce-1", "verifier-1");

        assertTrue(url.startsWith(issuer + "/authorize?"));
        assertTrue(url.contains("client_id=" + CLIENT_ID));
        assertTrue(url.contains("state=estado-1"));
        assertTrue(url.contains("nonce=nonce-1"));
        assertTrue(url.contains("code_challenge=" + ClienteOidcGubUy.desafioPkce("verifier-1")));
        assertTrue(url.contains("code_challenge_method=S256"));
        assertFalse(url.contains("verifier-1"), "el verifier nunca sale del central");
    }

    @Test
    void usaElEndpointConfiguradoSiLoHay() {
        String url = cliente(issuer, "http://localhost:8085/gubuy/authorize").urlAutorizacion("e", "n", "v");
        assertTrue(url.startsWith("http://localhost:8085/gubuy/authorize?"));
    }

    @Test
    void siGubUyNoRespondeAvisaQueNoEstaDisponible() {
        assertThrows(GubUyException.class,
                () -> cliente("http://127.0.0.1:1/gubuy", null).urlAutorizacion("e", "n", "v"));
    }

    // ---------------- configuracion ----------------

    @Test
    void soloRedirigeADestinosConocidos() {
        ConfiguracionGubUy config = cliente(issuer, null).config;
        assertEquals("http://localhost:3000/ingreso", config.urlDestino("web").orElseThrow());
        assertEquals("cargauy://ingreso", config.urlDestino("movil").orElseThrow());
        assertTrue(config.urlDestino("https://sitio-malicioso.com").isEmpty());
        assertFalse(config.cookieSegura(), "en local es http");
    }
}
