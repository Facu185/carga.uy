package uy.edu.fing.tse.carga.api;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.PaginaAuditoriaDTO;

/**
 * Pista de auditoria de solo agregado (Seccion 4.2 de la letra).
 *
 * Uso desde cualquier servicio, dentro de la misma transaccion que la accion:
 * <pre>
 *   &#64;EJB AuditoriaLocal auditoria;
 *   auditoria.registrar(AccionAuditada.CASO_CREADO, "CasoFiscalizacion", caso.getId(), fundamento, detalle);
 * </pre>
 * Si la accion falla no queda registro, y si el registro falla la accion no se confirma.
 */
@Local
public interface AuditoriaLocal {

    /** El usuario y el rol se toman de la sesion: nadie registra una accion a nombre de otro. */
    void registrar(AccionAuditada accion, String entidad, Object entidadId, String fundamento, String detalle)
            throws NegocioException;

    /** Solo para cuando todavia no hay sesion (un intento de ingreso). */
    void registrarComo(String usuario, String rol, AccionAuditada accion, String entidad, Object entidadId,
                       String fundamento, String detalle);

    /** Consulta paginada (pagina desde 0). Solo administradores y autoridades. */
    PaginaAuditoriaDTO buscar(String usuario, String entidad, String entidadId, Integer pagina, Integer tamano);
}
