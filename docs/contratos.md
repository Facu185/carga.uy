# Contratos entre frentes

Esto es lo que **no se cambia unilateralmente**. Si necesitás modificar algo de acá, avisá en el
grupo antes de mergear: hay código de otra persona que depende.

## 1. Interfaces de negocio (`carga-api`)

| Interfaz | Dueño | Qué garantiza |
|---|---|---|
| `EmpresaServicio` / `...Local` / `...Remote` | P2 | Alta y consulta de empresas |
| `IngestaTrackingLocal` | P4 | Recibe una posición y la encola; no bloquea |
| `PesajeLocal` | P4 | Devuelve las pesadas de una matrícula en una fecha; lista vacía si el nodo no responde |
| `PdiLocal` | P1 | Devuelve los datos de la persona, o `null` si la PDI no responde |
| `ServicioDnic` (SOAP) | P1 | Contrato SOAP hacia la PDI |
| `AuditoriaLocal` | P1 | Registra en la pista de solo agregado, en la misma transacción que la acción. Usuario y rol salen de la sesión. Las acciones de fiscalización exigen fundamento |
| `AutenticacionLocal`, `CuentaLocal` | P1 | Ingreso al backoffice, validación de tokens, cambio de clave y renovación de sesión |
| `IngresoGubUyLocal` | P1 | Ingreso de ciudadanos y funcionarios con gub.uy (OIDC + PKCE) |
| `MetricasLocal` | P1 | Contadores, histogramas de duración y medidores, exportados en formato Prometheus |
| `SaludLocal` | P1 | Estado del central y de sus dependencias (base de datos, PDI) |
| `CuentasInternasLocal`, `FuncionariosLocal`, `SolicitudesFuncionarioLocal`, `PerfilLocal` | P1 | Gestión de accesos del backoffice y perfil de quien ingresó con gub.uy |

Para proteger un servicio propio alcanza con `@RolesAllowed(Roles.X)` (constantes en
`uy.edu.fing.tse.carga.api.security.Roles`). La identidad la establece la capa web a partir del token
(`Authorization: Bearer ...`); ningún servicio recibe el usuario por parámetro.

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
| `GET /balanzas/api/pesadas?matricula=&fecha=` | **componente central** (`PesajeBean`) |
| `POST /carga/api/auth/login` · `POST /auth/cambiar-password` · `POST /auth/renovar` · `GET /auth/yo` | backoffice, app móvil |
| `GET /carga/api/auth/gubuy/iniciar?destino=web\|movil` · `GET /auth/gubuy/callback` | frontoffice, app móvil (redirecciones) |
| `/carga/api/usuarios-internos` · `/funcionarios` · `/solicitudes-funcionario` · `/auditoria` | backoffice |
| `GET /carga/api/mi-perfil` | frontoffice, app móvil |
| `GET /carga/api/salud` · `GET /carga/api/salud/listo` | health checks (Docker, nube) |
| `GET /carga/api/metricas` | Prometheus (texto) |

Errores: `{"error": "mensaje"}` con `400` (regla de negocio), `401` (sin sesión o credenciales
incorrectas), `403` (sin el rol), `429` (cuenta bloqueada) y `503` (sistema externo caído).

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

## Guías P2

`POST /api/guias` recibe `inicio` y `fin` ISO-8601 con zona horaria, además de los datos de carga. El periodo es semiabierto `[inicio, fin)`: dos viajes consecutivos cuyo fin/inicio coinciden son válidos. La creación toma bloqueos pesimistas de vehículo y chofer y rechaza solapamientos. El responsable se resuelve exclusivamente desde el principal autenticado (`usuario.cedula -> usuario.empresa_id`).

`GET /api/guias/mis-guias` requiere el rol `CHOFER` y resuelve el chofer por la cédula del principal; nunca acepta un identificador de chofer del cliente.

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
| `PDI_TIMEOUT_MS`, `PDI_REINTENTOS`, `PDI_CIRCUITO_UMBRAL`, `PDI_CIRCUITO_ESPERA_SEG` | `PdiBean` | `3000`, `1`, `3`, `30` |
| `JWT_SECRET` (≥ 32 caracteres), `JWT_DURACION_MINUTOS` | central | valor de desarrollo, `30` |
| `ADMIN_INICIAL_USUARIO`, `ADMIN_INICIAL_PASSWORD` | central (solo si no hay cuentas internas) | `admin` / valor de desarrollo |
| `AUTENTICACION_INTERNA` y `LDAP_*` | central (backoffice) | `bd`; con `ldap` se usan las `LDAP_*` |
| `GUBUY_ISSUER`, `GUBUY_AUTHORIZATION_ENDPOINT`, `GUBUY_CLIENT_ID`, `GUBUY_CLIENT_SECRET`, `GUBUY_REDIRECT_URI`, `GUBUY_SCOPES` | central (ingreso con gub.uy) | simulador local |
| `COMPOSE_PROFILES=ldap` | docker compose | sin definir (el directorio LDAP no arranca) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | central (trazas a Jaeger) | `http://jaeger:4318`; vacío = no exporta |
| `FRONTEND_URL_WEB`, `FRONTEND_URL_MOVIL` | central (a dónde vuelve el ingreso con gub.uy) | `http://localhost:3000`, `cargauy://ingreso` |

Nada de URLs ni credenciales hardcodeadas en el código: todo por entorno, con un default que
funcione en `docker compose`.

## 7. Observabilidad: ID de correlación

La letra (Sección 4.4, punto 7) pide logs estructurados con un identificador que siga una misma
operación por todos los componentes. Convención:

- El ID viaja en el header HTTP **`X-Correlation-Id`** (8 a 64 caracteres `A-Z a-z 0-9 . _ -`).
  El frontend lo genera en cada pedido; si no viene, el central genera uno.
- En el central queda en el MDC (`idCorrelacion`) durante todo el pedido: cualquier log de un EJB
  lo incluye solo. Para propagarlo a otro hilo: `Correlacion.ejecutarCon(id, tarea)`.
- Cada componente que el central llama (PDI, nodos) debería **leer el header y loguearlo**, y
  reenviarlo si a su vez llama a otro. La llamada a la PDI ya lo manda.
- Los logs del central salen en JSON por consola.

Métricas: `GET /carga/api/metricas` ya incluye pedidos HTTP por ruta y código de estado, latencia,
ingresos (exitosos, fallidos, bloqueados), consultas a la PDI y estado de su circuito, y memoria e
hilos de la JVM. Cada frente suma las suyas con `MetricasLocal`, con nombres `carga_<tema>_...`
(contadores terminan en `_total`, duraciones en `_ms`). Nunca usar ids, cédulas ni matrículas como
valor de etiqueta: cada valor distinto crea una serie nueva.

### Trazas distribuidas

El central sigue el estándar W3C Trace Context: recibe y manda el header **`traceparent`** y exporta
los tramos a Jaeger (http://localhost:16686). Cada respuesta trae `X-Trace-Id` para buscar su traza.
Flujo trazado de punta a punta: el ingreso con gub.uy (central → canje del código en gub.uy → PDI
→ simulador de DNIC). Para sumar un tramo propio:

```java
try (Traza.Tramo tramo = Traza.iniciar("Balanzas GET /pesadas", Traza.Tipo.CLIENTE)) {
    // mandar tramo.traceparent() en el header "traceparent" de la llamada
}
```
