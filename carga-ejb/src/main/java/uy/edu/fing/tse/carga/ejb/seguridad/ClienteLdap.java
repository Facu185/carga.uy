package uy.edu.fing.tse.carga.ejb.seguridad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;
import uy.edu.fing.tse.carga.ejb.modelo.Rol;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.time.Duration;
import java.util.Hashtable;
import java.util.Optional;

/**
 * Autenticacion contra un directorio LDAP (letra, seguridad opcional #8).
 *
 * Patron "search + bind": nunca se lee ni se compara la contrasena a mano.
 * 1. Con una cuenta de servicio de SOLO LECTURA se busca a la persona por su uid y se ve a que grupo pertenece.
 * 2. Se intenta abrir una conexion nueva autenticada con el DN encontrado y la contrasena que escribio
 *    la persona. Si el directorio la acepta, la contrasena es correcta; si no, lanza AuthenticationException.
 * Quien no esta en el grupo de administradores ni en el de autoridades no tiene rol interno: se rechaza.
 * Son dos conexiones por ingreso: una de la cuenta de servicio y otra de la persona.
 *
 * javax.naming.Context no implementa AutoCloseable (JDK-7057061): los contextos se cierran en un finally.
 */
@ApplicationScoped
public class ClienteLdap {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Inject
    ConfiguracionLdap config;

    /** Lo que la cuenta de servicio averigua antes de validar la contrasena. */
    private record Persona(String dn, String nombre, Rol rol) {
    }

    public Optional<IdentidadLdap> autenticar(String uid, String password) {
        // Una contrasena vacia en LDAP es un "bind no autenticado" (RFC 4513): muchos servidores lo ACEPTAN
        // como conexion anonima. Se corta aca, antes de hablar con el directorio.
        if (uid == null || uid.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }
        Optional<Persona> persona = buscarConCuentaDeServicio(uid);
        if (persona.isEmpty() || persona.get().rol() == null) {
            return Optional.empty(); // no existe, o existe pero sin rol interno en carga.uy
        }
        if (!validarContrasena(persona.get().dn(), password)) {
            return Optional.empty();
        }
        return Optional.of(new IdentidadLdap(uid, persona.get().nombre(), persona.get().rol()));
    }

    // ---------------- paso 1: cuenta de servicio ----------------

    private Optional<Persona> buscarConCuentaDeServicio(String uid) {
        DirContext contexto = null;
        try {
            contexto = new InitialDirContext(propiedades(config.getBindDn(), config.getBindPassword()));
            SearchControls controles = new SearchControls();
            controles.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            controles.setCountLimit(1);
            controles.setReturningAttributes(new String[]{"cn"});
            NamingEnumeration<SearchResult> resultados = contexto.search(
                    config.getUsuariosDn(), "(uid=" + escaparFiltro(uid) + ")", controles);
            try {
                if (!resultados.hasMore()) {
                    return Optional.empty();
                }
                SearchResult encontrado = resultados.next();
                // DN completo y bien formado tal como lo da el directorio (no armado a mano)
                String dn = encontrado.getNameInNamespace();
                Attribute cn = encontrado.getAttributes().get("cn");
                String nombre = cn == null ? null : String.valueOf(cn.get());
                return Optional.of(new Persona(dn, nombre, rolPorGrupo(contexto, dn)));
            } finally {
                resultados.close();
            }
        } catch (NamingException e) {
            throw new ServicioNoDisponibleException("El directorio LDAP no est\u00e1 disponible en este momento", e);
        } finally {
            cerrar(contexto);
        }
    }

    private Rol rolPorGrupo(DirContext contexto, String dnPersona) throws NamingException {
        if (esMiembroDe(contexto, config.getGrupoAdministradoresDn(), dnPersona)) {
            return Rol.ADMINISTRADOR;
        }
        if (esMiembroDe(contexto, config.getGrupoAutoridadesDn(), dnPersona)) {
            return Rol.AUTORIDAD;
        }
        return null;
    }

    /** Pregunta al grupo si tiene a la persona como miembro: el directorio compara los DN, no nosotros. */
    private static boolean esMiembroDe(DirContext contexto, String grupoDn, String dnPersona) throws NamingException {
        SearchControls controles = new SearchControls();
        controles.setSearchScope(SearchControls.OBJECT_SCOPE);
        controles.setCountLimit(1);
        controles.setReturningAttributes(new String[0]);
        NamingEnumeration<SearchResult> resultado = contexto.search(
                grupoDn, "(member=" + escaparFiltro(dnPersona) + ")", controles);
        try {
            return resultado.hasMore();
        } finally {
            resultado.close();
        }
    }

    // ---------------- paso 2: la persona ----------------

    private boolean validarContrasena(String dn, String password) {
        DirContext contexto = null;
        try {
            contexto = new InitialDirContext(propiedades(dn, password));
            return true;
        } catch (AuthenticationException e) {
            return false; // contrasena incorrecta: no es una falla del directorio
        } catch (NamingException e) {
            throw new ServicioNoDisponibleException("El directorio LDAP no est\u00e1 disponible en este momento", e);
        } finally {
            cerrar(contexto);
        }
    }

    // ---------------- utilidades ----------------

    /** Context.close() no esta en AutoCloseable; un fallo al cerrar no debe tapar el error real. */
    private static void cerrar(Context contexto) {
        if (contexto == null) {
            return;
        }
        try {
            contexto.close();
        } catch (NamingException ignorada) {
            // cerrar es best-effort: la operacion principal ya termino
        }
    }

    private Hashtable<String, String> propiedades(String dn, String password) {
        Hashtable<String, String> propiedades = new Hashtable<>();
        propiedades.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        propiedades.put(Context.PROVIDER_URL, config.getUrl());
        propiedades.put(Context.SECURITY_AUTHENTICATION, "simple");
        propiedades.put(Context.SECURITY_PRINCIPAL, dn);
        propiedades.put(Context.SECURITY_CREDENTIALS, password);
        propiedades.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(TIMEOUT.toMillis()));
        propiedades.put("com.sun.jndi.ldap.read.timeout", String.valueOf(TIMEOUT.toMillis()));
        return propiedades;
    }

    /**
     * Escapa un valor para usarlo en un filtro LDAP (RFC 4515): evita que alguien inyecte
     * "*", "(", ")" o "\" y altere la busqueda (el equivalente LDAP de una inyeccion SQL).
     */
    static String escaparFiltro(String valor) {
        StringBuilder resultado = new StringBuilder();
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '\\' -> resultado.append("\\5c");
                case '*' -> resultado.append("\\2a");
                case '(' -> resultado.append("\\28");
                case ')' -> resultado.append("\\29");
                case '\0' -> resultado.append("\\00");
                default -> resultado.append(c);
            }
        }
        return resultado.toString();
    }
}
