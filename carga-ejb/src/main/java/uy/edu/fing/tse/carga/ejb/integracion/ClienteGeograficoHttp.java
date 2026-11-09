package uy.edu.fing.tse.carga.ejb.integracion;

import jakarta.enterprise.context.ApplicationScoped;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Transporte acotado; no registra URLs, cuerpos, direcciones ni credenciales. */
@ApplicationScoped
public class ClienteGeograficoHttp {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final int conexionMs;
    private final int lecturaMs;

    public ClienteGeograficoHttp() { this(3000, 10000); }
    ClienteGeograficoHttp(int conexionMs, int lecturaMs) {
        this.conexionMs = conexionMs; this.lecturaMs = lecturaMs;
    }

    public record Respuesta(int estado, String cuerpo) { }

    public Respuesta solicitar(URI destino, String metodo, String cuerpo, String clave) {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) destino.toURL().openConnection();
            conexion.setConnectTimeout(conexionMs);
            conexion.setReadTimeout(lecturaMs);
            conexion.setInstanceFollowRedirects(false);
            conexion.setRequestMethod(metodo);
            conexion.setRequestProperty("Accept", "application/json, application/geo+json");
            conexion.setRequestProperty("User-Agent", "carga.uy-planificacion/1.0");
            String correlacion = Correlacion.actual().orElse(null);
            if (correlacion != null) conexion.setRequestProperty(Correlacion.HEADER, correlacion);
            if (clave != null) conexion.setRequestProperty("Authorization", clave);
            if (cuerpo != null) {
                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conexion.setFixedLengthStreamingMode(bytes.length);
                try (var salida = conexion.getOutputStream()) { salida.write(bytes); }
            }
            int estado = conexion.getResponseCode();
            // No leer paginas de redireccion ni permitir que el token siga Location.
            if (estado >= 300 && estado < 400) return new Respuesta(estado, "");
            if (conexion.getContentLengthLong() > MAX_BYTES) throw indisponible();
            InputStream cuerpoRespuesta = estado >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            if (cuerpoRespuesta == null) return new Respuesta(estado, "");
            try (InputStream entrada = cuerpoRespuesta) {
                byte[] bytes = entrada.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw indisponible();
                return new Respuesta(estado, new String(bytes, StandardCharsets.UTF_8));
            }
        } catch (IOException | IllegalArgumentException e) {
            // Ni causa ni mensaje externos: pueden contener ubicaciones o credenciales.
            throw indisponible();
        } finally {
            if (conexion != null) conexion.disconnect();
        }
    }

    private static ServicioNoDisponibleException indisponible() {
        return new ServicioNoDisponibleException("El servicio geografico no esta disponible. Intente nuevamente mas tarde.");
    }
}
