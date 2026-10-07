package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Remote;

/** Interfaz remota: la usan clientes fuera de la JVM del servidor. */
@Remote
public interface EmpresaServicioRemote extends EmpresaServicio {
}
