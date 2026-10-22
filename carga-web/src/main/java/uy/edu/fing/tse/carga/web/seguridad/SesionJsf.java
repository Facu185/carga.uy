package uy.edu.fing.tse.carga.web.seguridad;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/** Adaptacion de transporte del JWT para el navegador JSF; no contiene reglas de negocio. */
public final class SesionJsf {
    public static final String COOKIE = "cargauy_jsf";
    public static final String INGRESO = "/ingreso-jsf.xhtml";
    public static final String CAMBIO = "/cambiar-clave-jsf.xhtml";
    private static final Set<String> RUTAS = Set.of("/index.xhtml", "/empresas.xhtml", "/vehiculos.xhtml",
            "/choferes.xhtml", INGRESO, CAMBIO, "/jsf/ingresar", "/jsf/cambiar-clave", "/jsf/salir");

    private SesionJsf() { }

    public static boolean esRuta(HttpServletRequest pedido) {
        return RUTAS.contains(pedido.getServletPath());
    }

    public static boolean esPublica(HttpServletRequest pedido) {
        return Set.of("/index.xhtml", INGRESO, "/jsf/ingresar", "/jsf/salir").contains(pedido.getServletPath());
    }

    public static String token(HttpServletRequest pedido) {
        Cookie[] cookies = pedido.getCookies();
        if (cookies == null) return null;
        String token = null;
        boolean encontrada = false;
        for (Cookie cookie : cookies) {
            if (!COOKIE.equals(cookie.getName())) continue;
            // No elegir arbitrariamente entre cookies homonimas de distintos paths.
            if (encontrada) return null;
            encontrada = true;
            token = cookie.getValue();
        }
        return token == null || token.isBlank() ? null : token;
    }

    public static void guardar(HttpServletRequest pedido, HttpServletResponse respuesta, String token) {
        respuesta.addCookie(cookie(pedido, token));
    }

    public static void borrar(HttpServletRequest pedido, HttpServletResponse respuesta) {
        Cookie cookie = cookie(pedido, "");
        cookie.setMaxAge(0);
        respuesta.addCookie(cookie);
    }

    private static Cookie cookie(HttpServletRequest pedido, String token) {
        Cookie cookie = new Cookie(COOKIE, token);
        cookie.setPath(pedido.getContextPath().isEmpty() ? "/" : pedido.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setSecure(pedido.isSecure());
        cookie.setAttribute("SameSite", "Strict");
        // Cookie de navegador; la expiracion y revocacion efectivas las decide validarToken en P1.
        return cookie;
    }

    public static void noGuardarCache(HttpServletResponse respuesta) {
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("X-Frame-Options", "DENY");
        respuesta.setHeader("Referrer-Policy", "same-origin");
    }

    /** SameSite no sustituye la comprobacion de origen, tampoco para login o salida. */
    public static boolean origenPermitido(HttpServletRequest pedido) {
        if (Set.of("GET", "HEAD", "OPTIONS").contains(pedido.getMethod())) return true;
        String origen = pedido.getHeader("Origin");
        String referencia = origen != null ? origen : pedido.getHeader("Referer");
        if (referencia == null) return false;
        try {
            URI uri = URI.create(referencia);
            return uri.getHost() != null && uri.getUserInfo() == null
                    && pedido.getScheme().equalsIgnoreCase(uri.getScheme())
                    && pedido.getServerName().equalsIgnoreCase(uri.getHost())
                    && pedido.getServerPort() == puerto(uri);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static int puerto(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    /** JSF AJAX necesita una partial-response, no un 302 con HTML de la pagina de ingreso. */
    public static AuthenticationStatus redirigir(HttpServletRequest pedido, HttpServletResponse respuesta, String pagina) {
        String destino = pedido.getContextPath() + pagina;
        if ("partial/ajax".equals(pedido.getHeader("Faces-Request"))) {
            respuesta.setStatus(HttpServletResponse.SC_OK);
            respuesta.setContentType("text/xml;charset=UTF-8");
            String xml = destino.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
            try {
                respuesta.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<partial-response><redirect url=\"" + xml + "\"/></partial-response>");
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo enviar la redireccion JSF", e);
            }
        } else {
            respuesta.setStatus(HttpServletResponse.SC_SEE_OTHER);
            respuesta.setHeader("Location", destino);
        }
        return AuthenticationStatus.SEND_CONTINUE;
    }
}
