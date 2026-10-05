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

    /** userinfoEndpoint puede ser null: solo hace falta si el id_token no trae los datos de la persona. */
    private record Metadatos(String issuer, String authorizationEndpoint, String tokenEndpoint, String userinfoEndpoint,
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
        // HTTP Basic con client_id:client_secret tal cual, como en el ejemplo de la documentacion de ID Uruguay
        String credenciales = Base64.getEncoder().encodeToString(
                (config.getClientId() + ":" + config.getClientSecret()).getBytes(StandardCharsets.UTF_8));

        String idToken;
        String accessToken;
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
        } catch (IOException | ParseException e) {
            throw new GubUyException("gub.uy no est\u00e1 disponible en este momento", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GubUyException("Se interrumpi\u00f3 el ingreso con gub.uy", e);
        }
        if (idToken == null) {
            throw new GubUyException("gub.uy no devolvi\u00f3 un id_token");
        }
        IdentidadGubUy identidad = validarIdToken(idToken, nonceEsperado, m);
        return identidad.estaCompleta() ? identidad : completarConUserinfo(identidad, accessToken, m);
    }

    /**
     * ID Uruguay entrega los datos de la persona (documento, nombre, correo) en el UserInfo Endpoint;
     * el id_token puede traer solo el "sub". Si faltan, se piden ahi con el access_token. Segun la
     * especificacion, el "sub" de UserInfo tiene que ser el mismo del id_token: si no, se rechaza.
     */
    private IdentidadGubUy completarConUserinfo(IdentidadGubUy identidad, String accessToken, Metadatos m) {
        if (m.userinfoEndpoint() == null || accessToken == null) {
            return identidad;
        }
        try (Traza.Tramo tramo = Traza.iniciar("gub.uy GET /userinfo", Traza.Tipo.CLIENTE)
                .atributo("peer.service", "gub.uy")) {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(m.userinfoEndpoint()))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken)
                    .header(Traza.HEADER, tramo.traceparent())
                    .GET()
                    .build();
            HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            tramo.atributo("http.status_code", respuesta.statusCode());
            if (respuesta.statusCode() != 200) {
                tramo.marcarError();
                throw new GubUyException("gub.uy no entreg\u00f3 los datos de la persona (c\u00f3digo "
                        + respuesta.statusCode() + ")");
            }
            Map<String, Object> datos = JSONObjectUtils.parse(respuesta.body());
            if (!identidad.sub().equals(JSONObjectUtils.getString(datos, "sub"))) {
                throw new GubUyException("Los datos recibidos de gub.uy no corresponden a la persona que ingres\u00f3");
            }
            return new IdentidadGubUy(identidad.sub(),
                    primero(identidad.documento(), JSONObjectUtils.getString(datos, "numero_documento")),
                    primero(identidad.email(), JSONObjectUtils.getString(datos, "email")),
                    primero(identidad.nombre(), nombreDe(datos)));
        } catch (IOException | ParseException e) {
            throw new GubUyException("gub.uy no est\u00e1 disponible en este momento", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GubUyException("Se interrumpi\u00f3 el ingreso con gub.uy", e);
        }
    }

    /** nombre_completo (scope personal_info); si no viene, primer nombre y primer apellido, o name (profile). */
    private static String nombreDe(Map<String, Object> datos) throws ParseException {
        String completo = JSONObjectUtils.getString(datos, "nombre_completo");
        if (completo != null && !completo.isBlank()) {
            return completo;
        }
        String nombre = JSONObjectUtils.getString(datos, "primer_nombre");
        String apellido = JSONObjectUtils.getString(datos, "primer_apellido");
        if (nombre != null || apellido != null) {
            return ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).trim();
        }
        return JSONObjectUtils.getString(datos, "name");
    }

    private static String primero(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private IdentidadGubUy validarIdToken(String idToken, String nonceEsperado, Metadatos m) {
        DefaultJWTProcessor<SecurityContext> procesador = new DefaultJWTProcessor<>();
        procesador.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, m.claves()));
        // Verifica firma, iss, aud (= nuestro client_id), exp y que el nonce sea el que mandamos
        procesador.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                config.getClientId(),
                new JWTClaimsSet.Builder().issuer(m.issuer()).claim("nonce", nonceEsperado).build(),
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
            String userinfo = JSONObjectUtils.getString(doc, "userinfo_endpoint");
            // El "iss" de los id_token es el que anuncia el propio proveedor (en ID Uruguay puede no ser
            // exactamente la URL configurada: por eso existe su discovery v2)
            String emisor = JSONObjectUtils.getString(doc, "issuer");
            JWKSource<SecurityContext> claves = JWKSourceBuilder
                    .create(URI.create(jwks).toURL(), new DefaultResourceRetriever(TIMEOUT_JWKS_MS, TIMEOUT_JWKS_MS))
                    .build();
            return new Metadatos(emisor == null ? config.getIssuer() : emisor, autorizacion, token, userinfo, claves);
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
