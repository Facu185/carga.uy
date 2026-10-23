package uy.edu.fing.tse.carga.ejb.seguridad;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Configuracion de la integracion con gub.uy (ID Uruguay), toda por variables de entorno.
 * Compose base usa simulador; docker-compose.gubuy-testing.yml prepara ID Uruguay testing
 * con secreto obligatorio en .env. La conexion real requiere validacion integrada.
 */
@ApplicationScoped
public class ConfiguracionGubUy {

    private String issuer;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationEndpoint;
    private String scopes;
    private String urlWeb;
    private String urlMovil;

    @PostConstruct
    void cargar() {
        configurar(
                obligatoria("GUBUY_ISSUER"),
                obligatoria("GUBUY_CLIENT_ID"),
                obligatoria("GUBUY_CLIENT_SECRET"),
                obligatoria("GUBUY_REDIRECT_URI"),
                // Solo para el simulador en Docker: el navegador no resuelve el nombre interno del contenedor
                opcional("GUBUY_AUTHORIZATION_ENDPOINT", null),
                opcional("GUBUY_SCOPES", "openid personal_info email document"),
                obligatoria("FRONTEND_URL_WEB"),
                opcional("FRONTEND_URL_MOVIL", "cargauy://ingreso"));
    }

    /** Separado de cargar() para poder configurarlo en los tests sin variables de entorno. */
    void configurar(String issuer, String clientId, String clientSecret, String redirectUri,
                    String authorizationEndpoint, String scopes, String urlWeb, String urlMovil) {
        this.issuer = issuer;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizationEndpoint = authorizationEndpoint;
        this.scopes = scopes;
        this.urlWeb = urlWeb;
        this.urlMovil = urlMovil;
    }

    /** Lista blanca de destinos despues del ingreso: evita que alguien use el callback para redirigir a cualquier sitio. */
    public Optional<String> urlDestino(String destino) {
        if ("web".equals(destino)) {
            return Optional.of(urlWeb + "/ingreso");
        }
        if ("movil".equals(destino)) {
            return Optional.of(urlMovil);
        }
        return Optional.empty();
    }

    public boolean cookieSegura() {
        return redirectUri.startsWith("https://");
    }

    public String getIssuer() { return issuer; }
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getRedirectUri() { return redirectUri; }
    public Optional<String> getAuthorizationEndpoint() { return Optional.ofNullable(authorizationEndpoint); }
    public String getScopes() { return scopes; }

    private static String obligatoria(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre + " (integraci\u00f3n gub.uy)");
        }
        return valor.trim();
    }

    private static String opcional(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }
}
