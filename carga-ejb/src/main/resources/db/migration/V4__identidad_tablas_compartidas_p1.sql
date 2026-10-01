-- Frente P1: cambios en tablas compartidas (usuario y auditoria), avisados al grupo.
-- Solo agrega columnas nullable y protege la auditoria: no rompe a ningun otro frente.

-- ---------------------------------------------------------------- usuario
-- Datos que completa el ingreso con gub.uy y la verificacion con DNIC (via PDI).
ALTER TABLE usuario
    ADD COLUMN fecha_nacimiento   DATE,
    ADD COLUMN verificado_dnic_en TIMESTAMPTZ,
    ADD COLUMN ultimo_ingreso     TIMESTAMPTZ;

-- ---------------------------------------------------------------- auditoria
-- entidad_id pasa a texto para poder auditar entidades con clave no numerica (matricula, cedula).
ALTER TABLE auditoria
    ADD COLUMN rol     VARCHAR(50),
    ADD COLUMN detalle TEXT,
    ALTER COLUMN entidad_id TYPE VARCHAR(60) USING entidad_id::text;

CREATE INDEX ix_auditoria_entidad  ON auditoria (entidad, entidad_id);
CREATE INDEX ix_auditoria_usuario  ON auditoria (usuario);
CREATE INDEX ix_auditoria_instante ON auditoria (instante);

-- Solo agregado (Seccion 4.2 de la letra): la base rechaza modificar o borrar la pista,
-- aunque alguien lo intente por fuera de la aplicacion.
CREATE FUNCTION auditoria_solo_agregado() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La pista de auditoria es de solo agregado: % no permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_auditoria_sin_cambios
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_solo_agregado();

CREATE TRIGGER tr_auditoria_sin_truncate
    BEFORE TRUNCATE ON auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION auditoria_solo_agregado();
