-- Administracion de nodos perifericos (frente P4, ADR-0009).

-- A lo sumo un nodo activo por tipo: el central consulta a UN nodo Balanzas. Lo garantiza la base y no
-- solo el codigo, para que dos administradores activando a la vez no dejen dos nodos activos.
CREATE UNIQUE INDEX ux_nodo_periferico_activo_por_tipo ON nodo_periferico (tipo) WHERE activo;

-- Un nodo Balanzas sin URL no se puede consultar
ALTER TABLE nodo_periferico
    ADD CONSTRAINT ck_nodo_balanzas_con_url CHECK (tipo <> 'BALANZAS' OR (url IS NOT NULL AND url <> ''));

ALTER TABLE nodo_periferico ADD COLUMN actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now();

-- El nodo Tracking empuja sus posiciones: no tiene una URL que el central consulte
UPDATE nodo_periferico SET url = NULL WHERE tipo = 'TRACKING' AND url = 'interno';
