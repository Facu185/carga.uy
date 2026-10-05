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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Levanta un gub.uy falso (discovery, JWKS y token) en un puerto local y verifica que el central
 * valide de verdad el id_token: firma, audiencia, nonce y vencimiento.
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
    private volatile String cuerpoUserinfo;
    private volatile String autorizacionUserinfo;

    @BeforeEach
    void levantarGubUyFalso() throws Exception {
        clave = new RSAKeyGenerator(2048).keyID("k1").generate();
        claveFirma = clave;
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        issuer = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/gubuy";

        servidor.createContext("/gubuy/.well-known/openid-configuration", ex -> responder(ex, 200,
                "{\"authorization_endpoint\":\"" + issuer + "/authorize\","
                        + "\"token_endpoint\":\"" + issuer + "/token\","
                        + "\"userinfo_endpoint\":\"" + issuer + "/userinfo\","
                        + "\"jwks_uri\":\"" + issuer + "/jwks\"}"));
        servidor.createContext("/gubuy/userinfo", ex -> {
            autorizacionUserinfo = ex.getRequestHeaders().getFirst("Authorization");
            responder(ex, 200, cuerpoUserinfo);
        });
        servidor.createContext("/gubuy/jwks", ex -> responder(ex, 200, new JWKSet(clave.toPublicJWK()).toString()));
        servidor.createContext("/gubuy/token", ex -> {
            cuerpoToken = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            autorizacionToken = ex.getRequestHeaders().getFirst("Authorization");
            if (estadoToken != 200) {
                responder(ex, estadoToken, "{\"error\":\"invalid_grant\"}");
                return;
            }
            responder(ex, 200, "{\"access_token\":\"at-1\",\"token_type\":\"Bearer\",\"id_token\":\""
                    + firmar(claimsAEmitir, claveFirma) + "\"}");
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
                .claim("numero_documento", "12345672")
                .claim("email", "ana@correo.uy")
                .claim("nombre_completo", "Ana P\u00e9rez")
                .build();
    }

    /** Como ID Uruguay: el id_token solo identifica (sub); los datos de la persona van por UserInfo. */
    private JWTClaimsSet claimsSoloIdentificacion(String nonce) {
        Date ahora = new Date();
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("uid-1")
                .audience(CLIENT_ID)
                .issueTime(ahora)
                .expirationTime(new Date(ahora.getTime() + 300_000))
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
        assertEquals("ana@correo.uy", identidad.email());
        assertEquals("Ana P\u00e9rez", identidad.nombre());
        assertTrue(cuerpoToken.contains("code=codigo-1"));
        assertTrue(cuerpoToken.contains("code_verifier=verifier-1"));
        assertTrue(autorizacionToken.startsWith("Basic "), "el secreto viaja en el header, no en la URL");
    }

    @Test
    void siElIdTokenNoTraeLosDatosLosPideAlUserinfoComoIdUruguay() {
        claimsAEmitir = claimsSoloIdentificacion("nonce-1");
        cuerpoUserinfo = "{\"sub\":\"uid-1\",\"numero_documento\":\"12345672\",\"primer_nombre\":\"Ana\","
                + "\"primer_apellido\":\"P\u00e9rez\",\"email\":\"ana@correo.uy\"}";

        IdentidadGubUy identidad = cliente(issuer, null).canjearCodigo("codigo-1", "verifier-1", "nonce-1");

        assertEquals("12345672", identidad.documento());
        assertEquals("Ana P\u00e9rez", identidad.nombre());
        assertEquals("ana@correo.uy", identidad.email());
        assertEquals("Bearer at-1", autorizacionUserinfo);
    }

    @Test
    void rechazaUnUserinfoDeOtraPersona() {
        claimsAEmitir = claimsSoloIdentificacion("nonce-1");
        cuerpoUserinfo = "{\"sub\":\"uid-OTRO\",\"numero_documento\":\"12345672\",\"nombre_completo\":\"X\"}";

        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
    }

    @Test
    void rechazaUnNonceDistinto() {
        claimsAEmitir = claims(CLIENT_ID, "nonce-de-otro-ingreso", 300);
        assertThrows(GubUyException.class, () -> cliente(issuer, null).canjearCodigo("c", "v", "nonce-1"));
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
