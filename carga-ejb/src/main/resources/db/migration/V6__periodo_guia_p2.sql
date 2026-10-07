-- Los viajes se expresan como intervalo semiabierto [inicio, fin) en UTC.
-- La fecha historica se conserva para compatibilidad con P3/P4, derivada del inicio.
ALTER TABLE guia
    ADD COLUMN inicio TIMESTAMPTZ,
    ADD COLUMN fin TIMESTAMPTZ;

UPDATE guia SET inicio = fecha::timestamp AT TIME ZONE 'UTC', fin = (fecha + 1)::timestamp AT TIME ZONE 'UTC'
WHERE inicio IS NULL OR fin IS NULL;

ALTER TABLE guia
    ALTER COLUMN inicio SET NOT NULL,
    ALTER COLUMN fin SET NOT NULL,
    ADD CONSTRAINT ck_guia_periodo_valido CHECK (inicio < fin);

CREATE INDEX ix_guia_vehiculo_periodo ON guia (vehiculo_id, inicio, fin);
CREATE INDEX ix_guia_chofer_periodo ON guia (chofer_id, inicio, fin);
