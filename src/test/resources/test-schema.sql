-- Tablas de referencia sin entidad JPA y datos que exigen las pruebas HU-05.
-- Idempotente: compatible con el arranque diferido de Hibernate (schema.sql,
-- data.sql) y con la reapertura de la misma base embebida entre clases de prueba.
CREATE TABLE IF NOT EXISTS "Ciudades" (
    id_ciudad integer NOT NULL,
    nombre_ciudad character varying NOT NULL,
    CONSTRAINT Ciudades_pkey PRIMARY KEY (id_ciudad)
);

CREATE TABLE IF NOT EXISTS "Tipo_documento" (
    id_tipo_documento integer NOT NULL,
    nombre character varying NOT NULL,
    CONSTRAINT Tipo_documento_pkey PRIMARY KEY (id_tipo_documento)
);

CREATE TABLE IF NOT EXISTS "Estados_Reserva" (
    id integer NOT NULL,
    nombre character varying NOT NULL,
    CONSTRAINT Estados_Reserva_pkey PRIMARY KEY (id)
);

INSERT INTO "Ciudades" (id_ciudad, nombre_ciudad) VALUES
  (1, 'Medellín')
ON CONFLICT (id_ciudad) DO NOTHING;

INSERT INTO "Tipo_documento" (id_tipo_documento, nombre) VALUES
  (1, 'Cedula')
ON CONFLICT (id_tipo_documento) DO NOTHING;
