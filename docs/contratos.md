# Contratos entre frentes

Esto es lo que **no se cambia unilateralmente**. Si necesitás modificar algo de acá, avisá en el
grupo antes de mergear: hay código de otra persona que depende.

## 1. Interfaces de negocio (`carga-api`)

| Interfaz | Dueño | Qué garantiza |
|---|---|---|
| `EmpresaServicio` / `...Local` / `...Remote` | P2 | Alta y consulta de empresas |
| `VehiculoServicio` / `VehiculoServicioLocal` | P2 | ABM vehicular y reactivación; autorización por vínculo responsable activo o administrador |
| `IngestaTrackingLocal` | P4 | Recibe una posición y la encola; no bloquea |
| `PesajeLocal` | P4 | `obtenerPesadas` devuelve las pesadas; `consultarPesadas` además informa si el nodo respondió y cuánto tardó |
| `MonitorLocal` | P4 | Métricas de ingesta, estado de los nodos y mapa de la flota (solo lectura) |
| `SincronizacionPesajesLocal` | P4 | Trae las pesadas del nodo, las vincula con la guía vigente y las guarda sin duplicar; avisa cada pesada **nueva** por la cola `pesadas` |
| `RecorridoLocal` | P4 | **Para P5:** recorrido de un vehículo por fechas o por guía, con la distancia (PostGIS). Sin rol |
| `AvisoFinDeViajeLocal` | P4 | **Para P3:** llamarlo al registrar el fin de un viaje; dispara la obtención de pesajes |
| `PdiLocal` | P1 | Devuelve los datos de la persona, o `null` si la PDI no responde |
| `ServicioDnic` (SOAP) | P1 | Contrato SOAP hacia la PDI |
| `AuditoriaLocal` | P1 | Registra en la pista de solo agregado, en la misma transacción que la acción. Usuario y rol salen de la sesión. Las acciones de fiscalización exigen fundamento |
| `AutenticacionLocal`, `CuentaLocal` | P1 | Ingreso al backoffice, validación de tokens, cambio de clave y renovación de sesión |
| `IngresoGubUyLocal` | P1 | Ingreso de ciudadanos y funcionarios con gub.uy (OIDC + PKCE) |
| `MetricasLocal` | P1 | Contadores, histogramas de duración y medidores, exportados en formato Prometheus |
| `SaludLocal` | P1 | Estado del central y de sus dependencias (base de datos, PDI) |
| `CuentasInternasLocal`, `FuncionariosLocal`, `SolicitudesFuncionarioLocal`, `SolicitudesEmpresaLocal`, `PerfilLocal` | P1 | Gestión de accesos del backoffice, solicitudes de empresa/vínculo y perfil de quien ingresó con gub.uy |

Para proteger un servicio propio alcanza con `@RolesAllowed(Roles.X)` (constantes en
`uy.edu.fing.tse.carga.api.security.Roles`). La identidad la establece la capa web a partir del token
(`Authorization: Bearer ...`); ningún servicio recibe el usuario por parámetro.

