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

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private volatile Metadatos metadatos;

    private record Metadatos(String authorizationEndpoint, String tokenEndpoint, String userInfoEndpoint,
                             JWKSource<SecurityContext> claves) {
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
        String accessToken;
        String tipoToken;
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
            Map<String, Object> tokens = JSONObjectUtils.parse(respuesta.body());
            idToken = JSONObjectUtils.getString(tokens, "id_token");
            accessToken = JSONObjectUtils.getString(tokens, "access_token");
            tipoToken = JSONObjectUtils.getString(tokens, "token_type");
        } catch (ParseException e) {
            // La causa de parseo puede contener tokens: no incluirla en los logs del llamador.
            throw new GubUyException("La respuesta de tokens de gub.uy no es v\u00e1lida");
        } catch (IOException e) {
            throw new GubUyException("gub.uy no est\u00e1 disponible en este momento", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GubUyException("Se interrumpi\u00f3 el ingreso con gub.uy", e);
        }
        if (idToken == null || idToken.isBlank()) {
            throw new GubUyException("gub.uy no devolvi\u00f3 un id_token");
        }
        String subject = validarIdToken(idToken, nonceEsperado, m);
        if (accessToken == null || accessToken.isBlank() || !"Bearer".equalsIgnoreCase(tipoToken)) {
            throw new GubUyException("gub.uy no devolvi\u00f3 un access_token de tipo Bearer");
        }
        return consultarUserInfo(accessToken, subject, m);
    }

    private String validarIdToken(String idToken, String nonceEsperado, Metadatos m) {
        DefaultJWTProcessor<SecurityContext> procesador = new DefaultJWTProcessor<>();
        procesador.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, m.claves()));
        // Verifica firma, iss, aud (= nuestro client_id), exp y que el nonce sea el que mandamos
        procesador.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                config.getClientId(),
                new JWTClaimsSet.Builder().issuer(config.getIssuer()).claim("nonce", nonceEsperado).build(),
                Set.of("sub", "iat", "exp")));
        try {
            JWTClaimsSet claims = procesador.process(idToken, null);
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new GubUyException("La identidad recibida de gub.uy no tiene subject");
            }
            return subject;
        } catch (ParseException | BadJOSEException | JOSEException e) {
            throw new GubUyException("La identidad recibida de gub.uy no es v\u00e1lida", e);
        }
    }

    private IdentidadGubUy consultarUserInfo(String accessToken, String subject, Metadatos m) {
        try (Traza.Tramo tramo = Traza.iniciar("gub.uy GET /userinfo", Traza.Tipo.CLIENTE)
                .atributo("peer.service", "gub.uy")) {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(m.userInfoEndpoint()))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header(Traza.HEADER, tramo.traceparent())
                    .GET()
                    .build();
            HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            tramo.atributo("http.status_code", respuesta.statusCode());
            if (respuesta.statusCode() != 200) {
                tramo.marcarError();
                throw new GubUyException("No se pudieron obtener los datos de gub.uy (c\u00f3digo "
                        + respuesta.statusCode() + ")");
            }
            Map<String, Object> datos = JSONObjectUtils.parse(respuesta.body());
            // No basta una respuesta HTTP exitosa: los datos deben pertenecer al ID token validado.
            if (!subject.equals(JSONObjectUtils.getString(datos, "sub"))) {
                throw new GubUyException("Los datos de gub.uy no corresponden a la identidad autenticada");
            }
            String documento = JSONObjectUtils.getString(datos, "numero_documento");
            if (documento == null || documento.isBlank()) {
                throw new GubUyException("gub.uy no devolvi\u00f3 el documento de la persona");
            }
            return new IdentidadGubUy(subject, documento,
                    JSONObjectUtils.getString(datos, "email"),
                    JSONObjectUtils.getString(datos, "nombre_completo"));
        } catch (ParseException | IllegalArgumentException e) {
            // No propagar el cuerpo ni valores personales que pudiera incluir el error de parseo.
            throw new GubUyException("La respuesta de UserInfo de gub.uy no es v\u00e1lida");
        } catch (IOException e) {
            throw new GubUyException("No se pudieron consultar los datos de gub.uy en este momento", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GubUyException("Se interrumpi\u00f3 la consulta de datos de gub.uy", e);
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
            String userInfo = endpointUserInfo(doc);
            String jwks = JSONObjectUtils.getString(doc, "jwks_uri");
            JWKSource<SecurityContext> claves = JWKSourceBuilder
                    .create(URI.create(jwks).toURL(), new DefaultResourceRetriever(TIMEOUT_JWKS_MS, TIMEOUT_JWKS_MS))
                    .build();
            return new Metadatos(autorizacion, token, userInfo, claves);
        } catch (IOException | ParseException | IllegalArgumentException e) {
            throw new GubUyException("gub.uy no est\u00e1 disponible en este momento", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GubUyException("Se interrumpi\u00f3 la conexi\u00f3n con gub.uy", e);
        }
    }

    private String endpointUserInfo(Map<String, Object> doc) throws ParseException {
        String endpoint = JSONObjectUtils.getString(doc, "userinfo_endpoint");
        if (endpoint == null || endpoint.isBlank()) {
            throw new GubUyException("gub.uy no public\u00f3 un endpoint UserInfo");
        }
        URI uri = URI.create(endpoint);
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        // HTTP solo conserva el entorno local existente; un issuer HTTPS no puede degradar a HTTP.
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                || !(https || "http".equalsIgnoreCase(uri.getScheme()))
                || (config.getIssuer().startsWith("https://") && !https)) {
            throw new GubUyException("El endpoint UserInfo de gub.uy no es v\u00e1lido");
        }
        return endpoint;
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
