package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.comun.PersonaDTO;

/** Consulta al servicio de DNIC a traves de la PDI (Web Service SOAP). */
@Local
public interface PdiLocal {

    PersonaDTO obtenerPersona(String cedula);
}
