package uy.edu.fing.tse.carga.ejb.integracion;

import jakarta.enterprise.context.ApplicationScoped;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;

import java.net.URI;

/** Solo configuracion del servidor; ningun destino proviene del navegador. */
@ApplicationScoped
public class ConfiguracionGeografica {
    public URI ideBase() {
        return https("IDE_DIRECCIONES_URL", "https://direcciones.ide.uy/api/v1/geocode");
    }

    public URI orsRuta() {
        URI uri = https("ORS_RUTAS_URL", "https://api.heigit.org/openrouteservice/v2/directions/driving-hgv/geojson");
        validarPerfilOrs(uri);
        return uri;
    }

    static void validarPerfilOrs(URI uri) {
        if (uri.getPath() == null || !uri.getPath().endsWith("/driving-hgv/geojson"))
            throw new ServicioNoDisponibleException("El calculo por peso requiere el perfil driving-hgv de OpenRouteService.");
    }

    public String claveOrs() {
        String clave = System.getenv("ORS_API_KEY");
        return clave == null ? "" : clave.trim();
    }

    private static URI https(String variable, String defecto) {
        String valor = System.getenv(variable);
        try {
            URI uri = URI.create(valor == null || valor.isBlank() ? defecto : valor.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) throw new IllegalArgumentException();
            return uri;
        } catch (IllegalArgumentException e) {
            throw new ServicioNoDisponibleException("La configuracion del servicio geografico no es valida.");
        }
    }
}
