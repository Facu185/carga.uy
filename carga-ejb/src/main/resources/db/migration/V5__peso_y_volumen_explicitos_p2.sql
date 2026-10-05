-- Peso y volumen son limites fisicos independientes. La columna guia.volumen
-- se conserva y pasa a representar explicitamente metros cubicos desde el modelo.
ALTER TABLE guia
    ADD COLUMN peso_kg NUMERIC(12,2) NOT NULL DEFAULT 0;

ALTER TABLE vehiculo
    ADD COLUMN capacidad_volumen_m3 NUMERIC(12,2);

ALTER TABLE guia
    ADD CONSTRAINT ck_guia_peso_kg_no_negativo CHECK (peso_kg >= 0),
    ADD CONSTRAINT ck_guia_volumen_no_negativo CHECK (volumen IS NULL OR volumen >= 0);

ALTER TABLE vehiculo
    ADD CONSTRAINT ck_vehiculo_capacidad_volumen_m3_no_negativa
        CHECK (capacidad_volumen_m3 IS NULL OR capacidad_volumen_m3 >= 0);
