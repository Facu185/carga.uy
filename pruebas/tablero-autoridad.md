# Aceptación pendiente — Tablero de autoridad (ADR-0020)

**Estado:** escenarios definidos antes del código; no ejecutados. No hay runner
frontend en package.json; no se instalaron herramientas ni se ejecutaron builds.
Las pruebas Java tampoco acreditan ciclo rojo/verde, roles EJB ni HTTP reales.

## Regresión TS2322 del criterio temporal (2026-11-04, antes de la corrección)

El log aportado por el usuario registra TS2322 en `api/reportes.ts:113`: el
retorno asigna `doc.criterioTemporal` (`unknown`) a `CriterioActividad`.
Escenarios definidos antes del cambio, NO pruebas automatizadas: `package.json`
sigue sin runner frontend. No se ejecutan builds ni `tsc`.

1. Solicitar Guías registradas con JSON válido y `CREACION_GUIA`: aceptar y
   conservar ese criterio en el reporte interpretado.
2. Solicitar Viajes finalizados con JSON válido y `FIN_VIAJE`: aceptar y conservar
   ese criterio, tanto con resultados como con reporte vacío y series completas.
3. Recibir el criterio del otro modo: rechazar en ambos sentidos, sin gráficos ni
   exportaciones de una respuesta incompatible.
4. Criterio ausente, nulo, numérico o texto desconocido: rechazar; no sustituir
   silenciosamente un valor inválido por el criterio solicitado.
5. El retorno debe usar el parámetro `criterio` tipado, solo después de comprobar
   igualdad con el JSON; no introducir `as`, `any` ni quitar la validación.

Pendientes: comprobar compilación sin TS2322 y ejecutar los escenarios en el
navegador. Ciclo rojo/verde NO verificado por el agente; el fallo observado es
el log del usuario, no una ejecución propia.

## Backend y contrato

1. AUTORIDAD obtiene 200 sin vínculo empresarial. Sin sesión 401; ciudadano,
   chofer, funcionario y administrador 403, antes de leer guías. Token vencido y
   clave temporal conservan comportamiento P1. Verificar atravesando WildFly.
2. Sin límites, 30 días incluido hoy local; reloj fijo en 2026-11-02T01:00Z
   corresponde aún al 1 de noviembre en Montevideo.
3. Rango de un día válido y de 366 días válido; 367, invertido, imposible,
   espacios, año cero, fechas con hora y par incompleto: 400 seguro.
4. Inclusión exacta a medianoche local, exclusión de medianoche siguiente,
   incluyendo instantes que en UTC caen en el día siguiente. La agrupación no
   depende de la zona del navegador, JVM o sesión PostgreSQL.
5. Guías creadas dentro con inicio previsto fuera se incluyen; guías creadas
   fuera con inicio previsto dentro se excluyen. No usar columna heredada fecha.
6. Misma empresa con varias guías cuenta una vez; incluir empresa inactiva,
   guía sin asignaciones y sin rubro. Estados actuales, no reconstruidos.
7. Totales, estados, rubros y evolución coinciden; faltantes diarios/estados son
   cero. Vacío real devuelve 200 y cero; error de base NO devuelve cero.
8. Catálogo con rubro llamado «Sin rubro» y guía sin rubro son grupos distintos.
   Orden estable y sin truncado de guías. Verificar SQL/JDBC/Hibernate reales.
9. Única sentencia de agregación; sin escrituras ni locks explícitos. Altas o
   cambios de estado concurrentes no mezclan instantáneas entre distribuciones.
10. JSON-B mantiene nombres/enteros/arreglos y no serializa entidades; errores
    `{error:mensaje}`; respuesta no almacenada (`Cache-Control: no-store`).
11. Medir plan y latencia con volumen representativo antes de decidir índices.
    El límite temporal limita la serie, no garantiza costo constante de consulta.

## Navegador

