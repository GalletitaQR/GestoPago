CREATE TABLE IF NOT EXISTS gestopago_productos (
    id                  SERIAL PRIMARY KEY,
    codigo_producto     VARCHAR(50)     NOT NULL UNIQUE,
    nombre              VARCHAR(255)    NOT NULL,
    descripcion         TEXT,
    categoria           VARCHAR(100),
    precio              NUMERIC(12,2),
    costo               NUMERIC(12,2),
    comision            NUMERIC(12,2),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW()
);
