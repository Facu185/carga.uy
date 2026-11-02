-- Juego de datos de demostración: cinco empresas con flota y un año de guías.
-- Sirve para mostrar el tablero de la autoridad, el monitoreo de viajes y las cifras públicas
-- (ADR-0021), donde con los datos mínimos de desarrollo casi todo sale "Reservado".
--
--   docker compose exec -T db psql -U carga -d carga < pruebas/datos-demo.sql
--
-- Se puede correr más de una vez: primero borra lo que haya cargado antes (matrículas DEMO y
-- empresas 7001 a 7005). No toca los datos de desarrollo ni los de otras pruebas.
-- Los choferes demo ingresan con gub.uy (simulador) con las cedulas 41000019, 41000025, 41000031, 41000047, 41000053 (empresas 7001 a 7005).
-- Está armado para que en las cifras públicas se vea la regla: "Refrigerada" y "Supermercados"
-- quedan con menos de 3 empresas y se agrupan en "Otros"; un mes del año tiene un solo viaje y
-- figura como "Reservado".

BEGIN;

DELETE FROM descargo WHERE caso_id IN (SELECT c.id FROM caso_fiscalizacion c JOIN vehiculo v ON v.id = c.vehiculo_id WHERE v.matricula LIKE 'DEMO%');
DELETE FROM notificacion_empresa WHERE caso_id IN (SELECT c.id FROM caso_fiscalizacion c JOIN vehiculo v ON v.id = c.vehiculo_id WHERE v.matricula LIKE 'DEMO%');
DELETE FROM caso_fiscalizacion WHERE vehiculo_id IN (SELECT id FROM vehiculo WHERE matricula LIKE 'DEMO%');
DELETE FROM pesada WHERE guia_id IN (SELECT g.id FROM guia g JOIN vehiculo v ON v.id = g.vehiculo_id WHERE v.matricula LIKE 'DEMO%');
DELETE FROM evento_viaje WHERE guia_id IN (SELECT g.id FROM guia g JOIN vehiculo v ON v.id = g.vehiculo_id WHERE v.matricula LIKE 'DEMO%');
DELETE FROM guia WHERE vehiculo_id IN (SELECT id FROM vehiculo WHERE matricula LIKE 'DEMO%');
DELETE FROM pesada WHERE matricula LIKE 'DEMO%';
DELETE FROM posicion_tracking WHERE matricula LIKE 'DEMO%';
DELETE FROM vehiculo WHERE matricula LIKE 'DEMO%';
DELETE FROM chofer WHERE empresa_id IN (SELECT id FROM empresa WHERE numero_empresa BETWEEN 7001 AND 7005);
DELETE FROM vinculo_empresa_usuario WHERE empresa_id IN (SELECT id FROM empresa WHERE numero_empresa BETWEEN 7001 AND 7005);
DELETE FROM empresa WHERE numero_empresa BETWEEN 7001 AND 7005;

INSERT INTO empresa (nombre, razon_social, numero_empresa, rubro_id, fecha_alta) VALUES
    ('Fletes del Litoral', 'Fletes del Litoral S.A.', 7001, 1, DATE '2024-03-01'),
    ('Rutas del Norte', 'Rutas del Norte S.R.L.', 7002, 3, DATE '2024-06-15'),
    ('Logística Atlántida', 'Logística Atlántida S.A.', 7003, 4, DATE '2025-01-10'),
    ('Camiones del Este', 'Camiones del Este S.R.L.', 7004, 2, DATE '2025-02-20'),
    ('Transportes Colonia', 'Transportes Colonia S.A.', 7005, 5, DATE '2025-05-05');

-- Dos camiones por empresa: el segundo de cada una, con la ITV vencida
INSERT INTO vehiculo (empresa_id, matricula, marca, modelo, peso_kg, capacidad_carga_kg,
                      numero_permiso, permiso_desde, permiso_hasta, itv_vencimiento)
SELECT e.id, 'DEMO' || (e.numero_empresa - 7000) || n, 'Scania', 'R450', 9000, 26000,
       'PNC-' || e.numero_empresa || '-' || n, DATE '2026-01-01', DATE '2027-12-31',
       CASE WHEN n = 1 THEN DATE '2027-06-30' ELSE DATE '2026-06-30' END
