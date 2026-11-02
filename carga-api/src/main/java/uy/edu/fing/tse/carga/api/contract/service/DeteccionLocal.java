package uy.edu.fing.tse.carga.api.contract.service;

import jakarta.ejb.Local;
import uy.edu.fing.tse.carga.api.dto.nodo.PesadaRegistradaDTO;

/**
 * Deteccion automatica de incumplimientos (CU5, frente P5). Corre de forma asincronica respecto de los
 * eventos que la originan: la llaman el consumidor de la cola de pesadas, el consumidor de fin de viaje
 * y la agenda de actividad de tracking. Sin rol: la llaman procesos sin usuario.
 * Idempotente: un mismo hecho nunca genera dos casos.
 */
@Local
public interface DeteccionLocal {

    /** Exceso de carga, vehiculo sin habilitacion y viaje sin guia a partir de una pesada. */
    int evaluarPesada(PesadaRegistradaDTO pesada);

    /** Desvio de ruta al terminar un viaje, sobre el recorrido del vehiculo durante la guia. */
    int evaluarFinDeViaje(Long guiaId);

    /** Actividad de tracking reciente de vehiculos sin viaje en curso o sin habilitacion. */
    int evaluarActividadTracking(int minutos);
}
