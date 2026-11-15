INSERT INTO "Roles" (nombre) VALUES
  ('Cliente'),
  ('Proveedor de Servicios'),
  ('Administrador')
ON CONFLICT (nombre) DO NOTHING;

-- HU-15: el estado 1 (reserva vigente) se muestra como CONFIRMADA. DO UPDATE
-- renombra la fila en bases existentes que ya la tenian como ACTIVA.
INSERT INTO "Estados_Reserva" (id, nombre) VALUES
  (1, 'CONFIRMADA'),
  (2, 'CANCELADA'),
  (3, 'COMPLETADA')
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre;

-- Dias de la semana: la columna "id" no tiene default, el seed es la unica fuente de ids
INSERT INTO "Dias_Semana" (id, nombre) VALUES
  (1, 'Lunes'),
  (2, 'Martes'),
  (3, 'Miércoles'),
  (4, 'Jueves'),
  (5, 'Viernes'),
  (6, 'Sábado'),
  (7, 'Domingo')
ON CONFLICT (id) DO NOTHING;

-- Ciudades: valores espejo de produccion (tablas sin entidad JPA, creadas en schema.sql)
INSERT INTO "Ciudades" (id_ciudad, nombre_ciudad) VALUES
  (1, 'Medellín'),
  (2, 'Bogotá'),
  (3, 'Cali'),
  (4, 'Barranquila'),
  (5, 'Cartagena')
ON CONFLICT (id_ciudad) DO NOTHING;

INSERT INTO "Tipo_documento" (id_tipo_documento, nombre) VALUES
  (1, 'Cedula')
ON CONFLICT (id_tipo_documento) DO NOTHING;
