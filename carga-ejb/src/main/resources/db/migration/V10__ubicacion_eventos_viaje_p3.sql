-- P3: ubicacion opcional de cada evento de viaje (el telefono puede no tener GPS en ese momento).
-- Misma convencion que posicion_tracking: la aplicacion escribe latitud/longitud y la geometria se genera.
ALTER TABLE evento_viaje
    ADD COLUMN latitud  DOUBLE PRECISION,
    ADD COLUMN longitud DOUBLE PRECISION,
    ADD COLUMN geom     geometry(Point, 4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(longitud, latitud), 4326)) STORED,
    ADD CONSTRAINT ck_evento_coordenadas_juntas CHECK ((latitud IS NULL) = (longitud IS NULL)),
    ADD CONSTRAINT ck_evento_latitud  CHECK (latitud  IS NULL OR latitud  BETWEEN -90  AND 90),
    ADD CONSTRAINT ck_evento_longitud CHECK (longitud IS NULL OR longitud BETWEEN -180 AND 180);

-- Historial de un viaje en orden cronologico
CREATE INDEX ix_evento_viaje_guia_instante ON evento_viaje (guia_id, instante);
