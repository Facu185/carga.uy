# Contratos entre frentes

Esto es lo que **no se cambia unilateralmente**. Si necesitás modificar algo de acá, avisá en el
grupo antes de mergear: hay código de otra persona que depende.

## 1. Interfaces de negocio (`carga-api`)

| Interfaz | Dueño | Qué garantiza |
|---|---|---|
| `EmpresaServicio` / `...Local` / `...Remote` | P2 | Alta, modificación, baja lógica y consulta de empresas; mutaciones para funcionario/administrador |
| `PerfilEmpresaServicioLocal` | P2 | Consulta/edición del perfil propio; CIUDADANO/CHOFER con vínculo RESPONSABLE/ACTIVO y empresa activa |
| `ReporteActividadLocal` | P5 (bloques solicitados ADR-0020/0023) | Agregados por creación de guías o instante FIN; solo AUTORIDAD, sin vínculo ni mutaciones |
| `VehiculoServicio` / `VehiculoServicioLocal` | P2 | ABM vehicular y reactivación; autorización por vínculo responsable activo o administrador |
| `IngestaTrackingLocal` | P4 | Recibe una posición y la encola; no bloquea |
| `PesajeLocal` | P4 | `obtenerPesadas` devuelve las pesadas; `consultarPesadas` además informa si el nodo respondió y cuánto tardó |
| `MonitorLocal` | P4 | Métricas de ingesta, estado de los nodos y mapa de la flota (solo lectura) |
| `SincronizacionPesajesLocal` | P4 | Trae las pesadas del nodo, las vincula con la guía vigente y las guarda sin duplicar; avisa cada pesada **nueva** por la cola `pesadas` |
| `RecorridoLocal` | P4 | **Para P5:** recorrido de un vehículo por fechas o por guía, con la distancia (PostGIS). Sin rol |
| `AvisoFinDeViajeLocal` | P4 | **Para P3:** llamarlo al registrar el fin de un viaje; dispara la obtención de pesajes |
| `EventoViajeLocal` | P3 | Registra los eventos del chofer (idempotente por `idEvento`), avanza el estado de la guía y avisa el fin de viaje. Solo `CHOFER` (ADR-0019) |
| `PdiLocal` | P1 | Devuelve los datos de la persona, o `null` si la PDI no responde |
| `ServicioDnic` (SOAP) | P1 | Contrato SOAP hacia la PDI |
| `AuditoriaLocal` | P1 | Registra en la pista de solo agregado, en la misma transacción que la acción. Usuario y rol salen de la sesión. Las acciones de fiscalización exigen fundamento |
| `AutenticacionLocal`, `CuentaLocal` | P1 | Ingreso al backoffice, validación de tokens, cambio de clave y renovación de sesión |
| `IngresoGubUyLocal` | P1 | Ingreso de ciudadanos y funcionarios con gub.uy (OIDC + PKCE) |
| `MetricasLocal` | P1 | Contadores, histogramas de duración y medidores, exportados en formato Prometheus |
| `SaludLocal` | P1 | Estado del central y de sus dependencias (base de datos, PDI) |
| `CuentasInternasLocal`, `FuncionariosLocal`, `SolicitudesFuncionarioLocal`, `SolicitudesEmpresaLocal`, `PerfilLocal` | P1 | Gestión de accesos del backoffice, solicitudes de empresa/vínculo y perfil de quien ingresó con gub.uy |

Los roles de un servicio se declaran con `@RolesAllowed(Roles.X)` (constantes en
`uy.edu.fing.tse.carga.api.security.Roles`); esto no reemplaza controles de objeto,
pertenencia o incompatibilidad. La identidad la establece la capa web a partir del
token (`Authorization: Bearer ...`); ningún servicio recibe el usuario por parámetro.

Excepción de transporte **limitada al ABM JSF P2**: sus páginas pueden recibir el mismo
JWT en la cookie HttpOnly `cargauy_jsf`, validada por P1 en cada petición. La cookie
no autentica REST/SOAP/P4 ni reemplaza Bearer en los contratos existentes. El login JSF
reutiliza las cuentas internas; no agrega un destino OIDC para funcionarios/ciudadanos.
Detalle y límites: [ADR-0011](decisiones/0011-autenticacion-del-abm-jsf.md).

Regla: **las interfaces viven en `carga-api`, las implementaciones en `carga-ejb`.** Quien consume
depende de la interfaz, nunca de la clase.

### Incompatibilidad funcionario/responsable — ADR-0015

Acuerdo confirmado por el usuario para este cambio P1/P2, sin atribuir aceptación
externa de los frentes: [ADR-0015](decisiones/0015-incompatibilidad-funcionario-responsable.md).
CIUDADANO sigue siendo rol base de todo ingreso gub.uy. FUNCIONARIO y CIUDADANO
pueden coexistir; lo incompatible es funcionario **activo** y responsabilidad
empresarial **RESPONSABLE/ACTIVO**, comprobados por identidad en datos persistidos.

- `SolicitudesEmpresaLocal`: un funcionario activo no puede solicitar vínculo ni
  alta empresarial, recibir responsabilidad por aprobación/transferencia ni ser
  el responsable generado al aprobar un alta. Se valida al destinatario antes de
  mutar o revocar al responsable anterior. Conflicto: 400, solicitud sin resolver.
- `FuncionariosLocal` y `SolicitudesFuncionarioLocal`: un responsable activo no
  puede habilitarse/reactivarse por alta manual, cambio de estado o aprobación de
  solicitud. Conflicto: 400, sin cambio de rol, estado, dependencia ni resolución.
  Las solicitudes de acceso no habilitadas pueden seguir pendientes.
- Ambas direcciones toman un lock PESSIMISTIC_WRITE de la misma fila Usuario
  antes de comprobar el estado opuesto, dentro de la transacción EJB. No se usa
  el listado truncado de vínculos ni se exige empresa activa para detectar conflicto.
