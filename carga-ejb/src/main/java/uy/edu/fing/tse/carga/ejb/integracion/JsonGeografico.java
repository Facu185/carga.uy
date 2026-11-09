package uy.edu.fing.tse.carga.ejb.integracion;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jose.util.JSONArrayUtils;
import uy.edu.fing.tse.carga.api.dto.planificacion.PuntoMapaDTO;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/** Frontera JSON de proveedores; no conserva excepciones con el cuerpo externo. */
final class JsonGeografico {
    private JsonGeografico() { }

    static Map<String, Object> leerObjeto(String json) {
        try {
            var mapa = JSONObjectUtils.parse(json);
            if (mapa == null) throw new IllegalArgumentException("Objeto geografico nulo.");
            return mapa;
        }
        catch (ParseException e) { throw new IllegalArgumentException("Respuesta geografica invalida."); }
    }

    static List<?> leerLista(String json) {
        try {
            var datos = JSONArrayUtils.parse(json);
            if (datos == null) throw new IllegalArgumentException("Lista geografica nula.");
            return datos;
        }
        catch (ParseException e) { throw new IllegalArgumentException("Lista geografica invalida."); }
    }

    static Map<?, ?> objeto(Object valor) {
        if (valor instanceof Map<?, ?> mapa) return mapa;
        throw new IllegalArgumentException("Se esperaba un objeto.");
    }

    static List<?> lista(Object valor) {
        if (valor instanceof List<?> lista) return lista;
        throw new IllegalArgumentException("Se esperaba una lista.");
    }

    static String texto(Object valor, int maximo) {
        if (valor instanceof String s && !s.isBlank() && s.length() <= maximo) return s.trim();
        throw new IllegalArgumentException("Texto geografico invalido.");
    }

    static double numero(Object valor) {
        if (valor instanceof Number n && Double.isFinite(n.doubleValue())) return n.doubleValue();
        throw new IllegalArgumentException("Numero geografico invalido.");
    }

    static BigDecimal cantidad(Object valor) {
        if (numero(valor) < 0) throw new IllegalArgumentException("Cantidad negativa.");
        return new BigDecimal(valor.toString());
    }

    static long entero(Object valor) {
        double n = numero(valor);
        if (n <= 0 || n != Math.rint(n) || n > 9007199254740991d)
            throw new IllegalArgumentException("Identificador invalido.");
        return ((Number) valor).longValue();
    }

    static PuntoMapaDTO punto(Object latitud, Object longitud) {
        double lat = numero(latitud), lon = numero(longitud);
        if (Math.abs(lat) > 90 || Math.abs(lon) > 180 || (lat == 0 && lon == 0))
            throw new IllegalArgumentException("Coordenadas invalidas.");
        var punto = new PuntoMapaDTO(); punto.setLatitud(lat); punto.setLongitud(lon); return punto;
    }
}