FROM empresa e CROSS JOIN generate_series(1, 2) AS n
WHERE e.numero_empresa BETWEEN 7001 AND 7005;

-- Cedulas validas (con digito verificador), para poder ingresar con el simulador de gub.uy como chofer
INSERT INTO chofer (empresa_id, cedula, nombre)
SELECT e.id, (ARRAY['41000019', '41000025', '41000031', '41000047', '41000053'])[e.numero_empresa - 7000],
       'Chofer demo ' || (e.numero_empresa - 7000)
FROM empresa e WHERE e.numero_empresa BETWEEN 7001 AND 7005;

-- Un camion con la capacidad declarada baja (9.000 + 12.000 kg): sus pesadas del nodo Balanzas
-- (entre 24 y 33 t) siempre superan el maximo, y la deteccion abre casos de exceso de carga.
INSERT INTO vehiculo (empresa_id, matricula, marca, modelo, peso_kg, capacidad_carga_kg,
                      numero_permiso, permiso_desde, permiso_hasta, itv_vencimiento)
SELECT id, 'DEMO53', 'Volvo', 'FM', 9000, 12000, 'PNC-7003-3', DATE '2026-01-01', DATE '2027-12-31', DATE '2027-06-30'
FROM empresa WHERE numero_empresa = 7003;

-- Una guía por empresa, cada 9 días, durante el último año. Tipo de carga y rubro rotan de forma que
-- los más comunes los usan todas las empresas y los menos comunes, solo una o dos.
INSERT INTO guia (empresa_id, vehiculo_id, chofer_id, rubro_id, tipo_carga_id, fecha, origen, destino,
                  estado, creada_en, peso_kg, volumen, inicio, fin)
SELECT e.id, v.id, c.id,
       CASE WHEN k % 7 = 0 AND e.numero_empresa = 7005 THEN 5 ELSE 1 + (k + e.id) % 4 END,
       CASE WHEN k % 5 = 0 AND e.numero_empresa IN (7001, 7002) THEN 5 ELSE 1 + (k * 3 + e.id) % 4 END,
       (now() - (k * 9 || ' days')::interval)::date,
       (ARRAY['Montevideo', 'Paysandú', 'Rivera', 'Salto', 'Colonia'])[1 + (k + e.id) % 5],
       (ARRAY['Fray Bentos', 'Montevideo', 'Tacuarembó', 'Nueva Palmira', 'Melo'])[1 + (k * 2 + e.id) % 5],
       CASE WHEN k = 0 THEN 'EN_CURSO' ELSE 'FINALIZADA' END,
       now() - (k * 9 + 2 || ' days')::interval,
       12000 + ((k * 37 + e.id * 11) % 13) * 1000, NULL,
       now() - (k * 9 || ' days')::interval,
       now() - (k * 9 || ' days')::interval + interval '10 hours'
FROM empresa e
JOIN vehiculo v ON v.empresa_id = e.id AND v.matricula LIKE 'DEMO%1'
JOIN chofer c ON c.empresa_id = e.id AND c.nombre LIKE 'Chofer demo %'
CROSS JOIN generate_series(0, 39) AS k
WHERE e.numero_empresa BETWEEN 7001 AND 7005
  -- un mes con un solo viaje: muestra la celda "Reservado" en la serie mensual
  AND NOT (date_trunc('month', now() - (k * 9 || ' days')::interval) = date_trunc('month', now() - interval '6 months')
           AND e.numero_empresa <> 7003);

-- El chofer demo 1 es ademas responsable de su empresa, para mostrar las infracciones y el descargo.
-- Su usuario se crea al primer ingreso con gub.uy: si todavia no ingreso, volver a correr este script.
INSERT INTO vinculo_empresa_usuario (usuario_id, empresa_id, tipo, estado, solicitado_en, aprobado_en, aprobado_por)
SELECT u.id, e.id, 'RESPONSABLE', 'ACTIVO', now(), now(), 'datos-demo'
FROM usuario u, empresa e WHERE u.cedula = '41000019' AND e.numero_empresa = 7001;

COMMIT;