1. Autoridad entra a `/backoffice` y ve reporte; navegación a auditoría y cambio
   de clave intactos. Administrador conserva inicio/usuarios/nodos existentes;
   otros roles no acceden al reporte ni por URL directa.
2. Carga, éxito, vacío real, 400, 403, 401, 500, red caída y recuperación manual:
   fallos sin tarjetas numéricas engañosas ni datos anteriores bajo filtros nuevos.
3. Cambiar/aplicar rápidamente filtros y retrasar respuestas: solo gana la última
   consulta; desmontar pantalla cancela/ignora respuestas. Reintento vuelve a
   consultar el período aplicado, no un borrador sin enviar.
4. Mostrar período aplicado distinto del borrador y advertencia de estado actual.
   30 días iniciales vienen del servidor; aplicar otro rango y recargar lo conserva
   en URL. Atrás/adelante restaura consulta y campos.
5. Cada gráfico tiene tabla con los mismos datos, caption y encabezados; teclado,
   foco en error de formulario, lector de pantalla y móvil sin depender del color.
6. JSON inválido, conteo negativo/no seguro, grupos repetidos, días omitidos,
   totales incoherentes, período/zona/criterio incorrectos: mostrar error, no gráfico.
7. Sin llamadas a monitor/nodos, sincronización, eventos individuales ni nuevos
   permisos para consultar detalle de empresa/guía.

## Evidencia por registrar

Resultados de pruebas Java, PostgreSQL/Hibernate y contenedor; capturas y recorrido
de teclado/navegador; aceptación de P1/P5. No marcar cerrado por presencia de código.

## Entrega local y comprobaciones realizadas (2026-11-02)

- 20 pruebas Java escritas: 12 de servicio (autorización explícita, calendario,
  límites, vacío/fallo y completado), 3 de DAO (delegación/mapeo), 3 de recurso
  (delegación/cabecera/errores) y 2 de SQL/JDBC con Testcontainers. Ninguna fue
  compilada ni ejecutada; fixture SQL mínimo no sustituye migraciones/Hibernate.
- `git diff --check` sin incidencias. Revisión estática adicional de archivos
  nuevos/modificados: espacios finales/LF, existencia de enlaces locales Markdown
  e imports Java del proyecto. Ejemplo JSON del contrato parseado y sus sumas
  contrastadas. Esto NO valida sintaxis Java/TypeScript/SQL ni comportamiento.
- Contraste de fechas históricas de fixtures con tzdata del entorno: referencia
  auxiliar, no ejecución de las pruebas Java ni verificación del runtime destino.
- No hay `node_modules` ni ejecutable `oxlint` disponible; no se instaló nada.
  No se ejecutaron linters, `tsc`, Maven, Docker ni navegador.
- **Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**

## Regresión del filtro de tipo de carga (2026-11-03, antes del código)

Escenarios frontend definidos, NO pruebas automatizadas: `package.json` no tiene
runner de tests. No se instala uno ni se ejecutan builds/TypeScript.

1. Sin tipo en URL: conservar todos los agregados e incluir guías sin tipo.
   Selector muestra «Todos los tipos». Catálogo usa solo endpoint de reportes;
   ningún cambio en permisos operativos, autenticación ni fiscalización.
2. Seleccionar tipo y aplicar: todos los agregados de actividad, tablas y gráficos
   usan el mismo subconjunto. Recarga/atrás/adelante conservan fechas y tipo en URL.
   Cambiar solo el borrador NO cambia rótulos, resultados ni exportaciones.
3. Tipo existente sin guías: ceros y serie completa, nombre del filtro conservado;
   no ocultar infracciones generales porque actividad esté vacía.
4. Tipo inválido en URL (blanco, cero, negativo, texto, desbordamiento, inexistente):
   error visible, no cifras anteriores ni ceros fabricados. Con selector inválido
   no sustituir silenciosamente por «Todos»; permitir quitarlo explícitamente.
