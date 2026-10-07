-- P2: bajas logicas para preservar guias historicas y habilitacion explicita.
ALTER TABLE empresa ADD COLUMN activa BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE chofer ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE vehiculo ADD COLUMN habilitado BOOLEAN NOT NULL DEFAULT TRUE;

-- Capacidades volumetricas declaradas por fabricante para los vehiculos semilla.
-- Se aplica tambien sobre instalaciones existentes que recibieron V2 antes de V5.
UPDATE vehiculo
SET capacidad_volumen_m3 = CASE matricula
    WHEN 'STZ1234' THEN 95.00
    WHEN 'STZ5678' THEN 100.00
    WHEN 'CUY9012' THEN 90.00
    ELSE capacidad_volumen_m3
END
WHERE matricula IN ('STZ1234', 'STZ5678', 'CUY9012');
