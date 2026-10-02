-- Frente P1: tablas propias de identidad y acceso.
-- No modifica tablas compartidas: eso va en otra migracion, con aviso al grupo.

-- Cuentas del backoffice (autoridades y administradores) con autenticacion interna.
-- Van separadas de "usuario" porque no siempre son personas identificadas por cedula
-- (por ejemplo, el administrador inicial).
CREATE TABLE cuenta_interna (
    id                    BIGSERIAL PRIMARY KEY,
    nombre_usuario        VARCHAR(60)  NOT NULL UNIQUE,
    nombre                VARCHAR(120) NOT NULL,
    -- NULL solo si la clave la valida un directorio LDAP
    password_hash         VARCHAR(200),
    -- Referencia por nombre al catalogo "rol" (V2), para no duplicar la lista de roles
    rol                   VARCHAR(50)  NOT NULL REFERENCES rol (nombre),
    origen                VARCHAR(20)  NOT NULL DEFAULT 'BASE_DE_DATOS',
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    intentos_fallidos     INTEGER      NOT NULL DEFAULT 0,
    bloqueado_hasta       TIMESTAMPTZ,
    -- Se incrementa al dar de baja, cambiar el rol o la clave: invalida los tokens ya emitidos
    version_credenciales  INTEGER      NOT NULL DEFAULT 0,
    debe_cambiar_password BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_cuenta_rol    CHECK (rol IN ('ADMINISTRADOR', 'AUTORIDAD')),
    CONSTRAINT ck_cuenta_origen CHECK (origen IN ('BASE_DE_DATOS', 'LDAP')),
    CONSTRAINT ck_cuenta_password_segun_origen CHECK (
        (origen = 'BASE_DE_DATOS' AND password_hash IS NOT NULL)
        OR (origen = 'LDAP' AND password_hash IS NULL))
);

CREATE INDEX ix_cuenta_rol_activo ON cuenta_interna (rol, activo);

-- Funcionarios del MTOP habilitados. Cedula y nombre viven en "usuario" (vienen de gub.uy).
CREATE TABLE funcionario (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL UNIQUE REFERENCES usuario (id),
    dependencia VARCHAR(150),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Pedidos de acceso como funcionario, que aprueba o rechaza un administrador.
CREATE TABLE solicitud_funcionario (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL REFERENCES usuario (id),
    dependencia    VARCHAR(150) NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    creada_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resuelta_en    TIMESTAMPTZ,
    resuelta_por   VARCHAR(100),
    motivo_rechazo VARCHAR(500),
    CONSTRAINT ck_solicitud_estado   CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_solicitud_resuelta CHECK ((estado = 'PENDIENTE') = (resuelta_en IS NULL))
);

CREATE INDEX ix_solicitud_estado ON solicitud_funcionario (estado);

-- Una sola solicitud pendiente por persona, garantizado en la base y no solo en el codigo:
-- dos pedidos simultaneos a dos instancias del central no pueden colarse los dos.
CREATE UNIQUE INDEX ux_solicitud_una_pendiente
    ON solicitud_funcionario (usuario_id) WHERE estado = 'PENDIENTE';
