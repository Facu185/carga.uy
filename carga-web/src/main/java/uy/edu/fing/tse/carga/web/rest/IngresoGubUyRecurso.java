package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import uy.edu.fing.tse.carga.api.IngresoGubUyLocal;
import uy.edu.fing.tse.carga.api.NegocioException;
import uy.edu.fing.tse.carga.api.dto.RedireccionDTO;

import java.net.URI;

/**
 * Ingreso con gub.uy (web y app movil). Toda la logica esta en IngresoGubUyLocal;
 * aca solo se traducen sus resultados a redirecciones y a la cookie de la transaccion.
 *
 * GET /auth/gubuy/iniciar?destino=web|movil   redirige a gub.uy
 * GET /auth/gubuy/callback                     vuelta de gub.uy, redirige al frontend con la sesion
 */
@Path("/auth/gubuy")
public class IngresoGubUyRecurso {

    static final String COOKIE = "cargauy_oidc";

    @EJB
    private IngresoGubUyLocal ingreso;

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("/iniciar")
    public Response iniciar(@QueryParam("destino") @DefaultValue("web") String destino) throws NegocioException {
        return redirigir(ingreso.iniciar(destino));
    }

    @GET
    @Path("/callback")
    public Response callback(@QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @QueryParam("error") String error,
                             @CookieParam(COOKIE) String transaccion) {
        return redirigir(ingreso.completar(code, state, error, transaccion));
    }

    /** Sin transaccion en el resultado, la cookie se borra (maxAge 0). */
    private Response redirigir(RedireccionDTO r) {
        boolean guardar = r.getTransaccion() != null;
        return Response.seeOther(URI.create(r.getUrl()))
                .cookie(cookie(guardar ? r.getTransaccion() : "", guardar ? r.getVigenciaSegundos() : 0))
                .build();
    }

    private NewCookie cookie(String valor, int maxAge) {
        return new NewCookie.Builder(COOKIE)
                .value(valor)
                .path(uriInfo.getBaseUri().getPath() + "auth/gubuy")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(ingreso.cookieSegura())
                // Lax: el navegador la manda cuando gub.uy redirige de vuelta (navegacion GET)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }
}
