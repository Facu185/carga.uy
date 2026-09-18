package uy.edu.fing.tse.carga.api;

import jakarta.ejb.Local;

/** Interfaz local: la usan la web y los web services dentro del mismo EAR. */
@Local
public interface EmpresaServicioLocal extends EmpresaServicio {
}