Excepción de transporte **limitada al ABM JSF P2**: sus páginas pueden recibir el mismo
JWT en la cookie HttpOnly `cargauy_jsf`, validada por P1 en cada petición. La cookie
no autentica REST/SOAP/P4 ni reemplaza Bearer en los contratos existentes. El login JSF
reutiliza las cuentas internas; no agrega un destino OIDC para funcionarios/ciudadanos.
Detalle y límites: [ADR-0011](decisiones/0011-autenticacion-del-abm-jsf.md).

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
| `GET /carga/api/vehiculos?empresaId=` | Mi empresa (incluye vehículos dados de baja) |
| `POST /carga/api/vehiculos` · `PUT /carga/api/vehiculos/{id}` | Mi empresa; `VehiculoDTO`, respuestas 201 y 200 |
| `DELETE /carga/api/vehiculos/{id}` | Baja lógica del vehículo; 204, rechaza guía EN_CURSO con 400 |
| `PUT /carga/api/vehiculos/{id}/reactivar` | Mi empresa; sin cuerpo, reactiva sin modificar otros datos, 204 |
| `GET /carga/api/pdi/personas/{cedula}` | pruebas del frente P1 (atajo al cliente SOAP) |
| `POST /carga/api/tracking/posiciones` | **nodo Tracking** — responde `202`; `503` + `Retry-After` si la cola está saturada (ADR-0008) |
| `POST /carga/api/tracking/posiciones/lote` | **nodo Tracking** — hasta 500 posiciones; `202` con aceptadas y rechazadas; `503` + `Retry-After` si está saturada |
| `GET /carga/api/monitor/metricas` | panel de operación |
| `GET /carga/api/monitor/nodos` | panel de operación, monitoreo externo |
| `GET /carga/api/monitor/mapa?puntos=` | panel de operación (mapa de la flota) |
| `GET /carga/api/pesajes?matricula=&fecha=` | panel de operación, P5 (pesadas ya guardadas) |
| `POST /carga/api/pesajes/sincronizar?matricula=&fecha=` | panel de operación (dispara el caso de uso a mano) |
| `GET /carga/api/recorridos?matricula=&desde=&hasta=` | pantallas de fiscalización (rol administrador o funcionario) |
| `GET /carga/api/recorridos/guia/{id}` | ídem, el recorrido del período de la guía |
| `GET /balanzas/api/pesadas?matricula=&fecha=` | **componente central** (`PesajeBean`) |
| `POST /carga/api/auth/login` · `POST /auth/cambiar-password` · `POST /auth/renovar` · `GET /auth/yo` | backoffice, app móvil |
| `GET /carga/api/auth/gubuy/iniciar?destino=web\|movil` · `GET /auth/gubuy/callback` | frontoffice, app móvil (redirecciones) |
| `/carga/api/usuarios-internos` · `/funcionarios` · `/solicitudes-funcionario` · `/auditoria` | backoffice |
| `/carga/api/solicitudes-empresa` | frontoffice (solicitante) y backoffice (verificación por funcionario) |
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
| `java:/jms/queue/pesadas` | `SincronizacionPesajesBean` | **P5** — todavía sin consumidor | `PesadaRegistradaDTO` en JSON; `guiaId` null = pesado sin guía vigente |
| `java:/jms/queue/viajes-finalizados` | `AvisoFinDeViajeBean` (lo llama P3) | `FinDeViajeMDB` | `ViajeFinalizadoDTO` en JSON |

Se usa `TextMessage` con JSON y no `ObjectMessage` a propósito: `ObjectMessage` obliga a configurar
listas de clases permitidas en Artemis y ata productor y consumidor a la misma versión de la clase.

**Idempotencia:** todo mensaje lleva un `idEvento` generado en el origen. El consumidor descarta lo
que ya procesó. Un mensaje puede llegar más de una vez y eso tiene que ser inofensivo.

## 5. Esquema de base de datos

## Vehículos P2

El ABM requiere `ADMINISTRADOR`, `CIUDADANO` o `CHOFER`; fuera de la excepción
administrativa se comprueba en EJB un vínculo `RESPONSABLE/ACTIVO` del principal
con la empresa. El ID selecciona, no autoriza. El responsable no puede listar
globalmente ni gestionar vehículos de otra empresa. El portal usa su única empresa
responsable activa y no permite seleccionar ni cambiar la empresa del vehículo.

Se conservan los campos de `VehiculoDTO`: id, empresaId, matrícula, marca/modelo,
pesoKg, capacidadCargaKg, capacidadVolumenM3, numeroPermiso, permisoDesde,
permisoHasta, itvVencimiento y habilitado; las fechas ausentes se serializan como
null, no como el texto `"null"`. No se adjuntan archivos. Las validaciones actuales
continúan en EJB; la ITV ausente no habilita al vehículo para nuevas guías.

La baja por DELETE o por PUT con `habilitado=false` rechaza una guía del vehículo
en estado `EN_CURSO`, también para administrador. No bloquea otros estados ni
deduce fin del viaje por fechas. Reactivar solo cambia `habilitado=true`; no
renueva permiso/ITV ni elimina historial. Modificación, baja y reactivación bloquean
el vehículo dentro de la transacción. Coordinar el futuro inicio de viaje P3 con
ese mismo bloqueo y verificación de habilitación. Alcance y límites en
[ADR-0012](decisiones/0012-abm-vehiculos-del-responsable.md).
Las vistas JSF llaman al mismo servicio EJB directamente, no a estos endpoints.

## Guías P2

`POST /api/guias` recibe `inicio` y `fin` ISO-8601 con zona horaria, además de los datos de carga. El periodo es semiabierto `[inicio, fin)`: dos viajes consecutivos cuyo fin/inicio coinciden son válidos. La creación toma bloqueos pesimistas de vehículo y chofer y rechaza solapamientos. La autorización empresarial se resuelve exclusivamente desde el principal autenticado mediante un `vinculo_empresa_usuario` activo; nunca desde un rol global ni de un `empresa_id` en `usuario`.

