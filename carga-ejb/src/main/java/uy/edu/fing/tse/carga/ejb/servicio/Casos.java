package uy.edu.fing.tse.carga.ejb.servicio;

import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CasoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.DescargoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.PaginaCasosDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.CasoFiscalizacion;
import uy.edu.fing.tse.carga.ejb.modelo.Descargo;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoCaso;
import uy.edu.fing.tse.carga.ejb.modelo.TipoInfraccion;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Conversiones y validaciones comunes a la bandeja del funcionario y a las infracciones de la empresa. */
final class Casos {

    private Casos() {
    }

    static CasoDTO aDTO(CasoFiscalizacion c, Descargo descargo, Instant ahora) {
        CasoDTO dto = new CasoDTO();
        dto.setId(c.getId());
        dto.setTipo(c.getTipo().name());
        dto.setEstado(c.getEstado().name());
        dto.setEmpresaId(c.getEmpresa() == null ? null : c.getEmpresa().getId());
        dto.setEmpresaNombre(c.getEmpresa() == null ? null : c.getEmpresa().getNombre());
        dto.setMatricula(c.getMatricula());
        dto.setGuiaId(c.getGuiaId());
        dto.setEvidencia(c.getEvidencia());
        dto.setDetectadaEn(texto(c.getDetectadaEn()));
        dto.setNotificadaEn(texto(c.getNotificadaEn()));
        dto.setVenceDescargo(texto(c.getVenceDescargo()));
        dto.setPlazoVencido(c.getEstado() == EstadoCaso.NOTIFICADA && c.plazoVencido(ahora));
        dto.setAdmiteDescargo(c.getEstado() == EstadoCaso.NOTIFICADA && !c.plazoVencido(ahora) && descargo == null);
        dto.setResueltaEn(texto(c.getResueltaEn()));
        dto.setResueltaPor(c.getResueltaPor());
        dto.setFundamento(c.getFundamento());
        if (descargo != null) {
            DescargoDTO d = new DescargoDTO();
            d.setFundamento(descargo.getFundamento());
            d.setPresentadoPor(descargo.getUsuario() == null ? null : descargo.getUsuario().getCedula());
            d.setPresentadoEn(texto(descargo.getPresentadoEn()));
            dto.setDescargo(d);
        }
        return dto;
    }

    static PaginaCasosDTO pagina(List<CasoFiscalizacion> casos, Map<Long, Descargo> descargos, long total,
                                 int pagina, int tamano) {
        Instant ahora = Instant.now();
        PaginaCasosDTO p = new PaginaCasosDTO();
        p.setRegistros(casos.stream().map(c -> aDTO(c, descargos.get(c.getId()), ahora)).toList());
        p.setTotal(total);
        p.setPagina(pagina);
        p.setTamano(tamano);
        return p;
    }

    static int pagina(Integer pagina) throws NegocioException {
        int p = pagina == null ? 0 : pagina;
        if (p < 0) throw new NegocioException("La página no puede ser negativa.");
        return p;
    }

    static int tamano(Integer tamano) throws NegocioException {
        int t = tamano == null ? 20 : tamano;
        if (t < 1 || t > 100) throw new NegocioException("El tamaño de página tiene que estar entre 1 y 100.");
        return t;
    }

    static EstadoCaso estado(String valor) throws NegocioException {
        if (valor == null || valor.isBlank()) return null;
        try {
            return EstadoCaso.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Estado de caso desconocido: " + valor);
        }
    }

    static TipoInfraccion tipo(String valor) throws NegocioException {
        if (valor == null || valor.isBlank()) return null;
        try {
            return TipoInfraccion.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Tipo de infracción desconocido: " + valor);
        }
    }

    /** Fundamento obligatorio y con contenido: queda en el caso y en la auditoria. */
    static String fundamento(String texto, String que) throws NegocioException {
        String f = texto == null ? "" : texto.trim();
        if (f.length() < 10) throw new NegocioException("El " + que + " es obligatorio (al menos 10 caracteres).");
        if (f.length() > 4000) throw new NegocioException("El " + que + " no puede superar los 4.000 caracteres.");
        return f;
    }

    private static String texto(Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
