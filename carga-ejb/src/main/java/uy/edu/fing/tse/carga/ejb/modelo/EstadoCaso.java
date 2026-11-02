package uy.edu.fing.tse.carga.ejb.modelo;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida del caso de fiscalizacion (letra: detectada, notificada, en descargo y, finalmente,
 * confirmada o desestimada). Las transiciones permitidas estan aca y en ningun otro lado: cualquier
 * otra se rechaza (punto b de la Fiscalizacion Automatica).
 */
public enum EstadoCaso {
    DETECTADA, NOTIFICADA, EN_DESCARGO, CONFIRMADA, DESESTIMADA;

    public Set<EstadoCaso> siguientes() {
        return switch (this) {
            case DETECTADA -> EnumSet.of(NOTIFICADA);
            case NOTIFICADA -> EnumSet.of(EN_DESCARGO, CONFIRMADA, DESESTIMADA);
            case EN_DESCARGO -> EnumSet.of(CONFIRMADA, DESESTIMADA);
            case CONFIRMADA, DESESTIMADA -> EnumSet.noneOf(EstadoCaso.class);
        };
    }

    public boolean puedePasarA(EstadoCaso destino) {
        return siguientes().contains(destino);
    }

    public boolean resuelto() {
        return this == CONFIRMADA || this == DESESTIMADA;
    }
}