`GET /api/guias/mis-guias` requiere el rol `CHOFER` y resuelve el chofer por la cédula del principal; nunca acepta un identificador de chofer del cliente.

`GET /carga/api/guias/empresa?empresaId=1&pagina=0&tamano=20` consulta las guías de
una empresa para su responsable aprobado. Requiere rol `CIUDADANO` o `CHOFER` y
vínculo `RESPONSABLE/ACTIVO` del principal con la empresa solicitada, comprobado
en el EJB. El ID recibido selecciona la empresa, NO acredita autorización.
No filtra por estado de guía ni por actividad actual del chofer/vehículo: incluye
asignadas y finalizadas, y guías históricas sin asignaciones (`choferId`,
`vehiculoId`, `nombreChofer` o `matricula` pueden ser nulos). Orden: `inicio DESC, id DESC`.

Devuelve `PaginaGuiasEmpresaDTO`: `{registros, pagina, tamano, total}`; cada registro
es `GuiaEmpresaDTO`: `{guia: GuiaDTO, nombreChofer, matricula}`. Se conservan los
contratos de creación y de consulta del chofer sin cambios. `pagina` empieza en 0
(predeterminado 0), `tamano` admite 1–100 (predeterminado 20); el desplazamiento
debe caber en un entero de 32 bits. Empresa ausente/no positiva o paginación
inválida: `400`; sin vínculo autorizado: `403`, con `{"error":"mensaje"}`.
El total corresponde a esa empresa, no a todas las empresas del usuario.
El nombre y la matrícula reflejan los registros relacionados actuales; no son
instantáneas históricas. La consulta no modifica estados ni deduce finalización
por la fecha prevista del viaje.

Tablas compartidas (las toca más de un frente): `empresa`, `vehiculo`, `chofer`, `guia`, `usuario`,
`rol`, `auditoria`. Cambiarlas requiere aviso.

Tablas de un solo frente: `solicitud_alta_empresa` y `vinculo_empresa_usuario` (P1), `posicion_tracking` y `pesada` (P4), `caso_fiscalizacion` y `descargo`
(P5), `evento_viaje` (P3), `usuario_rol` (P1).

Convenciones:

- Nombres de tablas y columnas en `snake_case`, singular (`caso_fiscalizacion`, no `casos`).
- Clave primaria `id BIGSERIAL`.
- Fechas con hora: `TIMESTAMPTZ`. Fechas sin hora: `DATE`.
- Los estados van con `CHECK` y no como tabla aparte, salvo que el catálogo se edite desde la aplicación.
- Coordenadas: se guardan `lat`/`lon` como `DOUBLE PRECISION` y la columna `geometry` se **genera**
  a partir de ellas (`GENERATED ALWAYS AS ... STORED`). Así la aplicación no necesita saber PostGIS
  para escribir, y las consultas espaciales igual tienen índice GiST.

## Pesajes P4

Una pesada se vincula con la guía **del mismo vehículo cuyo período `[inicio, fin)` contiene el
instante del pesaje**. Es una lectura de la tabla compartida `guia` (P4 no la modifica): si P2 cambia
cómo se guarda el período o el vehículo de una guía, hay que avisar, porque rompe esta vinculación.

Una pesada sin guía vigente se guarda igual, con `guia_id` null: es la evidencia de "viaje sin guía".

La cola `pesadas` recibe un mensaje por cada pesada **nueva**, nunca por una que ya estaba. El
consumidor de P5 igual tiene que tolerar repetidos (deduplicar por `idPesada`), como cualquier cola.

## Seguridad de P4

| Qué | Cómo se protege | Si falla |
|---|---|---|
| `POST /carga/api/tracking/posiciones` (nodo Tracking → central) | header `X-Nodo-Token` = `NODO_TRACKING_TOKEN` | 401 |
| `POST /carga/api/tracking/posiciones/lote` (nodo Tracking → central) | header `X-Nodo-Token` = `NODO_TRACKING_TOKEN` | 401 |
| `GET /balanzas/api/pesadas` (central → nodo Balanzas) | header `X-Nodo-Token` = `NODO_BALANZAS_TOKEN` | 401; el central lo informa como "no aceptó las credenciales" |
| `/carga/api/monitor/*` y `/carga/api/pesajes/*` | sesión con rol `ADMINISTRADOR` o `FUNCIONARIO` | 401 sin sesión, 403 sin el rol |

El token de los nodos **no va en `Authorization`**: ese header lo procesa `MecanismoJwt` en todos los
pedidos y un token de nodo no es un JWT. Los dos lados fallan cerrado: sin token configurado no se
acepta a nadie. Detalle y alternativas en el ADR-0007.