- Ante combinaciones heredadas se deniega con 403 el ejercicio como responsable:
  perfil propio, ABM de vehículos/choferes, creación/catálogos empresariales y
  listado empresarial de guías. No se revocan vínculos ni se elimina historial.
  Consulta de estado/historial de solicitudes/vínculos y funciones oficiales
  conservan sus permisos. Resolver mediante baja funcionario o transferencia
  administrativa explícita, no con revocación automática por esta política.
- No cambia D0-04: crear guías/catálogos sigue admitiendo ACTIVO sin tipo para
  identidades sin la combinación incompatible. No impone incompatibilidad CHOFER,
  ni cambia las guías propias del chofer, sus eventos o sus asignaciones.

Rutas, DTO, firmas, roles declarativos y `{"error":"mensaje"}` se conservan.
Es una restricción de negocio acordada, no una migración de datos ni del login.
Implementación local con pruebas preparadas; seguridad/locks/rollback reales
**pendientes** en [matriz de integración](../pruebas/incompatibilidad-funcionario-responsable.md).

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
| `GET /carga/api/reportes/actividad?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | Tablero AUTORIDAD; agregados sin detalle individual (ADR-0020) |
| `GET /carga/api/reportes/viajes-finalizados?desde=YYYY-MM-DD&hasta=YYYY-MM-DD&tipoCargaId=` | AUTORIDAD; viajes con FIN aceptado por instante del evento, sin detalle individual (ADR-0023) |
| `POST /carga/api/empresas` | backoffice |
| `PUT /carga/api/empresas/{id}` | Gestión administrativa existente; `EmpresaDTO`, respuesta 200 con DTO; funcionario/administrador |
| `DELETE /carga/api/empresas/{id}` | Baja lógica administrativa existente; 204; funcionario/administrador |
| `GET /carga/api/empresas/{id}/perfil` · `PUT /carga/api/empresas/{id}/perfil` | Mi empresa / F10; 200 con `EmpresaDTO`; PUT recibe solo `EditarPerfilEmpresaDTO` |
| `GET /carga/api/vehiculos?empresaId=` | Mi empresa (incluye vehículos dados de baja) |
| `POST /carga/api/vehiculos` · `PUT /carga/api/vehiculos/{id}` | Mi empresa; `VehiculoDTO`, respuestas 201 y 200 |
| `DELETE /carga/api/vehiculos/{id}` | Baja lógica del vehículo; 204, rechaza guía EN_CURSO con 400 |
| `PUT /carga/api/vehiculos/{id}/reactivar` | Mi empresa; sin cuerpo, reactiva sin modificar otros datos, 204 |
| `GET /carga/api/pdi/personas/{cedula}` | pruebas del frente P1 (atajo al cliente SOAP) |
| `POST /carga/api/tracking/posiciones` | **nodo Tracking** — responde `202`; `503` + `Retry-After` si la cola está saturada (ADR-0008) |
| `POST /carga/api/tracking/posiciones/lote` | **nodo Tracking** — hasta 500 posiciones; `202` con aceptadas y rechazadas; `503` + `Retry-After` si está saturada |
| `GET /carga/api/monitor/metricas` | panel de operación. `vehiculosReportando` cuenta los que mandaron posiciones en los últimos 5 minutos; `posicionesTotales` es exacto hasta 100.000 filas y estimado por Postgres desde ahí |
| `GET /carga/api/monitor/nodos` | panel de operación, monitoreo externo |
| `GET /carga/api/monitor/mapa?puntos=` | panel de operación (mapa de la flota): vehículos con posiciones en las últimas 6 horas, con sus últimos `puntos` puntos |
| `GET /carga/api/pesajes?matricula=&fecha=` | panel de operación, P5 (pesadas ya guardadas) |
| `GET /carga/api/pesajes/guia/{id}` | **P2, monitoreo de viajes (F14, C14-P):** las pesadas vinculadas a la guía, de todos los días del viaje |
| `POST /carga/api/pesajes/sincronizar?matricula=&fecha=` | panel de operación (dispara el caso de uso a mano) |
| `GET /carga/api/recorridos?matricula=&desde=&hasta=` | pantallas de fiscalización (rol administrador o funcionario) |
| `GET /carga/api/recorridos/guia/{id}` | ídem, el recorrido del período de la guía |
| `GET /balanzas/api/pesadas?matricula=&fecha=` | **componente central** (`PesajeBean`) |
| `POST /carga/api/auth/login` · `POST /auth/cambiar-password` · `POST /auth/renovar` · `GET /auth/yo` | backoffice, app móvil |
| `GET /carga/api/auth/gubuy/iniciar?destino=web\|movil` · `GET /auth/gubuy/callback` | frontoffice, app móvil (redirecciones) |
| `/carga/api/usuarios-internos` · `/funcionarios` · `/solicitudes-funcionario` · `/auditoria` | backoffice |
| `/carga/api/solicitudes-empresa` | frontoffice (solicitante) y backoffice (verificación por funcionario) |
| `GET /carga/api/mi-perfil` | frontoffice, app móvil |
| `POST /carga/api/eventos-viaje` · `GET /carga/api/eventos-viaje?guiaId=` | **app móvil** (chofer) — ver "Eventos de viaje P3" |
| `GET /carga/api/salud` · `GET /carga/api/salud/listo` | health checks (Docker, nube) |
| `GET /carga/api/metricas` | Prometheus (texto) |

Errores: `{"error": "mensaje"}` con `400` (regla de negocio), `401` (sin sesión o credenciales
incorrectas), `403` (sin el rol, o `RecursoAjenoException`: el recurso no existe o es de otro),
`409` (`ConflictoException`: choca con el estado actual), `429` (cuenta bloqueada) y `503` (sistema
externo caído).

Las rutas nuevas de cada frente cuelgan de `/carga/api/<tema>`. No reutilizar prefijos ajenos.

### Reporte de actividad de autoridad — ADR-0020

El reporte por creación de esta sección se conserva. La consulta adicional de
viajes finalizados usa [ADR-0023](decisiones/0023-reporte-viajes-finalizados-autoridad.md):
GET `/reportes/viajes-finalizados?desde=&hasta=&tipoCargaId=` y
`ReporteActividadLocal.viajesFinalizados(String desde, String hasta, String tipoCargaId)`.
Solo AUTORIDAD; mismos límites, validación, metadatos de carga, errores y
`Cache-Control: no-store`. No expone historial ni abre permisos de eventos P3.
Devuelve `ReporteActividadDTO` con `criterioTemporal: "FIN_VIAJE"`: `totalGuias`
cuenta guías distintas con FIN aceptado (un viaje por guía), `empresasConGuias`
sus empresas y `porDia` usa el instante FIN en Montevideo. El primer instante FIN
por guía se reduce antes del rango, evitando doble conteo aun con filas repetidas.
No usa estado actual como sustituto de FIN, creación, fechas previstas ni recepción.
`porEstado` sigue mostrando estados actuales de la población seleccionada.
Conservar la consulta previa por creación, incluso al omitir todos los parámetros.
CSV/PDF y pantalla distinguen modo, fecha aplicada y fiscalización general por
detección; avisar de sincronizaciones tardías que pueden modificar resultados pasados.

Contrato aditivo local confirmado por el usuario; no acredita aceptación externa
P5 ni reasigna sus responsabilidades. [ADR-0020](decisiones/0020-tablero-autoridad-actividad.md).

- `ReporteActividadLocal.consultar(String desde, String hasta)` devuelve
  `ReporteActividadDTO`. Nuevo GET `/reportes/actividad`, sin operaciones de escritura.
- Ampliación confirmada del filtro de carga: se conserva la firma anterior
  (equivale a todos los tipos) y se añade
  `consultar(String desde, String hasta, String tipoCargaId)`. REST admite
  `?tipoCargaId=<id>` opcional: ausente incluye todas las guías, también sin tipo;
  presente exige entero decimal positivo sin espacios/signos/ceros iniciales y
  representable como `Long`. Blanco, formato inválido o ID inexistente: 400 con
  `{"error":"mensaje"}`. ID existente sin guías: 200 con ceros.
  El filtro se aplica a `guia.tipo_carga_id` antes de todos los agregados; no usa
  rubro ni tipo actual de vehículo/empresa. Fechas ausentes siguen permitidas.
- GET `/reportes/actividad/tipos-carga`, mediante `ReporteActividadLocal.tiposCarga()`:
  catálogo completo `{id,nombre}[]`, ordenado por nombre, solo AUTORIDAD y
  `Cache-Control: no-store`. Sin mutaciones ni cambios de permisos al catálogo
  operativo `/guias/catalogos/tipos-carga`. Vacío = `[]`, fallo no equivale a vacío.
- Respuesta de actividad incorpora `tipoCargaId` y `tipoCargaNombre` del filtro
  aplicado (ID/nombre del catálogo, incluso sin guías). Ausentes o nulos sin
  filtro; los consumidores anteriores pueden ignorarlos. La SPA exige ambos
  coherentes con la selección y no presenta como filtrada una respuesta antigua.
- El tipo de carga filtra **solo actividad**. Fiscalización mantiene fecha de
  detección, todos los tipos y casos sin guía; no se cambia su endpoint ni motor.
  Pantalla, CSV y PDF indican esa diferencia y el filtro aplicado a actividad,
  nunca el borrador del formulario. Entrega parcial: no cierra filtros globales,
  ubicación ni indicadores de habilitación. Propiedad y aceptación externa P5 intactas.
- Solo AUTORIDAD con principal válido. Sin sesión: 401; otros roles (también
  ADMINISTRADOR): 403. Se conserva sesión P1, clave temporal y Bearer existentes.
- Ambos parámetros presentes o ambos ausentes. Ausentes: últimos 30 días,
  incluido hoy según reloj del servidor en America/Montevideo. Presentes:
  fechas estrictas YYYY-MM-DD (años 0001–9999), desde <= hasta, máximo 366 días
  inclusivos. Blanco, fecha imposible, par incompleto o rango mayor: 400 con
  `{"error":"mensaje"}`. No se aplica un límite silencioso ni paginación.
- Filtrar `guia.creada_en >= desde 00:00 local` y `< (hasta + 1 día) 00:00 local`.
  Todos los agregados provienen de una única instantánea SQL. Se incluyen guías
  sin asignaciones/rubro y empresas actualmente inactivas, sin duplicar por vínculos.
- Respuesta 200 JSON, también para vacío; `Cache-Control: no-store`:

  ```json
  {
    "desde": "2026-11-01", "hasta": "2026-11-02",
    "zonaHoraria": "America/Montevideo", "criterioTemporal": "CREACION_GUIA",
    "totalGuias": 1, "empresasConGuias": 1,
    "porDia": [
      {"clave":"2026-11-01","etiqueta":"2026-11-01","cantidad":0},
      {"clave":"2026-11-02","etiqueta":"2026-11-02","cantidad":1}
    ],
    "porEstado": [
      {"clave":"CREADA","etiqueta":"CREADA","cantidad":0},
      {"clave":"ASIGNADA","etiqueta":"ASIGNADA","cantidad":1},
      {"clave":"EN_CURSO","etiqueta":"EN_CURSO","cantidad":0},
      {"clave":"FINALIZADA","etiqueta":"FINALIZADA","cantidad":0}
    ],
    "porRubro": [{"clave":"SIN_RUBRO","etiqueta":"Sin rubro","cantidad":1}]
  }
  ```

- Cada grupo usa `ConteoActividadDTO {clave, etiqueta, cantidad}`. Cantidades:
  enteros no negativos (`long`). Días completos en orden ascendente, cuatro
  estados en el orden del ejemplo; rubros por cantidad descendente y clave como
  desempate. Rubro presente: clave = ID de catálogo en texto, etiqueta = nombre
  de catálogo; ausente: `SIN_RUBRO`, sin confundirlo con un rubro de igual nombre.
  Vacío: totales cero, días/estados con cero y `porRubro: []`.
- `empresasConGuias` cuenta empresas distintas **con guías creadas en ese rango**,
  no empresas activas ni todas las empresas registradas. `porEstado` muestra el
  estado persistido **actual**, no al cierre del rango ni historia de eventos.
  Cada distribución suma `totalGuias`; el filtro no cambia por estado o rubro.
  No acredita viajes ejecutados, cumplimiento, salud/completitud de nodos ni
  fecha global de última actualización. No entrega guías individuales ni PII.
- Consumidor SPA `/backoffice/reportes`: valida estructura, cantidades seguras,
  período y coherencia de totales antes de mostrar datos. No convierte fallos
  en cero ni mezcla resultados de consultas distintas. Sin runner frontend
  instalado; escenarios y validaciones reales pendientes en
  [matriz de aceptación](../pruebas/tablero-autoridad.md).

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

## Empresas P2

`EmpresaServicio` expone `agregar`, `modificar`, `darDeBaja`, `listar` y
`buscarPorNombre`. GET `/carga/api/empresas[?nombre=]` devuelve una lista de
`EmpresaDTO`, ordenada por ID, sin paginación; nombre ausente/vacío lista todas y
nombre informado busca coincidencia parcial sin distinguir mayúsculas. La lectura
admite CIUDADANO, CHOFER, FUNCIONARIO, AUTORIDAD y ADMINISTRADOR; no aplica filtro
de pertenencia. No equivale a un contrato de perfil propio ni existe GET por ID.

POST devuelve 201 con DTO; PUT `/{id}` devuelve 200 con DTO; DELETE `/{id}`
devuelve 204 sin cuerpo. Mutaciones solo para FUNCIONARIO/ADMINISTRADOR.
`EmpresaDTO` contiene `id`, `nombre`, `razonSocial`, `numeroEmpresa`, `direccion`,
`fechaAlta` y `activa`. La modificación administrativa actual actualiza nombre,
razón social, número, dirección y fecha de alta; no cambia `activa`.
No se amplía a responsables ciudadanos por documentarla.

La baja desactiva la empresa, revoca sus vínculos activos y registra auditoría en
la operación EJB. Repetirla sobre empresa inactiva es error de negocio. Negocio
(incluido número duplicado/empresa inexistente) responde `400 {"error":"mensaje"}`;
autenticación/autorización conserva 401/403. Esto describe código existente,
no acredita rollback ni seguridad en ejecución. Fuente:
[EmpresaRecurso](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/EmpresaRecurso.java)
y [EmpresaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/EmpresaServicioBean.java).

**Consumidor F12 local (fase 3, 2026-10-22):** `/mi-cuenta` enlaza al área SPA
`/funcionario/empresas` para FUNCIONARIO. Listado/búsqueda y detalle
`/funcionario/empresas/:id` usan GET del listado actual (selección por ID en el
detalle); no se añade GET individual ni paginación. El formulario administrativo
envía solo los cinco campos editables a PUT y la baja confirmada consume DELETE
204. `fechaAlta=null` se completa con la fecha actual por el servicio existente;
esto NO aplica al perfil propio F10. El portal impide editar números fuera del
rango seguro de JavaScript sin modificar el contrato `long` del servidor.
`activa` no se envía como campo editable ni se ofrece reactivación.

La nueva área reutiliza la bandeja P1 de **vínculos de responsables** bajo
`/funcionario/solicitudes-responsables`, no un circuito de alta empresarial.
Alta y aceptación compartida P1/P3 siguen pendientes. Administrador/JSF conservan
sus recorridos y permisos anteriores; las rutas SPA no autorizan al servidor.
Vehículos enlaza a la consulta C13 de solo lectura de fase 4; Viajes consume
la consulta C14 de fase 5 y su consumidor local C14-P en el detalle, sin historial
P3 para funcionario ni aceptación integrada acreditada.
Validación real pendiente en [escenarios F12](../pruebas/p2-funcionario-empresas.md).

La elección de SPA F12–F14 está acotada por [ADR-0013](decisiones/0013-area-funcionario-spa-p2.md).
El perfil F10/C10 queda aprobado por el usuario y publicado a continuación.
C13/F13 fue confirmado posteriormente por el usuario en
[ADR-0016](decisiones/0016-consulta-vehicular-funcionario.md). C14 y la entrega
parcial P2/F14 fueron aprobados por el usuario en
[ADR-0018](decisiones/0018-consulta-viajes-funcionario-p2.md); C14-E para funcionario
sigue pendiente. C14-P está publicado en [Pesajes P4](#pesajes-p4) y su consumidor
SPA está implementado localmente, sin validación ejecutada.

### Consulta vehicular F13 / C13

Contrato aprobado por el usuario el 2026-10-22, sin acreditar aceptación externa
ni ejecución. `ConsultaVehiculosLocal.consultar(Long empresaId, String matricula,
Integer pagina, Integer tamano)` devuelve `PaginaConsultaVehiculosDTO`.
REST: `GET /carga/api/vehiculos/consulta`, solo FUNCIONARIO/ADMINISTRADOR, sin
vínculo empresarial; autorización en el EJB antes de existencia.

- `empresaId` obligatorio y positivo, incluida empresa inactiva; inexistente: 400.
- `matricula` opcional, recortada; búsqueda parcial literal sin distinguir
  mayúsculas. Vacía equivale a sin filtro. `%`, `_` y `!` no son comodines.
- `pagina` ausente: 0; `tamano` ausente: 20, rango 1–100. Negativos, números
  no enteros/no interpretables y desplazamiento mayor a `Integer.MAX_VALUE`: 400.
  Orden `matricula ASC, id ASC`, incluyendo vehículos dados de baja.
- Respuesta 200: `{registros, pagina, tamano, total, fechaReferencia, zonaHoraria}`.
  `registros` contiene `ConsultaVehiculoDTO`: `id`, `empresaId`, `matricula`,
  `habilitado`, `numeroPermiso`, `permisoDesde`, `permisoHasta`, `itvVencimiento`,
  `estadoPermiso`, `estadoItv`. Fechas ISO-8601 o null para datos ausentes.
  Página fuera de resultados: lista vacía, total conservado. Conteo y página
  pueden variar ante escrituras concurrentes; no hay promesa de instantánea.
- `fechaReferencia`: fecha actual del servidor en `America/Montevideo`, indicada
  por `zonaHoraria`. Una sola referencia para todos los registros de la respuesta.
  Permiso `FUTURO`/`VIGENTE`/`VENCIDO`, con inicio y fin inclusivos;
  `SIN_DATOS` si falta número/fecha o el intervalo está invertido. ITV
  `VIGENTE` hasta su vencimiento inclusive, `VENCIDO` después, `SIN_DATOS` si
  falta fecha. No hay estado FUTURO de ITV porque no se conoce su inicio.
  Habilitación/baja lógica independiente de esos estados; no se modifican reglas CU2.
- Errores `400 {"error":"mensaje"}` para filtros/paginación/empresa inexistente;
  401 sin sesión válida y 403 sin rol. Correlación y Bearer existentes conservados.

El consumidor SPA `/funcionario/empresas/:id/vehiculos` es de solo lectura;
la nueva API no cambia GET ni mutaciones anteriores de `/vehiculos`, sus DTO,
pertenencia ni excepciones administrativas. No se incorporan cambios SOAP/JMS.

### Perfil propio F10 / C10

`PerfilEmpresaServicioLocal.consultar(Long id)` y
`modificar(Long id, EditarPerfilEmpresaDTO datos)` conservan la API administrativa
y remota existente. GET/PUT `/carga/api/empresas/{id}/perfil` devuelven 200 con
`EmpresaDTO` (incluida la excepción de fechaAlta ya publicada). Sin paginación.

GET y PUT requieren CIUDADANO o CHOFER con vínculo RESPONSABLE/ACTIVO comprobado
desde el principal en EJB y empresa activa. No hay excepción administrativa en
este contrato: los administradores conservan su ABM anterior. Inexistencia,
inactividad y falta de pertenencia responden 403 sin distinguir el motivo.
Sin sesión válida: 401. ID no positivo, payload ausente/inválido o número
duplicado: 400. Errores `{"error":"mensaje"}` y correlación sin cambios.

PUT recibe `{nombre, razonSocial, numeroEmpresa, direccion}`: nombre no vacío,
número entero positivo de 64 bits/único; longitudes máximas del esquema 200,
200 y 300 para los tres textos, respectivamente. Razón social y dirección
pueden ser null/ausentes. El nombre se guarda sin espacios exteriores.
`id`, `fechaAlta`, `activa`, vínculos y bajas NO son campos de entrada: el
servidor nunca los asigna desde este payload, aunque un cliente los añada.
Un campo desconocido no obtiene permisos por aparecer en JSON.

El PUT bloquea la empresa y valida antes de asignar los cuatro campos; el flush
permite traducir duplicados de la restricción existente a negocio con rollback.
Esto requiere acreditación WildFly/PostgreSQL; no se promete exclusión de todos
los flujos administrativos ni rollback a partir de una prueba con dobles.
Alcance aceptado y alternativas en [ADR-0014](decisiones/0014-perfil-propio-empresa.md).

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

La implementación actual de creación y catálogos empresariales de
vehículos/choferes exige CIUDADANO y vínculo ACTIVO **sin filtrar su tipo**. El
listado empresarial siguiente exige RESPONSABLE/ACTIVO. No se unifican estas
condiciones mediante esta actualización documental: decisión P1/P2 pendiente
D0-04 en el [registro de fase 0](acuerdos-cierre-p2-fase-0.md).

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

### Consulta de viajes F14 / C14 (parte P2)

Contrato aprobado por el usuario el 2026-10-27, según
[ADR-0018](decisiones/0018-consulta-viajes-funcionario-p2.md). No acredita aceptación
externa, ejecución ni integración de fuentes P3/P4.

`ConsultaGuiasLocal.consultar(Long empresaId, String desde, String hasta,
String estado, Integer pagina, Integer tamano)` devuelve `PaginaConsultaGuiasDTO`.
`detalle(Long empresaId, Long guiaId)` devuelve `ConsultaGuiaDTO`.

- GET `/carga/api/guias/consulta` y GET `/carga/api/guias/consulta/{id}`:
  FUNCIONARIO/ADMINISTRADOR, solo lectura sin vínculo empresarial. Autorizar
  en el EJB antes de consultar existencia. No ampliar `GuiaServicioBean`.
- `empresaId` obligatorio y positivo en ambas rutas; `id` de guía positivo en
  detalle. Incluye empresas inactivas e históricos sin vehículo/chofer asignado.
  La guía del detalle debe pertenecer a la empresa seleccionada.
- Listado: `desde`/`hasta` opcionales como par de instantes ISO-8601 con offset
  o Z, `desde < hasta`. Se busca solapamiento del **período previsto** con
  `[desde, hasta)`: `inicio < hasta AND fin > desde`. Coincidir solo en un extremo
  no cuenta. Ambos ausentes no filtran; un límite ausente, vacío o sin zona es
  inválido. No usar la columna heredada `fecha` ni la zona del navegador.
- `estado` opcional, recortado; ausente/blanco no filtra. Valores exactos:
  `CREADA`, `ASIGNADA`, `EN_CURSO`, `FINALIZADA`. Es el estado persistido, no
  historial real ni una inferencia por fecha prevista.
- Página base 0 (predeterminada 0), tamaño 1–100 (predeterminado 20), producto
  página/tamaño hasta `Integer.MAX_VALUE`; orden `inicio DESC, id DESC`.
  Página fuera de resultados: registros vacíos con total conservado. Conteo y
  filas pueden variar ante escrituras concurrentes; no se promete instantánea.
- Listado 200: `{registros, pagina, tamano, total}`. Cada registro y el detalle
  200: `{guia: GuiaDTO, nombreEmpresa, matricula, nombreChofer}`. `GuiaDTO`
  conserva IDs, carga en kg/m³, origen/destino/coordenadas, inicio/fin previstos,
  estado y creación ISO-8601. Asignaciones y volumen históricos pueden ser
  nulos/ausentes; no se reconstruyen. El peso histórico cero se conserva.
  Nombre empresarial, matrícula y nombre de chofer reflejan registros actuales,
  no una instantánea histórica. No exponer cédula, correo ni entidades JPA.
- Errores `400 {"error":"mensaje"}` para filtros, IDs o paginación inválidos,
  números no representables, empresa/guía inexistente o guía de otra empresa;
  401 sin sesión válida y 403 sin permiso. Conservar correlación y Bearer.

SPA de FUNCIONARIO: `/funcionario/empresas/:id/viajes` y
`/funcionario/empresas/:id/viajes/:guiaId`, con navegación desde F12, filtros en
URL, paginación y detalle de solo lectura. ADMINISTRADOR dispone de la API sin
cambiar su acceso al área SPA. El historial P3 indica **pendiente de integración**,
no una lista vacía ni datos ficticios. Desde el 2026-10-31, el detalle válido de
C14 consume el GET C14-P publicado, solo si hay vehículo asignado; sin asignación
se indica fuente no consultable, no ausencia de pesajes. Muestra carga, vacío,
error y advertencias independientes por `truncado` y `nodoDisponible=false`.
No asocia por matrícula/día ni descarta asociaciones por comparar fechas previstas;
`guiaId=null` conserva su significado y no se admite como pesada de esa guía.
La validación del JSON rechaza otra guía, tipos/instantes inválidos, duplicados y
cantidad incoherente. Muestra instante y obtención de cada pesada en America/Montevideo,
sin inventar una fecha global de sincronización ni garantizar completitud por
`nodoDisponible=true`. No hay mapas, polling, sincronización manual ni mutaciones.
El consumidor reutiliza Bearer/correlación/expiración de sesión, permite reconsultar
solo lectura y aborta/ignora respuestas obsoletas; la seguridad real sigue en el servidor.
C14-E para funcionario y la aceptación integrada de C14-P siguen pendientes. Validación en
[escenarios F14 P2](../pruebas/p2-consulta-viajes.md), pendiente de ejecución.

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

**Pesajes de una guía (C14-P, para F14).** `GET /carga/api/pesajes/guia/{id}` (rol `FUNCIONARIO` o
`ADMINISTRADOR`; desde el servidor, `SincronizacionPesajesLocal.registradasDeGuia`) devuelve las pesadas
que la sincronización ya vinculó a esa guía, de **todos** los días del viaje, ordenadas por la hora de la
balanza. No se vuelve a asociar por matrícula y día, y una pesada con `guiaId` null nunca aparece.

```json
{ "guiaId": 77, "matricula": "STZ1234", "desde": "2026-10-07T12:00Z", "hasta": "2026-10-09T12:00Z",
  "pesadas": [ { "idPesada": "...", "peaje": "Peaje Pando", "pesoKg": 28000, "instante": "...", ... } ],
  "cantidad": 1, "truncado": false, "nodoDisponible": true }
