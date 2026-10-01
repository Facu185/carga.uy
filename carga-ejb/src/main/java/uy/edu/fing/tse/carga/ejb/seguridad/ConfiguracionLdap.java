package uy.edu.fing.tse.carga.ejb.seguridad;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/** Configuracion del directorio LDAP, toda por variables de entorno. */
@ApplicationScoped
public class ConfiguracionLdap {

    private String url;
    private String usuariosDn;
    private String gruposDn;
    private String bindDn;
    private String bindPassword;
    private String grupoAdministradoresDn;
    private String grupoAutoridadesDn;

    @PostConstruct
    void cargar() {
        if (!habilitado()) {
            return;
        }
        configurar(
                obligatoria("LDAP_URL"),
                obligatoria("LDAP_USUARIOS_DN"),
                obligatoria("LDAP_GRUPOS_DN"),
                obligatoria("LDAP_BIND_DN"),
                obligatoria("LDAP_BIND_PASSWORD"),
                obligatoria("LDAP_GRUPO_ADMINISTRADORES_DN"),
                obligatoria("LDAP_GRUPO_AUTORIDADES_DN"));
    }

    /** Separado de cargar() para poder configurarlo en los tests sin variables de entorno. */
    void configurar(String url, String usuariosDn, String gruposDn, String bindDn, String bindPassword,
                    String grupoAdministradoresDn, String grupoAutoridadesDn) {
        this.url = url;
        this.usuariosDn = usuariosDn;
        this.gruposDn = gruposDn;
        this.bindDn = bindDn;
        this.bindPassword = bindPassword;
        this.grupoAdministradoresDn = grupoAdministradoresDn;
        this.grupoAutoridadesDn = grupoAutoridadesDn;
    }

    /** El interruptor: "ldap" para autenticar el backoffice contra el directorio; cualquier otro valor, la base. */
    public static boolean habilitado() {
        return "ldap".equalsIgnoreCase(System.getenv("AUTENTICACION_INTERNA"));
    }

    public String getUrl() { return url; }
    public String getUsuariosDn() { return usuariosDn; }
    public String getGruposDn() { return gruposDn; }
    public String getBindDn() { return bindDn; }
    public String getBindPassword() { return bindPassword; }
    public String getGrupoAdministradoresDn() { return grupoAdministradoresDn; }
    public String getGrupoAutoridadesDn() { return grupoAutoridadesDn; }

    private static String obligatoria(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre + " (AUTENTICACION_INTERNA=ldap)");
        }
        return valor.trim();
    }
}
