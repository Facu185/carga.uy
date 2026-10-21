package uy.edu.fing.tse.carga.web.seguridad;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dobles HTTP: no acreditan propagacion de identidad ni interceptores de WildFly. */
class HttpJsfDoble {
    String ruta = "/empresas.xhtml";
    String metodo = "GET";
    boolean seguro;
    Cookie[] cookies;
    Set<String> roles = Set.of();
    String usuario;
    String autenticado;
    Set<String> rolesNotificados;
    int estado = 200;
    String redireccion;
    final Map<String, String> headers = new HashMap<>();
    final Map<String, String> parametros = new HashMap<>();
    final Map<String, String> salidaHeaders = new HashMap<>();
    final List<Cookie> salidaCookies = new ArrayList<>();
    final StringWriter cuerpo = new StringWriter();

    final HttpServletRequest pedido = doble(HttpServletRequest.class, (nombre, args) -> switch (nombre) {
        case "getRequestURI" -> "/carga" + ruta;
        case "getContextPath" -> "/carga";
        case "getServletPath" -> ruta;
        case "getMethod" -> metodo;
        case "getHeader" -> headers.get((String) args[0]);
        case "getParameter" -> parametros.get((String) args[0]);
        case "getCookies" -> cookies;
        case "getScheme" -> seguro ? "https" : "http";
        case "getServerName" -> "localhost";
        case "getServerPort" -> seguro ? 443 : 8080;
        case "isSecure" -> seguro;
        case "getUserPrincipal" -> usuario == null ? null : (Principal) () -> usuario;
        case "isUserInRole" -> roles.contains(args[0]);
        case "getSession" -> {
            if (args == null || args.length == 0 || Boolean.TRUE.equals(args[0])) {
                throw new AssertionError("La autenticacion no debe crear HttpSession");
            }
            yield null;
        }
        case "setCharacterEncoding", "logout" -> null;
        default -> throw new UnsupportedOperationException(nombre);
    });

    final HttpServletResponse respuesta = doble(HttpServletResponse.class, (nombre, args) -> switch (nombre) {
        case "setStatus", "sendError" -> { estado = (int) args[0]; yield null; }
        case "setHeader", "addHeader" -> { salidaHeaders.put((String) args[0], (String) args[1]); yield null; }
        case "setContentType", "setCharacterEncoding" -> null;
        case "addCookie" -> { salidaCookies.add((Cookie) args[0]); yield null; }
        case "getWriter" -> new PrintWriter(cuerpo);
        case "sendRedirect" -> { estado = 302; redireccion = (String) args[0]; yield null; }
        default -> throw new UnsupportedOperationException(nombre);
    });

    final HttpMessageContext contexto = doble(HttpMessageContext.class, (nombre, args) -> switch (nombre) {
        case "doNothing" -> AuthenticationStatus.NOT_DONE;
        case "responseUnauthorized" -> { estado = 401; yield AuthenticationStatus.SEND_FAILURE; }
        case "notifyContainerAboutLogin" -> {
            autenticado = (String) args[0];
            @SuppressWarnings("unchecked") Set<String> notificados = (Set<String>) args[1];
            rolesNotificados = notificados;
            yield AuthenticationStatus.SUCCESS;
        }
        default -> throw new UnsupportedOperationException(nombre);
    });

    interface Respuesta { Object obtener(String nombre, Object[] args) throws Throwable; }

    static <T> T doble(Class<T> tipo, Respuesta respuestas) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo},
                (proxy, metodo, args) -> respuestas.obtener(metodo.getName(), args)));
    }
}
