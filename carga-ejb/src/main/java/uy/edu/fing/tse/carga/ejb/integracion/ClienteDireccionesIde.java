package uy.edu.fing.tse.carga.ejb.integracion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.dto.planificacion.DireccionMapaDTO;
import uy.edu.fing.tse.carga.api.dto.planificacion.SugerenciaCalleDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.ServicioNoDisponibleException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Consulta y resolucion separadas: candidates no proporciona siempre coordenadas. */
@ApplicationScoped
public class ClienteDireccionesIde {
    private static final Set<String> TIPOS = Set.of("CALLE", "CALLEyPORTAL", "ESQUINA");
    private final CircuitBreaker circuito = new CircuitBreaker("IDE direcciones", 3, Duration.ofSeconds(30), Clock.systemUTC());
    @Inject ConfiguracionGeografica configuracion;
    @Inject ClienteGeograficoHttp http;

    public List<SugerenciaCalleDTO> sugerir(String texto) throws NegocioException {
        if (texto == null || texto.trim().length() < 3 || texto.trim().length() > 200)
            throw new NegocioException("Ingrese entre 3 y 200 caracteres para buscar una calle.");
        if (!circuito.permiteLlamada()) throw indisponible();
        try {
            var respuesta = http.solicitar(uri("candidates?q=" + enc(texto.trim()) + "&limit=5"), "GET", null, null);
            if (respuesta.estado() != 200) throw indisponible();
            List<?> datos = JsonGeografico.leerLista(respuesta.cuerpo());
            if (datos.size() > 50) throw new IllegalArgumentException();
            var resultado = new ArrayList<SugerenciaCalleDTO>();
            for (Object valor : datos) {
                var item = JsonGeografico.objeto(valor);
                if (!(item.get("type") instanceof String tipo)) throw new IllegalArgumentException();
                if (!TIPOS.contains(tipo)) continue;
                var s = new SugerenciaCalleDTO();
                s.setDireccion(JsonGeografico.texto(item.get("address"), 200));
                s.setTipo(tipo);
                s.setIdCalle(JsonGeografico.entero(item.get("idCalle")));
                if ("CALLEyPORTAL".equals(s.getTipo())) {
                    long portal = JsonGeografico.entero(item.get("portalNumber"));
                    if (portal > Integer.MAX_VALUE) throw new IllegalArgumentException();
                    s.setNumeroPuerta((int) portal);
                    if (item.get("letra") instanceof String letra && !letra.isBlank())
                        s.setLetra(JsonGeografico.texto(letra, 10));
                }
                if ("ESQUINA".equals(s.getTipo())) s.setIdCalleEsquina(JsonGeografico.entero(item.get("idCalleEsq")));
                resultado.add(s);
                if (resultado.size() == 5) break;
            }
            circuito.registrarExito(); return resultado;
        } catch (IllegalArgumentException | ServicioNoDisponibleException e) {
            circuito.registrarFalla(); throw indisponible();
        }
    }

    public DireccionMapaDTO resolver(SugerenciaCalleDTO s) throws NegocioException {
        return resolver(s, false).getFirst();
    }

    public List<DireccionMapaDTO> resolverAlternativas(SugerenciaCalleDTO s) throws NegocioException {
        return resolver(s, true);
    }

    private List<DireccionMapaDTO> resolver(SugerenciaCalleDTO s, boolean alternativas) throws NegocioException {
        validarSeleccion(s);
        String query = "find?type=" + ("ESQUINA".equals(s.getTipo()) ? "esquina" : "calle") + "&idcalle=" + s.getIdCalle();
        if ("CALLEyPORTAL".equals(s.getTipo())) {
            query += "&portal=" + s.getNumeroPuerta();
            if (s.getLetra() != null && !s.getLetra().isBlank()) query += "&letra=" + enc(s.getLetra().trim());
        }
        if ("ESQUINA".equals(s.getTipo())) query += "&idcalleEsq=" + s.getIdCalleEsquina();
        if (!circuito.permiteLlamada()) throw indisponible();
        try {
            var respuesta = http.solicitar(uri(query), "GET", null, null);
            if (respuesta.estado() != 200) throw indisponible();
            var datos = JsonGeografico.leerLista(respuesta.cuerpo());
            if (!alternativas && datos.size() != 1) {
                circuito.registrarExito(); throw sinPunto();
            }
            if (datos.size() > 50) throw new IllegalArgumentException();
            var resultado = new ArrayList<DireccionMapaDTO>();
            for (Object valor : datos) {
                var item = JsonGeografico.objeto(valor);
                // Un centro de calle no es una alternativa al portal solicitado.
                if (alternativas && "CALLEyPORTAL".equals(s.getTipo()) && "CALLE".equals(item.get("type"))) continue;
                resultado.add(direccion(item, s));
            }
            circuito.registrarExito(); return resultado;
        } catch (IllegalArgumentException | ServicioNoDisponibleException e) {
            circuito.registrarFalla(); throw indisponible();
        }
    }

    private DireccionMapaDTO direccion(Map<?, ?> item, SugerenciaCalleDTO s) throws NegocioException {
        var dto = new DireccionMapaDTO();
        dto.setDireccion(JsonGeografico.texto(item.get("address"), 200));
        try { dto.setPunto(JsonGeografico.punto(item.get("lat"), item.get("lng"))); }
        catch (IllegalArgumentException e) { circuito.registrarExito(); throw sinPunto(); }
        String mensaje = item.get("stateMsg") instanceof String m ? m.trim() : "";
        boolean exacta = item.get("state") instanceof Number n && n.doubleValue() == 1;
        dto.setAproximada("CALLE".equals(s.getTipo()) || !exacta || !mensaje.isEmpty()
                || "CALLE".equals(item.get("type")));
        dto.setDetalle(dto.isAproximada() ? "Ubicacion aproximada de IDE; revise el punto en el mapa." : "Direccion localizada por IDE Uruguay.");
        return dto;
    }

    private static void validarSeleccion(SugerenciaCalleDTO s) throws NegocioException {
        if (s == null || s.getTipo() == null || !TIPOS.contains(s.getTipo()) || s.getIdCalle() == null || s.getIdCalle() <= 0
                || ("CALLEyPORTAL".equals(s.getTipo()) && (s.getNumeroPuerta() == null || s.getNumeroPuerta() <= 0))
                || ("ESQUINA".equals(s.getTipo()) && (s.getIdCalleEsquina() == null || s.getIdCalleEsquina() <= 0))
                || (s.getLetra() != null && s.getLetra().length() > 10))
            throw new NegocioException("Seleccione una calle, portal o esquina valida de IDE.");
    }

    private URI uri(String recurso) {
        return URI.create(configuracion.ideBase().toString().replaceAll("/+$", "") + "/" + recurso);
    }
    private static String enc(String valor) { return URLEncoder.encode(valor, StandardCharsets.UTF_8); }
    private static NegocioException sinPunto() {
        return new NegocioException("IDE no encontro un punto unico para esa direccion. Precise la calle y numero o ingrese coordenadas manualmente.");
    }
    private static ServicioNoDisponibleException indisponible() {
        return new ServicioNoDisponibleException("No se pudieron consultar las direcciones de IDE. Puede reintentar o ingresar los datos manualmente.");
    }
}