### Administración de nodos (ADR-0009)

| Endpoint | Qué hace |
|---|---|
| `GET /carga/api/nodos` | Nodos registrados, con el estado de los activos verificado en el momento |
| `POST /carga/api/nodos` | Alta de un nodo Balanzas (`{nombre, url}`). Nace desactivado |
| `PUT /carga/api/nodos/{id}` | Cambia nombre y, si es Balanzas, la URL. Rige en la consulta siguiente |
| `PUT /carga/api/nodos/{id}/activo` | `{activo, fundamento}`. Desactivar exige fundamento |

Solo `ADMINISTRADOR` (lo controla `NodosPerifericosBean`). Cada cambio queda en la auditoría con las
acciones `NODO_PERIFERICO_CREADO`, `_MODIFICADO`, `_ACTIVADO` y `_DESACTIVADO`.

La tabla `nodo_periferico` es la fuente de verdad: el central consulta la URL del nodo Balanzas
**activo**, y la base no permite dos nodos activos del mismo tipo. `NODO_BALANZAS_URL` solo se usa si
no hay ningún nodo Balanzas registrado.

**Limitación conocida:** el panel de operación (`panel.xhtml`) usa los servicios directamente y no
tiene inicio de sesión: queda abierto hasta que pase al backoffice autenticado (ADR-0004).

## Recorridos y fin de viaje (P4 con P5 y P3, ADR-0010)

**Recorrido (para P5).** `RecorridoLocal.recorridoDeGuia(guiaId)` devuelve las posiciones del vehículo de
la guía durante su período `[inicio, fin)`, ordenadas por la hora del dispositivo, y la distancia
recorrida en metros. También se puede pedir por matrícula y período (ISO-8601 con zona, hasta 7 días).
Devuelve hasta 10.000 puntos; si había más, `truncado` es `true`. La distancia se calcula siempre sobre
el período completo. P5 lo llama directamente, desde adentro del servidor: el servicio no exige rol. La
API REST, en cambio, sí.

**Fin de viaje (para P3).** Al registrar el fin de un viaje, P3 llama a:

```java
@EJB AvisoFinDeViajeLocal avisoFinDeViaje;
avisoFinDeViaje.avisar(new ViajeFinalizadoDTO(guiaId, matricula, inicioIso, finIso));
```

El aviso se encola dentro de la transacción de P3, así que si esa transacción se deshace, no sale. La
consulta al nodo Balanzas ocurre después, en `FinDeViajeMDB`, para cada día de Uruguay que toca el
viaje. P3 no espera al nodo ni falla si el nodo está caído: en ese caso la agenda de pesajes lo
reintenta dentro de las 24 horas.

## 6. Variables de entorno

| Variable | Usada por | Default |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | central | base `carga` en el contenedor `db` |
| `NODO_BALANZAS_URL` | `PesajeBean` | `http://balanzas:8080/balanzas/api/pesadas` |
| `SIMULADOR_PDI_URL` | `PdiBean` | `http://localhost:8080/simulador-pdi/ServicioDnicImpl?wsdl` |
| `CENTRAL_INGESTA_URL` | nodo Tracking | `http://central:8080/carga/api/tracking/posiciones` |
| `TRACKING_INTERVALO_MS` | nodo Tracking | `5000` |
| `BALANZAS_DEMORA_MS`, `BALANZAS_FALLA` | nodo Balanzas | `0`, `false` |
| `TRACKING_COLA_UMBRAL_ALTO`, `TRACKING_COLA_UMBRAL_BAJO`, `TRACKING_REINTENTAR_SEG`, `TRACKING_LOTE_MAX` | central (ingesta de posiciones, ADR-0008) | `18000`, `9000`, `30`, `500` |
| `BALANZAS_TIMEOUT_MS`, `BALANZAS_REINTENTOS`, `BALANZAS_CIRCUITO_UMBRAL`, `BALANZAS_CIRCUITO_ESPERA_SEG` | `PesajeBean` | `3000`, `1`, `3`, `30` |
| `PESAJES_AGENDA_ACTIVA` | `AgendaPesajesBean` (sincronización cada 15 min) | `true` |
| `NODO_TRACKING_TOKEN` | central (valida) y nodo Tracking (lo manda como `NODO_TOKEN`) | `tracking-solo-desarrollo` |
| `NODO_BALANZAS_TOKEN` | central (lo manda) y nodo Balanzas (valida como `NODO_TOKEN`) | `balanzas-solo-desarrollo` |
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
