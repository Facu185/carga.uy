# Autoridad: consulta de viajes finalizados

Solicitud original: «aplica tu recomendacion», tras recomendar conservar Guías
registradas por creación y agregar Viajes finalizados por instante de FIN aceptado.
Se preserva el diff previo de tipo de carga y el historial ODD anterior.

## Specs

- S1. Dos modos: Guías registradas (predeterminado, sin cambiar contrato previo)
  y Viajes finalizados, según eventos FIN persistidos/aceptados por el central.
  Fecha del evento, no creación, inicio/fin previstos ni recepción. Calendario
  America/Montevideo, días inclusivos, 30 por defecto, máximo 366.
- S2. Viajes finalizados cuenta cada guía una vez; total, empresas distintas,
  evolución diaria y rubros. Conservar tipo de carga opcional y vacío real.
  Guía FINALIZADA sin FIN no se infiere. Deduplicación técnica: primer instante
  FIN por guía (terminal según ADR-0019), calculado antes de limitar el período;
  no repartir una misma guía por días por filas repetidas. Sin escrituras.
- S3. Consulta agregada exclusiva AUTORIDAD y endpoint aditivo; no abrir historial
  individual P3/CHOFER ni cambiar roles, sesión, motor, JMS, ABM o esquema.
  Fiscalización conserva fechas de detección y todos los tipos/casos sin guía.
- S4. Selector de consulta y filtros reproducibles en URL; etiquetas, gráficos,
  tablas y CSV/PDF derivan de respuesta aplicada, no borrador. Explicar sincronización
  tardía y distinguir finalización registrada de constatación física. Sin mezclar
  respuestas de modos diferentes; validar criterio temporal del JSON.
- S5. Pruebas Java/SQL primero y escenarios SPA antes del código. No builds,
  tsc, instalaciones, commits ni auditorías independientes. Validación real y
  aceptación externa P1/P3/P5 pendientes; no cerrar F06 completo ni medir duración,
  volumen/peso real, iniciados, mapa, ubicación, habilitaciones o escalado.

## Tasks

| ID | Specs | Ruta / responsabilidad | Estado | Evidencia |
| --- | --- | --- | --- | --- |
| T1 | S1–S5 | ADR-0023, contratos, pruebas/tablero-autoridad.md y pruebas Java/SQL | completada (escrita, no ejecutada) | ADR/contrato y escenarios SPA; 10 tests nuevos antes del código |
| T2 | S1,S2,S3 | ReporteActividadLocal, Bean/DAO, ViajesFinalizadosRecurso | completada (sin ejecución) | Método/GET aditivo, CTE de FIN y mismo calendario/mapeo |
| T3 | S1,S3,S4 | api/reportes.ts, ReporteActividad.tsx, IndicadoresFiscalizacion.tsx | completada (sin ejecución) | Selector/URL, validación de criterio, unidades y CSV/PDF aplicados |
| T4 | S1–S5 | documentación, diff/imports/enlaces y espejo | completada (estática) | diff --check, 182 enlaces/45 imports, LF/UTF-8 y JSON; SQL/protected files conservados |

## Log

- Usuario informó haber verificado funcionalidad previa, sin detallar escenarios;
  no se registra como ejecución propia ni aceptación integral.
- Se contrastaron memoria #683/#676 y archivos reales. `EventoViajeBean` solo
  acepta FIN en EN_CURSO y lo persiste con transición en la misma JTA; se consumen
  agregados internos, no el endpoint de historial reservado a CHOFER.
- Diseño aditivo: GET `/reportes/viajes-finalizados`, misma forma de agregados y
  `criterioTemporal=FIN_VIAJE`. El reporte anterior permanece por creación.

- T1: 4 pruebas servicio, 1 DAO, 2 SQL/JDBC y 3 recurso escritas antes de
  implementación. Fallo esperado: método/endpoint/SQL de finalizados inexistentes.
  No compiladas ni ejecutadas. T2 iniciada.

- T2: método EJB/endpoint aditivos y SQL reducido por guía; misma validación de
  fechas/carga y completado de series. No se cambian SQL de creación, productor
  de eventos ni fiscalización. `git diff --check` correcto; sin ejecución.

- T3: selector Consulta (vista=finalizados en URL), modo previo predeterminado y
  criterio JSON validado. Rótulos/series/tablas/CSV/PDF salen de datos aplicados;
  avisos de finalización registrada y sincronización tardía. Fiscalización general
  por detección intacta. Sin ejecución frontend. T4 iniciada.

- T4: comprobaciones estáticas correctas en 26 archivos; 182 enlaces y 45 imports
  locales sin faltantes, ejemplo JSON de creación parseado, SQL previo conservado.
  Productor/DAO de eventos y backend de fiscalización conservados por hash.
  Declaradas 40 pruebas en las suites (10 nuevas), ninguna compilada/ejecutada.
- Entrega local concluida con evidencia estática únicamente. Pendientes:
  compilación/ciclo rojo-verde, SQL/JDBC/Hibernate, roles/HTTP/JSON-B de WildFly,
  navegador/teclado/URL/CSV/PDF, rendimiento y aceptación integrada P1/P3/P5.
  No ejecutar builds bajo las instrucciones actuales. No se cierra F06 completo.
