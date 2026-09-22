CREATE TABLE IF NOT EXISTS personas (
    id                  SERIAL PRIMARY KEY,
    nombre              VARCHAR(100)    NOT NULL,
    apellido_paterno    VARCHAR(100),
    apellido_materno    VARCHAR(100),
    username            VARCHAR(100)    UNIQUE,
    password            VARCHAR(255),
    rol                 VARCHAR(50)     NOT NULL DEFAULT 'ROLE_USER',
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW()
);
