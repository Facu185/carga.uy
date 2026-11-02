package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.ParametrosReglasLocal;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.ParametroReglaDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.ParametroRegla;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO;

import java.util.List;

/**
 * Parametrizacion de las reglas de fiscalizacion sin redesplegar (backoffice, ADR-0022). Las reglas
 * leen el valor en cada evaluacion, asi que un cambio rige desde la siguiente.
 */
@Stateless
@RolesAllowed(Roles.ADMINISTRADOR)
public class ParametrosReglasBean implements ParametrosReglasLocal {

    @EJB
    FiscalizacionDAO dao;

    @EJB
    AuditoriaLocal auditoria;

    @Override
    public List<ParametroReglaDTO> listar() {
        return dao.parametros().stream().map(ParametrosReglasBean::aDTO).toList();
    }

    @Override
    public ParametroReglaDTO modificar(String clave, String valor, String motivo) throws NegocioException {
        ParametroRegla p = dao.parametro(clave == null ? "" : clave.trim())
                .orElseThrow(() -> new NegocioException("No existe el parámetro " + clave + "."));
        String nuevo = validar(p.getClave(), valor);
        String m = Casos.fundamento(motivo, "motivo del cambio");
        String anterior = p.getValor();
        p.setValor(nuevo);
        auditoria.registrar(AccionAuditada.PARAMETRO_REGLA_MODIFICADO, "ParametroRegla", p.getClave(), m,
                p.getClave() + ": " + anterior + " -> " + nuevo);
        return aDTO(p);
    }

    /** Todos los parametros son numeros no negativos; los porcentajes, ademas, hasta 100. */
    static String validar(String clave, String valor) throws NegocioException {
        double numero;
        try {
            numero = Double.parseDouble(valor == null ? "" : valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new NegocioException("El valor de " + clave + " tiene que ser un número.");
        }
        if (numero < 0 || Double.isNaN(numero) || Double.isInfinite(numero)) {
            throw new NegocioException("El valor de " + clave + " no puede ser negativo.");
        }
        if ((clave.endsWith("_pct") || clave.contains("porcentaje")) && numero > 100) {
            throw new NegocioException("El valor de " + clave + " es un porcentaje: tiene que estar entre 0 y 100.");
        }
        if (clave.equals("descargo.plazo_dias") && (numero < 1 || numero != Math.floor(numero))) {
            throw new NegocioException("El plazo de descargo tiene que ser un número entero de días, al menos 1.");
        }
        return numero == Math.floor(numero) ? String.valueOf((long) numero) : String.valueOf(numero);
    }

    private static ParametroReglaDTO aDTO(ParametroRegla p) {
        ParametroReglaDTO dto = new ParametroReglaDTO();
        dto.setClave(p.getClave());
        dto.setValor(p.getValor());
        dto.setDescripcion(p.getDescripcion());
        return dto;
    }
}
