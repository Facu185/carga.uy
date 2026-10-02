# Contratos entre frentes

Esto es lo que **no se cambia unilateralmente**. Si necesitás modificar algo de acá, avisá en el
grupo antes de mergear: hay código de otra persona que depende.

## 1. Interfaces de negocio (`carga-api`)

| Interfaz | Dueño | Qué garantiza |
|---|---|---|
| `EmpresaServicio` / `...Local` / `...Remote` | P2 | Alta y consulta de empresas |
| `IngestaTrackingLocal` | P4 | Recibe una posición y la encola; no bloquea |
| `PesajeLocal` | P4 | `obtenerPesadas` devuelve las pesadas; `consultarPesadas` además informa si el nodo respondió y cuánto tardó |
| `MonitorLocal` | P4 | Métricas de ingesta, estado de los nodos y mapa de la flota (solo lectura) |
| `PdiLocal` | P1 | Devuelve los datos de la persona, o `null` si la PDI no responde |
| `ServicioDnic` (SOAP) | P1 | Contrato SOAP hacia la PDI |

Regla: **las interfaces viven en `carga-api`, las implementaciones en `carga-ejb`.** Quien consume
depende de la interfaz, nunca de la clase.

## 2. DTO

Los DTO de `carga-api` cruzan la red (REST, SOAP, JMS, EJB remoto). Por eso:

- Son `Serializable`, con constructor vacío y getters/setters (lo exigen JSON-B, JAXB y JMS).
- Las fechas que viajan en JSON/JMS van como **`String` ISO-8601**, no como `LocalDate`/`OffsetDateTime`:
  evita depender de cómo serialice cada capa. `EmpresaDTO.fechaAlta` es la excepción y usa
  `FechaAdapter` porque también viaja por SOAP.
- Agregar un campo es compatible; **quitar o renombrar uno rompe a quien lo consume.**

## 3. Endpoints REST publicados

| Método y ruta | Quién lo consume |
|---|---|
| `GET /carga/api/salud` | healthchecks, monitoreo |
| `GET /carga/api/empresas[?nombre=]` | frontoffice, backoffice |
| `POST /carga/api/empresas` | backoffice |
| `GET /carga/api/pdi/personas/{cedula}` | pruebas del frente P1 (atajo al cliente SOAP) |
| `POST /carga/api/tracking/posiciones` | **nodo Tracking** — responde `202` |
| `GET /carga/api/monitor/metricas` | panel de operación |
| `GET /carga/api/monitor/nodos` | panel de operación, monitoreo externo |
| `GET /carga/api/monitor/mapa?puntos=` | panel de operación (mapa de la flota) |
| `GET /balanzas/api/pesadas?matricula=&fecha=` | **componente central** (`PesajeBean`) |

Las rutas nuevas de cada frente cuelgan de `/carga/api/<tema>`. No reutilizar prefijos ajenos.

## 4. Mensajería

| Cola | Productor | Consumidor | Cuerpo |
|---|---|---|---|
| `java:/jms/queue/posiciones` | `IngestaTrackingBean` | `PosicionMDB` | `PosicionDTO` en JSON (texto) |

Se usa `TextMessage` con JSON y no `ObjectMessage` a propósito: `ObjectMessage` obliga a configurar
listas de clases permitidas en Artemis y ata productor y consumidor a la misma versión de la clase.

**Idempotencia:** todo mensaje lleva un `idEvento` generado en el origen. El consumidor descarta lo
que ya procesó. Un mensaje puede llegar más de una vez y eso tiene que ser inofensivo.

## 5. Esquema de base de datos

Tablas compartidas (las toca más de un frente): `empresa`, `vehiculo`, `chofer`, `guia`, `usuario`,
`rol`, `auditoria`. Cambiarlas requiere aviso.

Tablas de un solo frente: `posicion_tracking` y `pesada` (P4), `caso_fiscalizacion` y `descargo`
(P5), `evento_viaje` (P3), `usuario_rol` (P1).

Convenciones:

- Nombres de tablas y columnas en `snake_case`, singular (`caso_fiscalizacion`, no `casos`).
- Clave primaria `id BIGSERIAL`.
- Fechas con hora: `TIMESTAMPTZ`. Fechas sin hora: `DATE`.
- Los estados van con `CHECK` y no como tabla aparte, salvo que el catálogo se edite desde la aplicación.
- Coordenadas: se guardan `lat`/`lon` como `DOUBLE PRECISION` y la columna `geometry` se **genera**
  a partir de ellas (`GENERATED ALWAYS AS ... STORED`). Así la aplicación no necesita saber PostGIS
  para escribir, y las consultas espaciales igual tienen índice GiST.

## 6. Variables de entorno

| Variable | Usada por | Default |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | central | base `carga` en el contenedor `db` |
| `NODO_BALANZAS_URL` | `PesajeBean` | `http://balanzas:8080/balanzas/api/pesadas` |
| `SIMULADOR_PDI_URL` | `PdiBean` | `http://localhost:8080/simulador-pdi/ServicioDnicImpl?wsdl` |
| `CENTRAL_INGESTA_URL` | nodo Tracking | `http://central:8080/carga/api/tracking/posiciones` |
| `TRACKING_INTERVALO_MS` | nodo Tracking | `5000` |
| `BALANZAS_DEMORA_MS`, `BALANZAS_FALLA` | nodo Balanzas | `0`, `false` |

Nada de URLs ni credenciales hardcodeadas en el código: todo por entorno, con un default que
funcione en `docker compose`.
