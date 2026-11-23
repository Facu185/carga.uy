package uy.edu.fing.tse.carga.ejb.seguridad;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Configuracion de la integracion con gub.uy (ID Uruguay), toda por variables de entorno.
 * Compose base usa el Keycloak de desarrollo (ADR-0026); docker-compose.gubuy-testing.yml prepara
 * ID Uruguay testing con secreto obligatorio en .env.
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
    private String discoveryUrl;

    @PostConstruct
    void cargar() {
        configurar(
                obligatoria("GUBUY_ISSUER"),
                obligatoria("GUBUY_CLIENT_ID"),
                obligatoria("GUBUY_CLIENT_SECRET"),
                obligatoria("GUBUY_REDIRECT_URI"),
                // Opcional: reemplaza el endpoint de autorizacion del discovery (con Keycloak no hace falta)
                opcional("GUBUY_AUTHORIZATION_ENDPOINT", null),
                opcional("GUBUY_SCOPES", "openid personal_info email document"),
                obligatoria("FRONTEND_URL_WEB"),
                opcional("FRONTEND_URL_MOVIL", "cargauy://ingreso"));
        // Solo si el proveedor tiene una URL publica (el emisor) y otra interna: el Keycloak en Docker firma con
        // la URL por la que entra el navegador, pero el central lo alcanza por la red de contenedores.
        usarDiscovery(opcional("GUBUY_DISCOVERY_URL", null));
    }

    /** null = el estandar: {issuer}/.well-known/openid-configuration. */
    void usarDiscovery(String url) {
        this.discoveryUrl = url;
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

    /** De donde se leen los endpoints y las claves. El emisor que se valida en el id_token sigue siendo el issuer. */
    public String getDiscoveryUrl() {
        return discoveryUrl != null ? discoveryUrl : issuer + "/.well-known/openid-configuration";
    }

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
