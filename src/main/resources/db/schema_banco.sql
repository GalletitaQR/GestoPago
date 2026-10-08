-- =============================================================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS - SISTEMA BANCARIO (CLIENTES, CUENTAS, USUARIOS)
-- =============================================================================

-- 1. TABLA CLIENTES
CREATE TABLE IF NOT EXISTS clientes (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(50)     NOT NULL,
    segundo_nombre      VARCHAR(50),
    apellido_paterno    VARCHAR(50)     NOT NULL,
    apellido_materno    VARCHAR(50)     NOT NULL,
    fecha_nacimiento    DATE            NOT NULL,
    curp                VARCHAR(18)     NOT NULL UNIQUE,
    rfc                 VARCHAR(13)     NOT NULL UNIQUE,
    sexo                VARCHAR(20)     NOT NULL,
    nacionalidad        VARCHAR(50)     NOT NULL,
    estado_civil        VARCHAR(30)     NOT NULL,
    correo              VARCHAR(100)    NOT NULL UNIQUE,
    telefono_movil      VARCHAR(10)     NOT NULL,
    telefono_alternativo VARCHAR(15),
    ocupacion           VARCHAR(100)    NOT NULL,
    empresa             VARCHAR(100)    NOT NULL,
    ingreso_mensual     NUMERIC(15,2)   NOT NULL,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clientes_curp ON clientes(curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc ON clientes(rfc);
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes(correo);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);

-- 2. TABLA DOMICILIOS
CREATE TABLE IF NOT EXISTS domicilios (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT          NOT NULL UNIQUE REFERENCES clientes(id) ON DELETE CASCADE,
    calle               VARCHAR(100)    NOT NULL,
    numero_exterior     VARCHAR(20)     NOT NULL,
    numero_interior     VARCHAR(20),
    colonia             VARCHAR(100)    NOT NULL,
    municipio           VARCHAR(100)    NOT NULL,
    estado              VARCHAR(100)    NOT NULL,
    codigo_postal       VARCHAR(5)      NOT NULL,
    pais                VARCHAR(50)     NOT NULL
);

-- 3. TABLA CUENTAS
CREATE TABLE IF NOT EXISTS cuentas (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT          NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    numero_cuenta       VARCHAR(20)     NOT NULL UNIQUE,
    saldo               NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    estatus             VARCHAR(20)     NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cuentas_numero_cuenta ON cuentas(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas(cliente_id);

-- 4. TABLA USUARIOS
CREATE TABLE IF NOT EXISTS usuarios (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT          NOT NULL UNIQUE REFERENCES clientes(id) ON DELETE CASCADE,
    correo              VARCHAR(100)    NOT NULL UNIQUE,
    password            VARCHAR(255)    NOT NULL,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_usuarios_cliente_id ON usuarios(cliente_id);
