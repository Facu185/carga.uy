package uy.edu.fing.tse.carga.ejb.seguridad;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import uy.edu.fing.tse.carga.api.observabilidad.Traza;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * Cliente OpenID Connect (Authorization Code + PKCE) contra gub.uy.
 * El client_secret nunca sale del servidor: el navegador y la app movil solo ven nuestro JWT final.
 * Todas las llamadas tienen timeout: si gub.uy no responde, el central sigue funcionando
 * y el usuario ve un mensaje de "no disponible" (comportamiento degradado).
 */
@ApplicationScoped
public class ClienteOidcGubUy {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int TIMEOUT_JWKS_MS = 5_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    ConfiguracionGubUy config;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private volatile Metadatos metadatos;

    private record Metadatos(String authorizationEndpoint, String tokenEndpoint, JWKSource<SecurityContext> claves) {
    }

    // ---------------- paso 1: redirigir a gub.uy ----------------

    public String urlAutorizacion(String state, String nonce, String codeVerifier) {
        Metadatos m = metadatos();
        return m.authorizationEndpoint()
                + "?response_type=code"
                + "&client_id=" + enc(config.getClientId())
                + "&redirect_uri=" + enc(config.getRedirectUri())
                + "&scope=" + enc(config.getScopes())
                + "&state=" + enc(state)
                + "&nonce=" + enc(nonce)
                + "&code_challenge=" + enc(desafioPkce(codeVerifier))
                + "&code_challenge_method=S256";
    }

    // ---------------- paso 2: canjear el code y validar el id_token ----------------

    public IdentidadGubUy canjearCodigo(String code, String codeVerifier, String nonceEsperado) {
        Metadatos m = metadatos();
        String cuerpo = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(config.getRedirectUri())
                + "&code_verifier=" + enc(codeVerifier);
        String credenciales = Base64.getEncoder().encodeToString(
                (enc(config.getClientId()) + ":" + enc(config.getClientSecret())).getBytes(StandardCharsets.UTF_8));

        String idToken;
        // Tramo de cliente de la traza: cuanto tarda gub.uy en canjear el codigo
        try (Traza.Tramo tramo = Traza.iniciar("gub.uy POST /token", Traza.Tipo.CLIENTE)
                .atributo("peer.service", "gub.uy")) {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(m.tokenEndpoint()))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + credenciales)
                    .header(Traza.HEADER, tramo.traceparent())
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();
            HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            tramo.atributo("http.status_code", respuesta.statusCode());
            if (respuesta.statusCode() != 200) {
                tramo.marcarError();
                throw new GubUyException("gub.uy rechaz\u00f3 el ingreso (c\u00f3digo " + respuesta.statusCode() + ")");
            }
            idToken = JSONObjectUtils.getString(JSONObjectUtils.parse(respuesta.body()), "id_token");
        } catch (IOException | ParseException e) {
            throw new GubUyException("gub.uy no est\u00e1 disponible en este momento", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GubUyException("Se interrumpi\u00f3 el ingreso con gub.uy", e);
        }
        if (idToken == null) {
            throw new GubUyException("gub.uy no devolvi\u00f3 un id_token");
        }
        return validarIdToken(idToken, nonceEsperado, m);
    }

    private IdentidadGubUy validarIdToken(String idToken, String nonceEsperado, Metadatos m) {
        DefaultJWTProcessor<SecurityContext> procesador = new DefaultJWTProcessor<>();
        procesador.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, m.claves()));
        // Verifica firma, iss, aud (= nuestro client_id), exp y que el nonce sea el que mandamos
        procesador.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                config.getClientId(),
                new JWTClaimsSet.Builder().issuer(config.getIssuer()).claim("nonce", nonceEsperado).build(),
                Set.of("sub", "iat", "exp")));
        try {
            JWTClaimsSet claims = procesador.process(idToken, null);
            return new IdentidadGubUy(
                    claims.getSubject(),
                    claims.getStringClaim("numero_documento"),
                    claims.getStringClaim("email"),
                    claims.getStringClaim("nombre_completo"));
        } catch (ParseException | BadJOSEException | JOSEException e) {
            throw new GubUyException("La identidad recibida de gub.uy no es v\u00e1lida", e);
        }
    }

    // ---------------- discovery (se carga una vez y queda en memoria) ----------------

    private Metadatos metadatos() {
        Metadatos actual = metadatos;
        if (actual != null) {
            return actual;
        }
        synchronized (this) {
            if (metadatos == null) {
                metadatos = cargarMetadatos();
            }
            return metadatos;
        }
    }

    private Metadatos cargarMetadatos() {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(config.getIssuer() + "/.well-known/openid-configuration"))
                .timeout(TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                throw new GubUyException("gub.uy no est\u00e1 disponible en este momento");
            }
            Map<String, Object> doc = JSONObjectUtils.parse(respuesta.body());
            String autorizacion = config.getAuthorizationEndpoint()
                    .orElse(JSONObjectUtils.getString(doc, "authorization_endpoint"));
            String token = JSONObjectUtils.getString(doc, "token_endpoint");
            String jwks = JSONObjectUtils.getString(doc, "jwks_uri");
            JWKSource<SecurityContext> claves = JWKSourceBuilder
                    .create(URI.create(jwks).toURL(), new DefaultResourceRetriever(TIMEOUT_JWKS_MS, TIMEOUT_JWKS_MS))
                    .build();
            return new Metadatos(autorizacion, token, claves);
        } catch (IOException | ParseException | IllegalArgumentException e) {
            throw new GubUyException("gub.uy no est\u00e1 disponible en este momento", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GubUyException("Se interrumpi\u00f3 la conexi\u00f3n con gub.uy", e);
        }
    }

    // ---------------- utilidades ----------------

    /** Valor aleatorio no adivinable para state, nonce y code_verifier. */
    public static String aleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** PKCE S256: el desafio es el SHA-256 del verifier en base64url. */
    static String desafioPkce(String codeVerifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
