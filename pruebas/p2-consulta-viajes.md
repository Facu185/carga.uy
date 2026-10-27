# F14 / C14 — Preparación de viajes limitada a P2

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
| Historial/pesajes | Mensaje pendiente de integración, no “sin eventos/pesajes”; ninguna llamada a endpoints supuestos o sincronización. |
| Filtros, página, URL directa, recarga, atrás/adelante, lista → detalle → volver | Empresa, filtros y página preservados; detalle se consulta al servidor, no depende de navegación previa. |
| Respuesta antigua tras cambiar ruta/filtros/empresa o desmontar | Abortada/ignorada; nunca filas de empresa anterior bajo nuevo encabezado. |
| Red, 5xx, DTO inválido, luego reconsulta | Error sin datos anteriores; reintento solo de lectura. |
| Teclado, foco, zoom y pantalla estrecha | Etiquetas, error anunciado, foco visible, tabla accesible y estados no solo por color. |
| Regresión guías/CU2/ABM/JSF y C13 | Permisos de creación, listado empresarial y chofer intactos; no mutaciones nuevas. |

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

## Dependencias que NO cierra esta entrega

C14-E P3: lectura de estado/historial real, transiciones y disponibilidad.
C14-P P4: pesajes por guía, varios días, paginación y disponibilidad.
Aviso real de fin P3 → P4, offline y concurrencia inicio/baja siguen en fase 6.
No se declara F14 integrado ni fase 5 validada por preparar P2.
