-- Esquema base de carga.uy.
-- Acordado entre los 5 frentes (ver docs/contratos.md).
-- Cada frente agrega sus propias migraciones V2__, V3__... SIN modificar las ya aplicadas.

CREATE EXTENSION IF NOT EXISTS postgis;

-- ---------------------------------------------------------------- catalogos (datos precargados)
CREATE TABLE rubro (
    id   BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tipo_carga (
    id   BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

-- ---------------------------------------------------------------- identidad y acceso (frente P1)
CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    cedula      VARCHAR(20)  NOT NULL UNIQUE,
    correo      VARCHAR(200),
    nombre      VARCHAR(200),
    empresa_id  BIGINT,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE rol (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol_id     BIGINT NOT NULL REFERENCES rol (id)     ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

-- ---------------------------------------------------------------- dominio (frente P2)
CREATE TABLE empresa (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(200) NOT NULL,
    razon_social   VARCHAR(200),
    numero_empresa BIGINT       NOT NULL UNIQUE,
    direccion      VARCHAR(300),
    rubro_id       BIGINT REFERENCES rubro (id),
    fecha_alta     DATE
);

ALTER TABLE usuario ADD CONSTRAINT fk_usuario_empresa
    FOREIGN KEY (empresa_id) REFERENCES empresa (id);

CREATE TABLE chofer (
    id         BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT      NOT NULL REFERENCES empresa (id),
    cedula     VARCHAR(20) NOT NULL UNIQUE,
    nombre     VARCHAR(200) NOT NULL
);

CREATE TABLE vehiculo (
    id                 BIGSERIAL PRIMARY KEY,
    empresa_id         BIGINT      NOT NULL REFERENCES empresa (id),
    matricula          VARCHAR(20) NOT NULL UNIQUE,
    marca              VARCHAR(60),
    modelo             VARCHAR(60),
    peso_kg            INTEGER     NOT NULL,
    capacidad_carga_kg INTEGER     NOT NULL,
    numero_permiso     VARCHAR(50),
    permiso_desde      DATE,
    permiso_hasta      DATE,
    itv_vencimiento    DATE
);

-- Guia de viaje de carga. origen/destino quedan georreferenciados para la regla de desvio de ruta.
CREATE TABLE guia (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT       NOT NULL REFERENCES empresa (id),
    vehiculo_id   BIGINT       REFERENCES vehiculo (id),
    chofer_id     BIGINT       REFERENCES chofer (id),
    rubro_id      BIGINT       REFERENCES rubro (id),
    tipo_carga_id BIGINT       REFERENCES tipo_carga (id),
    volumen       NUMERIC(12,2),
    fecha         DATE         NOT NULL,
    origen        VARCHAR(200) NOT NULL,
    origen_lat    DOUBLE PRECISION,
    origen_lon    DOUBLE PRECISION,
    destino       VARCHAR(200) NOT NULL,
    destino_lat   DOUBLE PRECISION,
    destino_lon   DOUBLE PRECISION,
    estado        VARCHAR(30)  NOT NULL DEFAULT 'CREADA',
    creada_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    origen_geom   geometry(Point, 4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(origen_lon, origen_lat), 4326)) STORED,
    destino_geom  geometry(Point, 4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(destino_lon, destino_lat), 4326)) STORED,
    CONSTRAINT ck_guia_estado CHECK (estado IN ('CREADA', 'ASIGNADA', 'EN_CURSO', 'FINALIZADA'))
);

CREATE INDEX ix_guia_vehiculo_fecha ON guia (vehiculo_id, fecha);
CREATE INDEX ix_guia_origen_geom    ON guia USING GIST (origen_geom);
CREATE INDEX ix_guia_destino_geom   ON guia USING GIST (destino_geom);

-- ---------------------------------------------------------------- viaje y eventos (frente P3)
CREATE TABLE evento_viaje (
    id         BIGSERIAL PRIMARY KEY,
    guia_id    BIGINT      NOT NULL REFERENCES guia (id),
    -- generado en el dispositivo: evita duplicados cuando se reenvia (idempotencia)
    id_evento  VARCHAR(80) NOT NULL UNIQUE,
    tipo       VARCHAR(30) NOT NULL,
    instante   TIMESTAMPTZ NOT NULL,
    detalle    TEXT,
    foto       BYTEA,
    recibido_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_evento_tipo CHECK (tipo IN ('INICIO', 'FIN', 'CARGA_PARCIAL', 'DESCARGA_PARCIAL', 'INCIDENTE'))
);

-- ---------------------------------------------------------------- nodos perifericos (frente P4)
CREATE TABLE nodo_periferico (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    tipo   VARCHAR(30)  NOT NULL,
    url    VARCHAR(300),
    activo BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_nodo_tipo CHECK (tipo IN ('BALANZAS', 'TRACKING'))
);

CREATE TABLE posicion_tracking (
    id          BIGSERIAL PRIMARY KEY,
    id_evento   VARCHAR(80)      NOT NULL UNIQUE,
    matricula   VARCHAR(20)      NOT NULL,
    latitud     DOUBLE PRECISION NOT NULL,
    longitud    DOUBLE PRECISION NOT NULL,
    instante    TIMESTAMPTZ      NOT NULL,
    recibido_en TIMESTAMPTZ      NOT NULL DEFAULT now(),
    geom        geometry(Point, 4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(longitud, latitud), 4326)) STORED
);

