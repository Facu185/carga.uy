package uy.edu.fing.tse.carga.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import uy.edu.fing.tse.carga.api.contract.service.ConsultaGuiasLocal;
import uy.edu.fing.tse.carga.api.dto.guia.ConsultaGuiaDTO;
import uy.edu.fing.tse.carga.api.dto.guia.PaginaConsultaGuiasDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

/** Transporte C14: conversion explicita para conservar 400, no el 404 implicito JAX-RS. */
@Path("/guias/consulta")
@Produces(MediaType.APPLICATION_JSON)
public class ConsultaGuiasRecurso {
    @EJB
    private ConsultaGuiasLocal servicio;

    @GET
    public PaginaConsultaGuiasDTO consultar(@QueryParam("empresaId") String empresaId,
            @QueryParam("desde") String desde, @QueryParam("hasta") String hasta, @QueryParam("estado") String estado,
            @QueryParam("pagina") String pagina, @QueryParam("tamano") String tamano) throws NegocioException {
        Long empresa;
        Integer numero, cantidad;
        try {
            empresa = empresaId == null ? null : Long.valueOf(empresaId);
            numero = pagina == null ? null : Integer.valueOf(pagina);
            cantidad = tamano == null ? null : Integer.valueOf(tamano);
        } catch (NumberFormatException e) {
            throw new NegocioException("La empresa, pagina y tamano deben ser numeros enteros representables.");
        }
        return servicio.consultar(empresa, desde, hasta, estado, numero, cantidad);
    }

    @GET
    @Path("/{id}")
    public ConsultaGuiaDTO detalle(@QueryParam("empresaId") String empresaId, @PathParam("id") String guiaId) throws NegocioException {
        Long empresa, guia;
        try {
            empresa = empresaId == null ? null : Long.valueOf(empresaId);
            guia = guiaId == null ? null : Long.valueOf(guiaId);
        } catch (NumberFormatException e) {
            throw new NegocioException("La empresa y la guia deben ser numeros enteros representables.");
        }
        return servicio.detalle(empresa, guia);
    }
}
