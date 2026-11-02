-- P5: ciclo de vida del caso de fiscalizacion, notificacion a la empresa y parametros de las reglas
-- (ADR-0022). Las tablas caso_fiscalizacion, descargo y parametro_regla ya existen desde V1.

ALTER TABLE caso_fiscalizacion
    ADD COLUMN matricula      VARCHAR(20),
    ADD COLUMN notificada_en  TIMESTAMPTZ,
    -- hasta cuando la empresa puede presentar su descargo
    ADD COLUMN vence_descargo TIMESTAMPTZ,
    -- usuario del funcionario que lo resolvio (el detalle queda en la auditoria)
    ADD COLUMN resuelta_por   VARCHAR(100);

CREATE INDEX ix_caso_estado_detectada ON caso_fiscalizacion (estado, detectada_en DESC);
CREATE INDEX ix_caso_empresa          ON caso_fiscalizacion (empresa_id, detectada_en DESC);

-- Un solo descargo por caso
CREATE UNIQUE INDEX ux_descargo_caso ON descargo (caso_id);

-- Notificaciones a la empresa. Hoy el canal es la bandeja del frontoffice (y un correo simulado en
-- el log); la tabla deja constancia de cuando y como se aviso.
CREATE TABLE notificacion_empresa (
    id         BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT       NOT NULL REFERENCES empresa (id),
    caso_id    BIGINT       NOT NULL REFERENCES caso_fiscalizacion (id),
    canal      VARCHAR(30)  NOT NULL,
    mensaje    VARCHAR(500) NOT NULL,
    creada_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_notificacion_empresa ON notificacion_empresa (empresa_id, creada_en DESC);

INSERT INTO parametro_regla (clave, valor, descripcion) VALUES
    ('desvio_ruta.tolerancia_relativa_pct', '15',
     'Ancho del corredor como porcentaje de la distancia en linea recta entre origen y destino (se usa el mayor entre este y tolerancia_m)'),
    ('desvio_ruta.porcentaje_puntos', '20',
     'Porcentaje de posiciones del viaje fuera del corredor a partir del cual hay desvio'),
    ('descargo.plazo_dias', '10',
     'Dias corridos que tiene la empresa para presentar su descargo desde la notificacion');