5. Catálogo cargando/vacío/fallido: distinguir estados, reintentar fallo sin perder
   filtro actual. La actividad sin filtro sigue disponible aunque falle catálogo.
   Tipo filtrado ya aplicado muestra nombre del servidor, no un nombre inventado.
6. Fiscalización se consulta solo con desde/hasta; cambiar carga no cambia su
   población. Aviso visible: todos los tipos y casos sin guía, por detección.
7. CSV: identifica ID/nombre aplicado a actividad, ambos criterios temporales y
   alcance general de fiscalización. PDF: esos rótulos siguen visibles al imprimir
   aunque se oculte formulario. Nunca rotular fiscalización como filtrada por carga.
8. Cambios rápidos A/B, quitar tipo, reintentar, respuesta tardía y desmontaje:
   solo se muestra/exporta la consulta vigente. JSON de filtro incoherente, nombre
   vacío, ID inseguro o servidor antiguo ignorando filtro: error, no gráficos.
9. Teclado, foco/error asociado, móvil, sesión vencida/403 y roles directos:
   mantener manejo existente; catálogo también exige AUTORIDAD en servidor.

Pruebas Java/SQL por escribir primero en las suites ReporteActividad existentes:
propagación/validación/ausencia del filtro, catálogo/autorización, delegación REST,
SQL parametrizado antes de agregar y fixture PostgreSQL con distintos tipos/null.
Rojo/verde, HTTP/JSON-B, Hibernate y navegador pendientes por restricción de builds.

## Entrega del filtro y evidencia estática (2026-11-03)

- Contrato aditivo y ampliación de ADR-0020 registrados antes del código. Nueve
  pruebas nuevas escritas primero (6 servicio, 1 DAO de filtrado, 1 SQL/JDBC y
  1 recurso) y regresiones existentes adaptadas; una prueba DAO complementaria
  cubre mapeo del catálogo, orden solicitado y ausencia explícita. Total actual:
  30 pruebas (18 servicio, 5 DAO, 3 SQL/JDBC, 4 recurso), ninguna ejecutada.
- Selector y catálogo independientes de actividad; ID/nombre aplicado validado
  en JSON, incluido reporte vacío. Sin filtro admite metadatos ausentes/nulos
  para compatibilidad; filtrado rechaza servidor que ignore la selección.
- API de fiscalización/motor/ABM/roles operativos sin cambios. Exportaciones
  distinguen filtro de actividad frente a fiscalización general por detección.
- Sin node_modules/oxlint ni runner frontend; no se instaló nada. La impresión
  PDF se conserva, con avisos fuera del formulario oculto; revisión visual real
  pendiente, no acreditada por leer CSS/JSX.
- **Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
  Validación WildFly, HTTP/JSON-B, Hibernate/PostgreSQL, navegador y aceptación
  externa P1/P5 pendientes. No cierra el requisito completo de filtros F06.

Comprobaciones estáticas de esta ampliación: `git diff --check` correcto; 147
referencias locales Markdown y 39 imports locales Java/TS revisados en 21 archivos,
sin destinos ausentes; UTF-8/LF/espacios finales correctos. El JSON del ejemplo de
actividad se pudo parsear. Una pasada inicial de todos los ejemplos JSON del
contrato falló en un ejemplo no estricto ya presente en HEAD y ajeno al reporte;
no se modificó y se acotó el control al ejemplo de actividad. Comparación textual:
SQL sin filtro y RolesAllowed del reporte conservados, sin cambios en migraciones,
GuiaServicioBean ni backend fiscalización. No equivale a compilación ni ejecución.

## Escenarios de Viajes finalizados (2026-11-04, antes del código)

Ampliación confirmada: ADR-0023. Escenarios SPA, no tests automatizados: no hay
runner frontend y no se instala uno. Java/SQL se escriben antes de implementar.

1. Consulta predeterminada Guías registradas conserva endpoint, fechas y resultados.
   Elegir Viajes finalizados y aplicar cambia endpoint/criterio, no motor ni roles.
