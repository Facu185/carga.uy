-- Datos precargados y juego minimo de prueba para desarrollo.

INSERT INTO rubro (nombre) VALUES
    ('Agropecuario'), ('Construccion'), ('Forestal'), ('Industrial'), ('Supermercados');

INSERT INTO tipo_carga (nombre) VALUES
    ('Granel solido'), ('Granel liquido'), ('Contenedor'), ('Carga general'), ('Refrigerada');

INSERT INTO rol (nombre) VALUES
    ('CIUDADANO'), ('CHOFER'), ('FUNCIONARIO'), ('AUTORIDAD'), ('ADMINISTRADOR');

INSERT INTO parametro_regla (clave, valor, descripcion) VALUES
    ('exceso_carga.tolerancia_kg', '500', 'Margen tolerado por encima del peso maximo admitido'),
    ('desvio_ruta.tolerancia_m',  '5000', 'Distancia maxima al recorrido esperado antes de configurar desvio');

INSERT INTO nodo_periferico (nombre, tipo, url) VALUES
    ('Balanzas peajes', 'BALANZAS', 'http://balanzas:8080/balanzas/api/pesadas'),
    ('Tracking flota',  'TRACKING', 'interno');

INSERT INTO empresa (nombre, razon_social, numero_empresa, rubro_id, fecha_alta) VALUES
    ('Transportes del Sur', 'Transportes del Sur S.A.', 1001, 1, DATE '2020-03-15'),
    ('Cargas Uruguay',      'Cargas Uruguay SRL',       1002, 4, DATE '2021-07-01');

INSERT INTO vehiculo (empresa_id, matricula, marca, modelo, peso_kg, capacidad_carga_kg,
                      numero_permiso, permiso_desde, permiso_hasta, itv_vencimiento)
VALUES
    (1, 'STZ1234', 'Scania', 'R450', 9000, 21000, 'PNC-1001', DATE '2026-01-01', DATE '2026-12-31', DATE '2026-11-30'),
    (1, 'STZ5678', 'Volvo',  'FH16', 9500, 22000, 'PNC-1002', DATE '2025-01-01', DATE '2025-12-31', DATE '2025-12-31'),
    (2, 'CUY9012', 'Mercedes', 'Actros', 8800, 19000, 'PNC-2001', DATE '2026-02-01', DATE '2027-01-31', DATE '2027-01-15');

INSERT INTO chofer (empresa_id, cedula, nombre) VALUES
    (1, '11111111', 'Ana Perez'),
    (1, '22222222', 'Luis Gomez'),
    (2, '33333333', 'Marta Silva');
