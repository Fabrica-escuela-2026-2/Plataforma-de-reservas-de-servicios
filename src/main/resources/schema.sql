-- Tablas de referencia que Hibernate no crea (no tienen entidad JPA).
-- Se ejecuta diferido despues del DDL de Hibernate y antes de data.sql
-- (spring.jpa.defer-datasource-initialization=true).
-- En bases existentes es un no-op por IF NOT EXISTS.
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
