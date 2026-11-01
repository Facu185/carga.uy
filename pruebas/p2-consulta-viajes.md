# F14 / C14 — Consulta de viajes e integración de pesajes C14-P

Escenarios definidos antes del código tras aprobación del usuario el 2026-10-27:
[ADR-0018](../docs/decisiones/0018-consulta-viajes-funcionario-p2.md) y
[C14](../docs/contratos.md#consulta-de-viajes-f14--c14-parte-p2).
La SPA no tiene runner de tests; esta matriz NO es automatización frontend.
No se ejecutan builds, pruebas compiladoras ni llamadas al despliegue.

## Matriz de aceptación pendiente de ejecución

Usar entorno aislado y fixtures sintéticos, sin modificar estados de producción.
Registrar versión desplegada, fecha, identidad sintética, resultado y evidencia
sin tokens ni datos personales. Todos los escenarios están **pendientes**.

| Escenario | Resultado esperado |
| --- | --- |
| Funcionario sin vínculo, empresa activa/inactiva | F12 → Viajes → página y detalle 200; solo lectura. |
| Administrador por API | Ambos GET 200 sin vínculo; recorridos anteriores intactos. |
| Ciudadano/responsable, chofer, autoridad sin roles autorizados | Ambos GET 403 incluso con empresa/guía inexistente; SPA deniega sin consulta. |
| Sin sesión, token inválido/vencido, revocación de acceso | 401/403 según mecanismo existente; sin datos operativos previos. Anotaciones y dobles no acreditan estos rechazos. |
| ID ausente, cero, negativo, decimal, desbordado o mal formado | 400 para actor autorizado; no conversión implícita JAX-RS a 404. |
| Empresa/guía inexistente o detalle con guía de otra empresa | 400 tras autorización; no entregar DTO de otra empresa. |
| Dos empresas con varias guías y mismos inicios | Solo empresa solicitada; inicio/id DESC, sin omisiones/duplicados al paginar sin escrituras concurrentes. |
| Sin filtros; cada uno de los cuatro estados; estado desconocido | Todos / estado exacto / 400; vacío y espacios no filtran. |
| Desde/hasta con Z y offsets distintos | Mismos instantes producen mismos resultados. No depender de la zona del navegador. |
| Un límite, vacío explícito, sin zona, fecha imposible, intervalo igual/invertido | 400; no ejecutar consulta de guías. |
| Viaje que empieza antes y termina después del filtro; viaje multidía | Incluido por solapamiento, no por inicio contenido ni por columna fecha. |
| Viaje termina exactamente en desde o empieza exactamente en hasta | Excluido por intervalo semiabierto. |
| Período y estado combinados | Conteo y página aplican las mismas condiciones y parámetros. |
| Página omitida, tamaño 1/100, negativo/0/101, decimal, offset desbordado | Defaults 0/20; límites válidos; inválidos 400. |
| Empresa sin guías o página fuera de resultados | Vacío diferenciado; conservar total y posibilidad de volver a página anterior. |
| Histórico CREADA sin chofer/vehículo; chofer inactivo/vehículo dado de baja | Listado y detalle sin errores, sin filtrarlos ni inventar asignaciones. |
| Volumen histórico ausente y peso cero | Sin datos de volumen y cero kg, sin confundirlos ni aplicar reglas de creación. |
| Fin previsto pasado con estado ASIGNADA o EN_CURSO | Conservar estado persistido; no mostrar fin real ni deducir finalización. |
| Historial P3 | Mensaje pendiente de integración, no “sin eventos”; ninguna llamada a endpoints supuestos. Pesajes se verifica en la matriz C14-P siguiente. |
| Filtros, página, URL directa, recarga, atrás/adelante, lista → detalle → volver | Empresa, filtros y página preservados; detalle se consulta al servidor, no depende de navegación previa. |
| Respuesta antigua tras cambiar ruta/filtros/empresa o desmontar | Abortada/ignorada; nunca filas de empresa anterior bajo nuevo encabezado. |
| Red, 5xx, DTO inválido, luego reconsulta | Error sin datos anteriores; reintento solo de lectura. |
| Teclado, foco, zoom y pantalla estrecha | Etiquetas, error anunciado, foco visible, tabla accesible y estados no solo por color. |
| Regresión guías/CU2/ABM/JSF y C13 | Permisos de creación, listado empresarial y chofer intactos; no mutaciones nuevas. |

## C14-P: escenarios definidos antes del consumidor (2026-10-31)

El usuario solicitó aplicar la recomendación de integrar primero pesajes por guía,
usando el [contrato P4 publicado](../docs/contratos.md#pesajes-p4). Esta matriz se
redacta antes de implementar el consumidor. No son tests automatizados: la SPA
sigue sin runner y **no se verifica el ciclo rojo/verde**. Todos los escenarios
quedan pendientes de ejecución en un entorno aislado autorizado.

| Escenario | Resultado esperado |
| --- | --- |
| Detalle C14 aún cargando, inválido, denegado o guía de otra empresa | No consultar pesajes ni mostrar registros de una guía anterior. Solo cargar C14-P tras obtener el detalle válido de la empresa solicitada. |
| Histórico sin vehículo asignado | Mostrar consulta no disponible por falta de asignación, no “sin pesajes”; no invocar el endpoint P4 que requiere vehículo. |
| Funcionario / administrador por API | GET `/carga/api/pesajes/guia/{id}` conserva autorización del servidor. La SPA mantiene su recorrido FUNCIONARIO; no amplía rutas de administrador. |
| Respuesta lenta | Carga de pesajes anunciada; el detalle C14 ya visible no se bloquea. |
| Pesadas de dos días o fechas límite | Mostrar las asociaciones devueltas por P4, en su orden, sin reconstruirlas por matrícula/día ni filtrar por fechas previstas del navegador. |
| Peaje nulo/ausente, peso cero o matrícula actual distinta de la guardada en una pesada | Mostrar “Sin datos” para peaje ausente, cero como cero y asociaciones persistidas sin descartarlas por matrícula. |
| `cantidad=0`, `truncado=false`, `nodoDisponible=true` | “No hay pesajes registrados en el central para esta guía”; no afirmar que el viaje no tuvo pesajes ni que la fuente está completa. |
| `nodoDisponible=false`, con registros o sin ellos | Advertir que puede haber pesajes pendientes; conservar registros recibidos. Una lista vacía no se presenta como ausencia de pesajes del viaje. |
| `truncado=true` (hasta 500 filas), con cualquiera de los estados del nodo | Advertencia visible de resultados truncados, cantidad recibida y límite; no inventar total, paginación ni completitud. Las dos advertencias pueden coexistir. |
| Instantes con Z/offset, navegador en otra zona | Instante de balanza y obtención por el central se muestran en America/Montevideo. No inventar fecha global de última sincronización ni usar el período de la guía como inicio/fin real. |
| JSON inválido: guía distinta, `guiaId=null`/ajeno en una pesada, metadatos ausentes o tipos incorrectos | Error de respuesta sin mostrar datos parciales como válidos ni reasociar registros. |
| JSON inválido: cantidad incoherente, más de 500 filas, identificadores repetidos o instantes inválidos (incluido `2026-02-30T10:00Z`) | Error de respuesta; sin tabla con filas duplicadas, fechas normalizadas o datos de otra guía. |
| 400, 403, 5xx o fallo de red al consultar pesajes | Error propio de pesajes con reconsulta de lectura; el detalle C14 permanece visible. No mostrar error de transporte como nodo caído ni como vacío. |
| 401 con sesión guardada | Cliente compartido conserva borrado/aviso de sesión expirada y salida de la ruta protegida; no retener datos visibles tras cerrar sesión. |
| Reconsulta o cambio rápido de guía/empresa y respuesta antigua | Limpiar registros durante la reconsulta; abortar/ignorar respuesta obsoleta al desmontar o cambiar de guía. Filtros/página de vuelta a Viajes se conservan. |
| Teclado, lector de pantalla, zoom y ancho estrecho | Título, carga/error anunciados, botón accesible, tabla con caption, encabezados y etiquetas móviles; advertencias no solo por color. |
| Inspección de peticiones y regresión de historial/CU2/ABM | Solo GET C14 y C14-P; sin POST de sincronización, polling, mapas, descargas de fotos ni llamadas al historial P3. Permisos y mutaciones previos intactos. |

Fixtures sintéticos mínimos para preparar en el entorno de pruebas: guía de una
empresa con vehículo, dos pesadas asociadas en días distintos y una pesada sin
guía que **no** debe devolver C14-P; guía histórica sin vehículo; respuestas
controladas vacías, truncadas, con circuito abierto y fallas HTTP. No se modifican
datos de producción ni se incluyen credenciales en esta matriz.

## Evidencia automatizada escrita y límites

- `ConsultaGuiasBeanTest`: autorización explícita antes de existencia, validación,
  parametrización/orden de consultas, DTO/listado/detalle y datos históricos.
  Dobles que rechazan escrituras; NO ejecutan JPQL, roles del contenedor ni JTA.
- `ConsultaGuiasRecursoTest`: GET, conversión numérica, delegación y propagación
  a mappers existentes; NO acredita HTTP/JSON-B ni seguridad del contenedor.
- Conservar regresiones existentes `GuiaServicioBeanTest`, C13 y F11.
- **Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**

## Comprobaciones estáticas realizadas (2026-10-27)

- `git diff --check` sin errores y revisión de espacios/LF en archivos nuevos.
- Referencias de imports propios/relativos y enlaces Markdown locales, incluidos
  sus fragmentos, comprobados en los archivos afectados.
- Contraste de firmas EJB/REST, DTO y consumidor; veinte métodos JUnit escritos
  (quince de servicio y cinco de recurso), sin ejecutarlos.
- Servicios de guías anteriores, entidad, migraciones, autenticación, cliente
  HTTP, POM y dependencias frontend sin cambios. No se instalaron herramientas.

Estas comprobaciones NO validan compilación Java/TypeScript, JPQL, JSON-B,
autorización efectiva, funcionamiento visual ni integración real.

## Comprobaciones estáticas del consumidor C14-P (2026-10-31)

- `git diff --check` sin errores. Revisión de formato LF/salto final, 142 enlaces
  locales con fragmentos y 19 imports relativos/37 nombres exportados en los 14
  archivos del conjunto de cambios, incluidos los dos archivos TypeScript nuevos.
- Contraste del consumidor con `PesajesDeGuiaDTO`, `PesadaRegistradaDTO`, recurso,
  EJB y DAO P4. Peaje admite ausencia; la asociación almacenada no se reconstruye
  por matrícula ni por período previsto en el navegador.
- Revisión de los dos consumidores de `DatosViaje`: listado y detalle conservan
  historial P3 pendiente; el listado deriva al detalle para consultar pesajes.
- Sin cambios en Java, autenticación, rutas, esquema, dependencias ni scripts npm.
  Se conservó el reparto ya editado en el registro de fase 0.
- No se ejecutaron tests, TypeScript, builds, aplicación, HTTP ni navegador. No hay
  tests automatizados frontend nuevos: únicamente escenarios previos al código,
  pendientes por ausencia de runner. **Ciclo rojo/verde no verificado.**

## Dependencias que NO cierra esta entrega

C14-E P3: lectura de estado/historial real autorizada para funcionario.
C14-P P4: aceptación ejecutada de consumidor/servidor, límite y disponibilidad;
el contrato ya existe y esta entrega implementa su consumo, no paginación nueva.
P3 ya invoca el aviso de fin en código; su ejecución P3 → P4, offline y
concurrencia inicio/baja siguen pendientes de acreditar en fase 6.
No se declara F14 completo ni fase 5 validada por incorporar el consumidor P4.
