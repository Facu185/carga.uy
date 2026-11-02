-- P4: el panel de operacion consulta cada pocos segundos las posiciones recibidas en los ultimos
-- minutos y la ultima recepcion. Sin indice, cada consulta recorria la tabla entera, que al volumen
-- base del ADR-0008 crece unos 8,6 millones de filas por dia (prueba de performance, 2026-11-02).
CREATE INDEX ix_posicion_recibido_en ON posicion_tracking (recibido_en);
