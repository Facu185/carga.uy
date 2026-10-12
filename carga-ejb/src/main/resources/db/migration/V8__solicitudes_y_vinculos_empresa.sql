-- Una persona se autentica como Usuario; su autorización empresarial vive en
-- un vínculo auditable. Las solicitudes previenen autoasignaciones de empresa.
CREATE TABLE solicitud_alta_empresa (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL REFERENCES usuario(id),
    nombre          VARCHAR(200) NOT NULL,
    razon_social    VARCHAR(200),
    direccion       VARCHAR(300) NOT NULL,
    numero_empresa  BIGINT       NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    creada_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resuelta_en     TIMESTAMPTZ,
    resuelta_por    VARCHAR(100),
    motivo_rechazo  VARCHAR(500),
    empresa_id      BIGINT REFERENCES empresa(id),
    CONSTRAINT ck_solicitud_empresa_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_solicitud_empresa_resuelta CHECK ((estado = 'PENDIENTE') = (resuelta_en IS NULL))
);
CREATE UNIQUE INDEX ux_solicitud_empresa_pendiente_usuario ON solicitud_alta_empresa(usuario_id) WHERE estado = 'PENDIENTE';
CREATE UNIQUE INDEX ux_solicitud_empresa_pendiente_numero ON solicitud_alta_empresa(numero_empresa) WHERE estado = 'PENDIENTE';

CREATE TABLE vinculo_empresa_usuario (
    id                   BIGSERIAL PRIMARY KEY,
    usuario_id           BIGINT       NOT NULL REFERENCES usuario(id),
    empresa_id           BIGINT       NOT NULL REFERENCES empresa(id),
    tipo                 VARCHAR(20)  NOT NULL,
    estado               VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    solicitado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    aprobado_en          TIMESTAMPTZ,
    aprobado_por         VARCHAR(100),
    revocado_en          TIMESTAMPTZ,
    revocado_por         VARCHAR(100),
    motivo_revocacion    VARCHAR(500),
    CONSTRAINT ck_vinculo_empresa_tipo CHECK (tipo IN ('RESPONSABLE', 'OPERADOR')),
    CONSTRAINT ck_vinculo_empresa_estado CHECK (estado IN ('PENDIENTE', 'ACTIVO', 'RECHAZADO', 'REVOCADO'))
);
CREATE UNIQUE INDEX ux_vinculo_empresa_pendiente ON vinculo_empresa_usuario(usuario_id, empresa_id) WHERE estado = 'PENDIENTE';
CREATE UNIQUE INDEX ux_vinculo_empresa_responsable_activo ON vinculo_empresa_usuario(empresa_id) WHERE tipo = 'RESPONSABLE' AND estado = 'ACTIVO';
CREATE INDEX ix_vinculo_empresa_usuario ON vinculo_empresa_usuario(usuario_id, estado);
CREATE INDEX ix_vinculo_empresa_empresa ON vinculo_empresa_usuario(empresa_id, estado);

-- Conserva el comportamiento de usuarios creados antes de este modelo.
INSERT INTO vinculo_empresa_usuario(usuario_id, empresa_id, tipo, estado, solicitado_en, aprobado_en, aprobado_por)
SELECT id, empresa_id, 'RESPONSABLE', 'ACTIVO', creado_en, now(), 'MIGRACION_V8'
FROM (
    SELECT u.id, u.empresa_id, u.creado_en,
           row_number() OVER (PARTITION BY u.empresa_id ORDER BY u.creado_en, u.id) AS posicion
    FROM usuario u JOIN empresa e ON e.id = u.empresa_id
    WHERE u.empresa_id IS NOT NULL AND e.activa = TRUE
) responsables_historicos
WHERE posicion = 1;