2. Guía creada fuera del rango con FIN dentro sí cuenta. Creada dentro con FIN
   fuera, sin FIN o solo INICIO no cuenta; FINALIZADA sin FIN tampoco se infiere.
   Usar instante del evento y Montevideo, no recibido_en ni fin previsto.
3. Totales de viajes, empresas, días y rubros usan el mismo conjunto, una vez por
   guía. Repetir filas FIN no multiplica conteos; reducción antes del rango.
   Todos incluye sin tipo; filtro específico conserva metadatos aunque sea vacío.
4. Conservar 30 días por defecto, hasta 366 inclusive, medianoches y cambios de
   horario histórico; filtros inválidos/roles indebidos no consultan agregados.
5. Selector de consulta y filtros en URL, recarga/atrás/adelante y quitar carga.
   Vista desconocida en URL debe fallar explícitamente, no volver a guías en silencio.
   Cambiar borrador no modifica resultados ni etiquetas/exportación aplicados.
6. Cambiar rápido modo/fechas/carga o reintentar: descartar respuestas tardías;
   no mostrar/exportar datos anteriores bajo la nueva consulta. JSON con criterio
   de creación al pedir finalizados (o inverso) es error, no gráfico.
7. Gráfico/tabla de evolución, rubros y tarjetas cambian de unidad/fecha según
   respuesta. CSV/PDF identifican modo, carga y criterio (evento FIN frente a creación).
   Avisar que sincronizaciones tardías pueden actualizar períodos anteriores.
8. Fiscalización sigue general por fechas de detección, sin filtro de carga o
   población finalizada; su tasa no se calcula sobre el total de viajes.
9. Navegador: estados carga/vacío/fallo, 400/401/403/500, teclado/foco y móvil.
   PDF conserva avisos aunque oculte formulario. No se abre historial P3 a AUTORIDAD
   ni se introducen peticiones por cada guía ni PII en el reporte.

Pendientes: ejecución Java/SQL/JDBC/Hibernate/WildFly/JSON-B, navegador/CSV/PDF y
aceptación integrada. No dar por cerrados F06 ni iniciados/carga física/duración.

## Entrega local de Viajes finalizados y controles (2026-11-04)

- 10 pruebas nuevas escritas antes del código: 4 servicio (calendario, filtros,
  metadatos, errores/permisos), 1 DAO, 2 SQL/JDBC y 3 recurso nuevo. Total actual
  40 declaraciones @Test en estas suites: 22 servicio, 6 DAO, 5 SQL/JDBC, 4 recurso
  de actividad y 3 de finalizados. Ninguna compilada ni ejecutada.
- Fixture SQL cubre creación/recepción fuera del período, FIN en los bordes,
  FINALIZADA sin FIN, solo INICIO, tipo/null, rubro/null, conteo distinto de empresas,
  repetición de FIN y primer FIN anterior al rango. Es fixture mínimo temporal,
  no prueba de migraciones, Hibernate, contenedor ni datos reales.
- `git diff --check` correcto. Revisión estática de 26 archivos con UTF-8/LF,
  182 enlaces locales y 45 imports locales sin destinos ausentes; ejemplo JSON
  del reporte de creación parseado. SQL previo de creación conservado textualmente.
  Hashes de productor/DAO de eventos y backend de fiscalización conservados.
- AplicacionRest no enumera recursos: el nuevo recurso usa descubrimiento de
  anotaciones existente. No se ejecutó servidor/HTTP para acreditar registro real.
- Sin node_modules/runner frontend, builds, tsc, linters, Docker ni navegador.
  **Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
- Validación funcional (JSON-B, Hibernate/PostgreSQL, roles reales, teclado,
  URL, CSV/PDF y eventos offline tardíos), volumen/rendimiento y aceptación
  compartida pendientes. No se da por cerrado F06 completo.
