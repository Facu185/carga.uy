package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.MonitorLocal;
import uy.edu.fing.tse.carga.api.contract.service.NodosPerifericosLocal;
import uy.edu.fing.tse.carga.api.dto.nodo.ActivacionNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.DatosNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.EstadoNodoDTO;
import uy.edu.fing.tse.carga.api.dto.nodo.NodoPerifericoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.NodoPeriferico;
import uy.edu.fing.tse.carga.ejb.modelo.TipoNodo;
import uy.edu.fing.tse.carga.ejb.persistencia.NodoPerifericoDAO;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso "Gestionar nodos perifericos" (Administrador, frente P4, ADR-0009).
 *
 * La base es la fuente de verdad: la URL del nodo Balanzas activo es la que consulta el central, y
 * desactivarlo corta las consultas en el acto, sin reiniciar nada.
 *
 * Se administran nodos Balanzas. El nodo Tracking se muestra con su estado, pero se habilita con su
 * token (ADR-0007): activarlo o desactivarlo aca no cambiaria lo que acepta la ingesta.
 */
@Stateless
@RolesAllowed(Roles.ADMINISTRADOR)
public class NodosPerifericosBean implements NodosPerifericosLocal {

    static final String ENTIDAD = "NodoPeriferico";

    @EJB
    NodoPerifericoDAO dao;

    @EJB
    AuditoriaLocal auditoria;

    @EJB
    MonitorLocal monitor;

    @Override
    public List<NodoPerifericoDTO> listar() {
        Map<Long, EstadoNodoDTO> estados = monitor.estadoNodos().stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(EstadoNodoDTO::getId, Function.identity(), (a, b) -> a));
        return dao.listar().stream().map(n -> aDto(n, estados.get(n.getId()))).toList();
    }

    @Override
    public NodoPerifericoDTO crear(DatosNodoDTO datos) throws NegocioException {
        if (datos == null) {
            throw new NegocioException("Faltan los datos del nodo.");
        }
        if (datos.getTipo() != null && !TipoNodo.BALANZAS.name().equalsIgnoreCase(datos.getTipo().trim())) {
            throw new NegocioException("Desde aca se dan de alta nodos Balanzas. El nodo Tracking se habilita con "
                    + "su token (NODO_TRACKING_TOKEN).");
        }
        NodoPeriferico nodo = NodoPeriferico.nuevoBalanzas(datos.getNombre(), datos.getUrl());
        exigirNombreLibre(nodo.getNombre(), null);
        dao.guardar(nodo);
        auditoria.registrar(AccionAuditada.NODO_PERIFERICO_CREADO, ENTIDAD, nodo.getId(), null,
                "nombre=" + nodo.getNombre() + ", url=" + nodo.getUrl());
        return aDto(nodo, null);
    }

    @Override
    public NodoPerifericoDTO modificar(Long id, DatosNodoDTO datos) throws NegocioException {
        if (datos == null) {
            throw new NegocioException("Faltan los datos del nodo.");
        }
        NodoPeriferico nodo = buscar(id);
        StringBuilder detalle = new StringBuilder();

        if (datos.getNombre() != null && !datos.getNombre().trim().equals(nodo.getNombre())) {
            String anterior = nodo.getNombre();
            exigirNombreLibre(datos.getNombre().trim(), nodo.getId());
            nodo.renombrar(datos.getNombre());
            detalle.append("nombre=").append(nodo.getNombre()).append(" (antes ").append(anterior).append(")");
        }
        if (datos.getUrl() != null && !Objects.equals(datos.getUrl().trim(), nodo.getUrl())) {
            String anterior = nodo.getUrl();
            nodo.cambiarUrl(datos.getUrl());
            if (!detalle.isEmpty()) {
                detalle.append(", ");
            }
            detalle.append("antes=").append(anterior).append(", ahora=").append(nodo.getUrl());
        }
        if (!detalle.isEmpty()) {
            auditoria.registrar(AccionAuditada.NODO_PERIFERICO_MODIFICADO, ENTIDAD, nodo.getId(), null,
                    detalle.toString());
        }
        return aDto(nodo, null);
    }

    @Override
    public NodoPerifericoDTO cambiarActivacion(Long id, ActivacionNodoDTO activacion) throws NegocioException {
        if (activacion == null) {
            throw new NegocioException("Indica si el nodo queda activo o no.");
        }
        NodoPeriferico nodo = buscar(id);
        if (nodo.getTipo() != TipoNodo.BALANZAS) {
            throw new NegocioException("El nodo Tracking se habilita o deshabilita con su token "
                    + "(NODO_TRACKING_TOKEN, ADR-0007), no desde aca.");
        }
        if (nodo.isActivo() == activacion.isActivo()) {
            return aDto(nodo, null);
        }
        if (activacion.isActivo()) {
            var otro = dao.buscarActivo(nodo.getTipo());
            if (otro.isPresent()) {
                throw new NegocioException("Ya esta activo el nodo \"" + otro.get().getNombre()
                        + "\": desactivalo antes de activar otro. El central consulta a un solo nodo Balanzas.");
            }
            nodo.setActivo(true);
            auditoria.registrar(AccionAuditada.NODO_PERIFERICO_ACTIVADO, ENTIDAD, nodo.getId(), null,
                    "nombre=" + nodo.getNombre());
        } else {
            String fundamento = activacion.getFundamento();
            if (fundamento == null || fundamento.isBlank()) {
                throw new NegocioException("Indica por que se desactiva el nodo: el central deja de obtener pesajes.");
            }
            nodo.setActivo(false);
            auditoria.registrar(AccionAuditada.NODO_PERIFERICO_DESACTIVADO, ENTIDAD, nodo.getId(),
                    fundamento.trim(), "nombre=" + nodo.getNombre());
        }
        return aDto(nodo, null);
    }

    private NodoPeriferico buscar(Long id) throws NegocioException {
        return dao.buscar(id).orElseThrow(() -> new NegocioException("No existe el nodo indicado."));
    }

    private void exigirNombreLibre(String nombre, Long excepto) throws NegocioException {
        if (dao.existeNombre(nombre, excepto)) {
            throw new NegocioException("Ya hay un nodo con el nombre \"" + nombre + "\".");
        }
    }

    static NodoPerifericoDTO aDto(NodoPeriferico n, EstadoNodoDTO estado) {
        NodoPerifericoDTO dto = new NodoPerifericoDTO();
        dto.setId(n.getId());
        dto.setNombre(n.getNombre());
        dto.setTipo(n.getTipo().name());
        dto.setUrl(n.getUrl());
        dto.setActivo(n.isActivo());
        dto.setActualizadoEn(n.getActualizadoEn() == null ? null : n.getActualizadoEn().toString());
        if (n.isActivo() && estado != null) {
            dto.setEnLinea(estado.isEnLinea());
            dto.setDetalle(estado.getDetalle());
        }
        return dto;
    }
}
