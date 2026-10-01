package uy.edu.fing.tse.carga.ejb.seguridad;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Emite y valida los JWT propios de carga.uy (HS256).
 * El secreto viene de la variable de entorno JWT_SECRET: nunca del repositorio.
 * Al no guardar sesion en el servidor, cualquier instancia del central puede validar el token
 * (escalabilidad horizontal).
 *
 * Firma dos tipos de token, distinguidos por el claim "tipo" para que uno no se pueda usar como el otro:
 * - "acceso": el que usa el frontend / la app en cada request.
 * - "oidc": datos temporales del ingreso por gub.uy (state, nonce, PKCE), viajan en una cookie.
 */
@ApplicationScoped
public class TokenService {

    static final String EMISOR = "carga.uy";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_DATOS = "datos";
    private static final String CLAIM_ORIGEN = "origen";
    private static final String CLAIM_VERSION = "ver";
    private static final String CLAIM_CAMBIAR_PASSWORD = "cpw";

    /** De donde viene la sesion: define contra que se verifica que siga vigente. */
    public static final String ORIGEN_INTERNO = "interno";
    public static final String ORIGEN_GUBUY = "gubuy";
    private static final String TIPO_ACCESO = "acceso";
    private static final String TIPO_OIDC = "oidc";
    private static final int LARGO_MINIMO_SECRETO = 32; // HS256 exige al menos 256 bits

    private byte[] secreto;
    private Duration duracion;

    public TokenService() {
        // CDI
    }

    TokenService(String secreto, Duration duracion) {
        configurar(secreto, duracion);
    }

    @PostConstruct
    void inicializar() {
        String minutos = System.getenv("JWT_DURACION_MINUTOS");
        Duration d = Duration.ofMinutes(minutos == null || minutos.isBlank() ? 30 : Long.parseLong(minutos));
        configurar(System.getenv("JWT_SECRET"), d);
    }

    private void configurar(String secretoTexto, Duration duracionToken) {
        if (secretoTexto == null || secretoTexto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException("JWT_SECRET no configurado o con menos de " + LARGO_MINIMO_SECRETO + " caracteres");
        }
        this.secreto = secretoTexto.getBytes(StandardCharsets.UTF_8);
        this.duracion = duracionToken;
    }

    public record TokenEmitido(String token, Instant expiraEn) {
    }

    /**
     * version: version de credenciales del usuario interno al emitir el token (0 para gub.uy).
     * debeCambiarPassword: sesion con contrasena temporal, solo puede cambiarla.
     */
    public record DatosToken(String usuario, Set<String> roles, String origen, int version, boolean debeCambiarPassword) {
    }

    // ---------------- token de acceso ----------------

    public TokenEmitido emitir(String usuario, Set<String> roles, String origen, int version,
                               boolean debeCambiarPassword) {
        Instant expira = Instant.now().plus(duracion);
        JWTClaimsSet.Builder claims = base(TIPO_ACCESO, expira)
                .subject(usuario)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .claim(CLAIM_ORIGEN, origen)
                .claim(CLAIM_VERSION, version);
        if (debeCambiarPassword) {
            claims.claim(CLAIM_CAMBIAR_PASSWORD, true);
        }
        return new TokenEmitido(firmar(claims.build()), expira);
    }

    public Optional<DatosToken> validar(String token) {
        return verificar(token, TIPO_ACCESO).map(claims -> {
            try {
                List<String> roles = claims.getStringListClaim(CLAIM_ROLES);
                Integer version = claims.getIntegerClaim(CLAIM_VERSION);
                Boolean cambiar = claims.getBooleanClaim(CLAIM_CAMBIAR_PASSWORD);
                return new DatosToken(claims.getSubject(), roles == null ? Set.of() : new HashSet<>(roles),
                        claims.getStringClaim(CLAIM_ORIGEN), version == null ? 0 : version, Boolean.TRUE.equals(cambiar));
            } catch (ParseException e) {
                return null;
            }
        });
    }

    // ---------------- transaccion de ingreso gub.uy ----------------

    public String firmarTransaccion(Map<String, String> datos, Duration vigencia) {
        JWTClaimsSet claims = base(TIPO_OIDC, Instant.now().plus(vigencia))
                .claim(CLAIM_DATOS, Map.copyOf(datos))
                .build();
        return firmar(claims);
    }

    public Optional<Map<String, String>> leerTransaccion(String token) {
        return verificar(token, TIPO_OIDC).map(claims -> {
            try {
                Map<String, Object> crudo = claims.getJSONObjectClaim(CLAIM_DATOS);
                if (crudo == null) {
                    return null;
                }
                Map<String, String> datos = new HashMap<>();
                crudo.forEach((k, v) -> datos.put(k, String.valueOf(v)));
                return datos;
            } catch (ParseException e) {
                return null;
            }
        });
    }

    // ---------------- comun ----------------

    private JWTClaimsSet.Builder base(String tipo, Instant expira) {
        return new JWTClaimsSet.Builder()
                .issuer(EMISOR)
                .issueTime(new Date())
                .expirationTime(Date.from(expira))
                .jwtID(UUID.randomUUID().toString())
                .claim(CLAIM_TIPO, tipo);
    }

    private String firmar(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    private Optional<JWTClaimsSet> verificar(String token, String tipoEsperado) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new MACVerifier(secreto))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiracion = claims.getExpirationTime();
            boolean valido = EMISOR.equals(claims.getIssuer())
                    && tipoEsperado.equals(claims.getStringClaim(CLAIM_TIPO))
                    && expiracion != null && expiracion.after(new Date());
            return valido ? Optional.of(claims) : Optional.empty();
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }
}
