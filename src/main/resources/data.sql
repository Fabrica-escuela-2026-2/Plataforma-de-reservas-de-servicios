INSERT INTO "Roles" (nombre) VALUES
  ('Cliente'),
  ('Proveedor de Servicios'),
  ('Administrador')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO "Estados_Reserva" (id, nombre) VALUES
  (1, 'ACTIVA'),
  (2, 'CANCELADA'),
  (3, 'COMPLETADA')
ON CONFLICT (id) DO NOTHING;

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