CREATE INDEX ix_posicion_matricula_instante ON posicion_tracking (matricula, instante);
CREATE INDEX ix_posicion_geom               ON posicion_tracking USING GIST (geom);

CREATE TABLE pesada (
    id        BIGSERIAL PRIMARY KEY,
    id_pesada VARCHAR(80)  NOT NULL UNIQUE,
    guia_id   BIGINT       REFERENCES guia (id),
    matricula VARCHAR(20)  NOT NULL,
    peaje     VARCHAR(100),
    peso_kg   INTEGER      NOT NULL,
    instante  TIMESTAMPTZ  NOT NULL,
    obtenida_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_pesada_matricula ON pesada (matricula, instante);

-- ---------------------------------------------------------------- fiscalizacion (frente P5)
CREATE TABLE parametro_regla (
    id     BIGSERIAL PRIMARY KEY,
    clave  VARCHAR(80)  NOT NULL UNIQUE,
    valor  VARCHAR(200) NOT NULL,
    descripcion VARCHAR(300)
);

CREATE TABLE caso_fiscalizacion (
    id          BIGSERIAL PRIMARY KEY,
    guia_id     BIGINT      REFERENCES guia (id),
    vehiculo_id BIGINT      REFERENCES vehiculo (id),
    empresa_id  BIGINT      REFERENCES empresa (id),
    tipo        VARCHAR(40) NOT NULL,
    estado      VARCHAR(20) NOT NULL DEFAULT 'DETECTADA',
    evidencia   TEXT,
    -- identifica el hecho que origino el caso: evita duplicados al reprocesar eventos
    hecho       VARCHAR(160) NOT NULL UNIQUE,
    detectada_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    resuelta_en  TIMESTAMPTZ,
    fundamento   TEXT,
    CONSTRAINT ck_caso_tipo   CHECK (tipo IN ('EXCESO_CARGA', 'SIN_HABILITACION', 'VIAJE_SIN_GUIA', 'DESVIO_RUTA', 'ANOMALIA')),
    CONSTRAINT ck_caso_estado CHECK (estado IN ('DETECTADA', 'NOTIFICADA', 'EN_DESCARGO', 'CONFIRMADA', 'DESESTIMADA'))
);

CREATE TABLE descargo (
    id        BIGSERIAL PRIMARY KEY,
    caso_id   BIGINT      NOT NULL REFERENCES caso_fiscalizacion (id),
    usuario_id BIGINT     REFERENCES usuario (id),
    fundamento TEXT       NOT NULL,
    presentado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Pista de auditoria de solo agregado (Seccion 4.2 de la letra).
-- No se actualiza ni se borra desde la aplicacion.
CREATE TABLE auditoria (
    id         BIGSERIAL PRIMARY KEY,
    usuario    VARCHAR(100) NOT NULL,
    accion     VARCHAR(100) NOT NULL,
    entidad    VARCHAR(100),
    entidad_id BIGINT,
    fundamento TEXT,
    instante   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
