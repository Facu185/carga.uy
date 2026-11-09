package uy.edu.fing.tse.carga.ejb.integracion;

import com.nimbusds.jose.util.JSONObjectUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.dto.planificacion.PuntoMapaDTO;
import uy.edu.fing.tse.carga.api.dto.planificacion.RutaPreviaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Peso total en toneladas; no simula restricciones no solicitadas ni rutas exitosas. */
@ApplicationScoped
public class ClienteRutasOrs {
    private final CircuitBreaker circuito = new CircuitBreaker("ORS rutas", 3, Duration.ofSeconds(30), Clock.systemUTC());
    @Inject ConfiguracionGeografica configuracion;
    @Inject ClienteGeograficoHttp http;

    public RutaPreviaDTO calcular(PuntoMapaDTO origen, PuntoMapaDTO destino, BigDecimal pesoTotalKg) throws NegocioException {
        String clave = configuracion.claveOrs();
        if (clave.isBlank()) throw new ServicioNoDisponibleException("El calculo de rutas no esta configurado en el servidor.");
        if (!circuito.permiteLlamada()) throw indisponible();
        try {
            String cuerpo = JSONObjectUtils.toJSONString(Map.of(
                    "coordinates", List.of(List.of(origen.getLongitud(), origen.getLatitud()), List.of(destino.getLongitud(), destino.getLatitud())),
                    "instructions", false, "optimized", false,
                    "options", Map.of("vehicle_type", "hgv", "profile_params",
                            Map.of("restrictions", Map.of("weight", pesoTotalKg.movePointLeft(3))))));
            var respuesta = http.solicitar(configuracion.orsRuta(), "POST", cuerpo, clave);
            if (respuesta.estado() != 200) {
                if ((respuesta.estado() == 400 || respuesta.estado() == 404) && esSinRuta(respuesta.cuerpo())) {
                    circuito.registrarExito(); throw sinRuta();
                }
                throw indisponible();
            }
            var raiz = JsonGeografico.leerObjeto(respuesta.cuerpo());
            var features = JsonGeografico.lista(raiz.get("features"));
            if (features.isEmpty()) { circuito.registrarExito(); throw sinRuta(); }
            var feature = JsonGeografico.objeto(features.getFirst());
            var geometria = JsonGeografico.objeto(feature.get("geometry"));
            if (!"LineString".equals(geometria.get("type"))) throw new IllegalArgumentException();
            var coordenadas = JsonGeografico.lista(geometria.get("coordinates"));
            if (coordenadas.size() < 2 || coordenadas.size() > 25000) throw new IllegalArgumentException();
            var puntos = new ArrayList<PuntoMapaDTO>();
            for (Object valor : coordenadas) {
                var par = JsonGeografico.lista(valor);
                if (par.size() < 2) throw new IllegalArgumentException();
                puntos.add(JsonGeografico.punto(par.get(1), par.get(0)));
            }
            var propiedades = JsonGeografico.objeto(feature.get("properties"));
            var resumen = JsonGeografico.objeto(propiedades.get("summary"));
            var dto = new RutaPreviaDTO(); dto.setPuntos(puntos);
            dto.setDistanciaMetros(JsonGeografico.cantidad(resumen.get("distance")));
            dto.setDuracionSegundos(JsonGeografico.cantidad(resumen.get("duration")));
            dto.setPesoTotalKg(pesoTotalKg);
            dto.setAdvertenciasProveedor(propiedades.get("warnings") instanceof List<?> l && !l.isEmpty());
            circuito.registrarExito(); return dto;
        } catch (IllegalArgumentException | ServicioNoDisponibleException e) {
            circuito.registrarFalla(); throw indisponible();
        }
    }

    private static boolean esSinRuta(String json) {
        try {
            Object codigo = JsonGeografico.objeto(JsonGeografico.leerObjeto(json).get("error")).get("code");
            return codigo instanceof Number n && (n.intValue() == 2009 || n.intValue() == 2010);
        } catch (IllegalArgumentException e) { return false; }
    }
    private static NegocioException sinRuta() {
        return new NegocioException("No se encontro un recorrido con esos puntos y peso. Revise las direcciones; no se reemplazara por una ruta sin restricciones.");
    }
    private static ServicioNoDisponibleException indisponible() {
        return new ServicioNoDisponibleException("No se pudo calcular el recorrido en OpenRouteService. Reintente mas tarde; la vista previa no es obligatoria para registrar.");
    }
}