```

- Sin paginación: una guía junta unas pocas pesadas (una por peaje y día). Tope de 500, con `truncado`.
- `nodoDisponible: false` = el nodo Balanzas está caído ahora (circuito abierto). La lista puede estar
  incompleta; mostrarlo como "puede haber pesajes pendientes de obtener", no como "sin pesajes". La
  agenda los trae cuando el nodo vuelve.
- Guía inexistente: 400 `{"error": ...}`. Sin sesión 401, sin rol 403.
- No sincroniza: solo lee lo guardado. La sincronización la disparan la agenda y el fin de viaje (P3).

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

## Eventos de viaje P3 (ADR-0019)

`POST /carga/api/eventos-viaje`, solo rol `CHOFER`. El chofer sale del token; nunca del cuerpo.

```json
{
  "idEvento": "3f2a9c1e-7b4d-4e8a-9c1b-2d3e4f5a6b7c",
  "guiaId": 1,
  "tipo": "INICIO",
  "instante": "2026-10-20T08:05:00-03:00",
  "detalle": "opcional; obligatorio en INCIDENTE, hasta 1000 caracteres",
  "latitud": -34.9011,
  "longitud": -56.1645,
  "fotoBase64": "opcional, JPEG en base64"
}
```

- `idEvento`: lo genera el dispositivo (8 a 80 caracteres `A-Z a-z 0-9 . _ -`; la app usa UUID).
- `tipo`: `INICIO`, `FIN`, `CARGA_PARCIAL`, `DESCARGA_PARCIAL` o `INCIDENTE`.
- `instante`: cuándo ocurrió en el dispositivo, ISO-8601 con zona. Hasta 5 minutos en el futuro (desfase de reloj).
  Registros nuevos normalizan la fracción a microsegundos para su persistencia y comparación.
- `latitud` y `longitud`: opcionales, pero juntas.
- `fotoBase64`: opcional, JPEG, hasta `EVENTOS_FOTO_MAX_BYTES`.

Respuesta: `{"nuevo": true|false, "estadoGuia": "...", "evento": {...}}` (sin la foto; `tieneFoto` indica si hay).

| Código | Cuándo |
|---|---|
| `201` | Evento nuevo |
| `200` | Ese `idEvento` ya estaba registrado con el mismo contenido normalizado: sin duplicar efectos |
| `400` | Datos inválidos |
| `403` | La guía no existe o no es del chofer (se responde igual en los dos casos) |
| `409` | Estado incompatible, INICIO nuevo con vehículo sin asignar/de baja, asignación cambiada durante la espera o `idEvento` reutilizado con otro contenido |

Estado de la guía: `ASIGNADA --INICIO--> EN_CURSO --(CARGA_PARCIAL | DESCARGA_PARCIAL | INCIDENTE)--> EN_CURSO --FIN--> FINALIZADA`.
El `FIN` llama a `AvisoFinDeViajeLocal` en la misma transacción (dispara los pesajes del CU4).

**Política central aprobada por el usuario (2026-10-31):** se decide sobre el estado
confirmado al procesar, no se reconstruye habilitación histórica. Un INICIO
capturado antes de una baja pero recibido después se rechaza con 409. No afecta
al reenvío idéntico de un evento ya aceptado, que se resuelve antes de comprobar
estado/baja como una operación nueva. La aprobación y la implementación locales
no acreditan acuerdo individual de Federico ni ejecución del móvil.

La igualdad comprende guía, tipo, instante normalizado, detalle (blancos como null,
sin espacios externos), coordenadas y bytes de foto; no compara `recibidoEn` ni
`tieneFoto`. Offsets del mismo instante son equivalentes. `estadoGuia` refleja el
estado actual, no una reproducción del estado que tenía al aceptar el evento.
La unicidad global se conserva con `INSERT ... ON CONFLICT (id_evento) DO NOTHING`;
solo el insertado cambia estado o emite FIN. Comparar datos antiguos con precisión
submicrosegundo requiere caracterización de compatibilidad en el entorno de pruebas.

INICIO toma exclusión **Vehículo → Guía** y relee baja/asignación; DELETE y PUT de
baja toman el mismo vehículo antes de comprobar EN_CURSO. Si cambió la asignación
se devuelve 409 sin tomar un vehículo distinto fuera de orden. La baja P2 conserva
su error de negocio 400; no se cambia su contrato a 409. Esta entrega no cambia
las reglas de permiso/ITV de creación ni define validez histórica de esas vigencias.

El móvil debe conservar el evento y su motivo de conflicto sin pérdida silenciosa
ni reenvío automático infinito. La cola por guía es ordenada; los fallos temporales
conservan pendientes y reintentos limitados. Implementación/aceptación del móvil
pendientes de P3. [Regresiones y aceptación ADR-0019](../pruebas/p2-p3-eventos-conflictos.md)
pendientes de ejecución: mocks no acreditan locks, SQL/JDBC ni rollback JTA/JMS.

`GET /carga/api/eventos-viaje?guiaId=`: eventos de una guía del chofer, en orden cronológico, sin las fotos.
La lectura C14-E para funcionario sigue pendiente; esta evolución no amplía roles
del GET/POST ni cierra F14/fase 5.

**Tabla `evento_viaje` (P3):** la migración `V10` agrega `latitud`, `longitud` y `geom` (generada).
**Tabla `guia` (compartida):** P3 cambia `estado` al registrar eventos (`Guia.aplicarEvento`).

## Frontoffice público (ADR-0021)

Sin sesión. Servicio `InformacionPublicaLocal` (`@PermitAll`), recurso `/carga/api/publico`.

| Endpoint | Responde |
|---|---|
| `GET /publico/permisos/vehiculo?matricula=STZ1234` | `ValidacionVehiculoDTO`: `encontrado`, `vigente`, `vehiculoHabilitado`, `empresaHabilitada`, `estadoPermiso` (`VIGENTE`, `FUTURO`, `VENCIDO`, `SIN_DATOS`), `permisoHasta`, `estadoItv`, `itvVencimiento`, `fechaReferencia`. **No dice la empresa ni el chofer** |
| `GET /publico/permisos/empresa?numero=1001` | `ValidacionEmpresaDTO`: `encontrada`, `nombre` (público), `habilitada`, `fechaReferencia` |
| `GET /publico/estadisticas` | `EstadisticasPublicasDTO`: últimos 12 meses, `Cache-Control: max-age=60` |

- La matrícula se compara sin espacios ni guiones y sin importar mayúsculas. Inválida (vacía, más de
  20 caracteres o con símbolos) o número de empresa no numérico: 400 `{"error": ...}`.
- Un identificador inexistente no es un error: 200 con `encontrado`/`encontrada` en `false`.
- `vigente` es `true` solo si el vehículo y su empresa están habilitados y el permiso y la ITV están
  vigentes. Mismo criterio de vigencia que la consulta del funcionario (`ConsultaVehiculosBean`).
- En las estadísticas, una cifra se publica solo si reúne **al menos 5 guías de al menos 3 empresas
  distintas**. Las que no llegan vienen con `reservada: true` y `guias`/`toneladas` en null, o se
  agrupan en la fila `OTROS`. `empresasHabilitadas`, `vehiculosHabilitados` y `vehiculosAlDia` vienen
  en null si no cumplen. Detalle y justificación en el ADR-0021.

## Fiscalización P5 (ADR-0022)

| Endpoint | Quién | Qué hace |
|---|---|---|
| `GET /fiscalizacion/casos?estado=&tipo=&pagina=&tamano=` | FUNCIONARIO, ADMINISTRADOR | Bandeja, del más reciente al más antiguo |
| `GET /fiscalizacion/casos/{id}` | FUNCIONARIO, ADMINISTRADOR | Caso con evidencia, plazo y descargo |
| `POST /fiscalizacion/casos/{id}/resolucion` | FUNCIONARIO, ADMINISTRADOR | `{"confirmar": true, "fundamento": "..."}`. 409 si el estado no lo admite (confirmar antes de que venza el plazo, o un caso ya resuelto) |
| `GET /fiscalizacion/reporte?desde=&hasta=` | AUTORIDAD, ADMINISTRADOR | Detectados, confirmados, desestimados y pendientes, por tipo, y tasa de confirmación |
| `GET /infracciones?empresaId=` | responsable activo de la empresa | Casos de su empresa (403 si no es responsable) |
| `GET /infracciones/{casoId}?empresaId=` | ídem | Detalle; 403 si el caso es de otra empresa |
| `POST /infracciones/{casoId}/descargo?empresaId=` | ídem | `{"fundamento": "..."}`. Uno por caso, dentro del plazo; 409 si no corresponde |
| `GET /parametros-reglas`, `PUT /parametros-reglas/{clave}` | ADMINISTRADOR | `{"valor": "600", "motivo": "..."}`; rige desde la próxima evaluación |

- **Tipos:** `EXCESO_CARGA`, `SIN_HABILITACION`, `VIAJE_SIN_GUIA`, `DESVIO_RUTA`.
- **Estados:** `DETECTADA → NOTIFICADA → EN_DESCARGO → CONFIRMADA | DESESTIMADA`. Desde `NOTIFICADA` se
  puede resolver directamente: desestimar en cualquier momento, y confirmar con el plazo vencido.
- **Fundamentos** (descargo, resolución, motivo de un cambio de parámetro): entre 10 y 4.000 caracteres.
- **Consumidores:** `DeteccionPesadaMDB` lee la cola `pesadas` (antes no tenía consumidor).
  `FinDeViajeMDB` evalúa el desvío después de traer los pesajes. `AgendaDeteccionBean` revisa el tracking
  cada 5 minutos.
- **Parámetros** (`parametro_regla`): `exceso_carga.tolerancia_kg` (500), `desvio_ruta.tolerancia_m`
  (5000), `desvio_ruta.tolerancia_relativa_pct` (15), `desvio_ruta.porcentaje_puntos` (20) y
  `descargo.plazo_dias` (10).

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
| `FISCALIZACION_AGENDA_ACTIVA` | `AgendaDeteccionBean` (revisa el tracking cada 5 min) | `true` |
| `NODO_TRACKING_TOKEN` | central (valida) y nodo Tracking (lo manda como `NODO_TOKEN`) | `tracking-solo-desarrollo` |
| `NODO_BALANZAS_TOKEN` | central (lo manda) y nodo Balanzas (valida como `NODO_TOKEN`) | `balanzas-solo-desarrollo` |
| `PDI_TIMEOUT_MS`, `PDI_REINTENTOS`, `PDI_CIRCUITO_UMBRAL`, `PDI_CIRCUITO_ESPERA_SEG` | `PdiBean` | `3000`, `1`, `3`, `30` |
| `EVENTOS_FOTO_MAX_BYTES` | `EventoViajeBean` (tope de la foto de un evento) | `2000000` |
| `JWT_SECRET` (≥ 32 caracteres), `JWT_DURACION_MINUTOS` | central | valor de desarrollo, `30` |
| `ADMIN_INICIAL_USUARIO`, `ADMIN_INICIAL_PASSWORD` | central (solo si no hay cuentas internas) | `admin` / valor de desarrollo |
| `AUTENTICACION_INTERNA` y `LDAP_*` | central (backoffice) | `bd`; con `ldap` se usan las `LDAP_*` |
| `GUBUY_ISSUER`, `GUBUY_AUTHORIZATION_ENDPOINT`, `GUBUY_CLIENT_ID`, `GUBUY_CLIENT_SECRET`, `GUBUY_REDIRECT_URI`, `GUBUY_SCOPES` | central (ingreso con gub.uy) | simulador local |
| `COMPOSE_PROFILES=ldap` | docker compose | sin definir (el directorio LDAP no arranca) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | central (trazas a Jaeger) | `http://jaeger:4318`; vacío = no exporta |
| `FRONTEND_URL_WEB`, `FRONTEND_URL_MOVIL` | central (a dónde vuelve el ingreso con gub.uy) | `http://localhost:3000`, `cargauy://ingreso` |

Nada de URLs ni credenciales hardcodeadas en el código: todo por entorno, con un default que
funcione en `docker compose`.

**ID Uruguay testing (ADR-0017):** `docker-compose.gubuy-testing.yml` sobre el Compose base
selecciona el proveedor real de testing y exige `GUBUY_CLIENT_SECRET` no vacío. No cambia
rutas REST, `IngresoGubUyLocal`, `SesionDTO`, destinos web/móvil, roles ni cierre local.
El cliente descubre `userinfo_endpoint`, valida primero el `id_token`, consulta UserInfo
con Bearer y exige igualdad exacta de `sub`. Cédula obligatoria; nombre/correo siguen
siendo opcionales. Fallas de UserInfo impiden el ingreso, sin fallback a datos personales
del `id_token`; la PDI conserva su degradación posterior. Configuración, límites y pruebas
pendientes: [guía de testing](integracion-id-uruguay-testing.md).

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
