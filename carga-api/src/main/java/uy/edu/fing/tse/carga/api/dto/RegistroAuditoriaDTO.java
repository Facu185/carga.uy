package uy.edu.fing.tse.carga.api.dto;

import java.io.Serializable;

/**
 * momento: ISO-8601.
 */
public class RegistroAuditoriaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String momento;
    private String usuario;
    private String rol;
    private String accion;
    private String entidad;
    private String entidadId;
    private String fundamento;
    private String detalle;

    public RegistroAuditoriaDTO() {
    }

    public RegistroAuditoriaDTO(Long id, String momento, String usuario, String rol, String accion, String entidad, String entidadId, String fundamento, String detalle) {
        this.id = id;
        this.momento = momento;
        this.usuario = usuario;
        this.rol = rol;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.fundamento = fundamento;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMomento() { return momento; }
    public void setMomento(String momento) { this.momento = momento; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getEntidadId() { return entidadId; }
    public void setEntidadId(String entidadId) { this.entidadId = entidadId; }

    public String getFundamento() { return fundamento; }
    public void setFundamento(String fundamento) { this.fundamento = fundamento; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

}
