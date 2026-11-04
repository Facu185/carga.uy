# Autoridad: filtro de tipo de carga en actividad

Solicitud confirmada: «aplica tu recomendacion», tras recomendar filtrar solo la
actividad de guías, mantener fiscalización general por período y distinguir ambos
alcances en pantalla, CSV y PDF. Entrega parcial; P5 conserva propiedad funcional.

## Specs

- S1. Selector «Todos los tipos / tipo específico» para actividad: totales de
  guías, empresas con guías, estados, días y rubros. Todos conserva comportamiento
  anterior, incluidas guías sin tipo y empresas inactivas. Filtrado en servidor,
  antes de agregar, por `guia.tipo_carga_id`; sin cambios de esquema ni escrituras.
- S2. Fiscalización solo por período; no excluir casos sin guía ni inferir su
  carga. Explicar alcance y criterios temporales (creación de guías frente a
  detección de casos) en pantalla y exportaciones. El CSV identifica filtro
  aplicado, no borrador; PDF es impresión y conserva esas etiquetas.
- S3. Contrato aditivo: mantener consulta EJB de dos argumentos y REST sin filtro;
  parámetro opcional `tipoCargaId`, ausente = todos; blanco, no entero positivo,
  desbordamiento o ID inexistente = 400. Metadatos opcionales del filtro en JSON.
  Catálogo de lectura bajo reporte con permisos AUTORIDAD existentes, sin ampliar
  `GuiaServicioBean`. Sin cambiar sesión P1, roles, fiscalización, JMS ni ABM.
- S4. Mantener calendario Montevideo, máximo 366 días, error distinto de vacío,
  una sentencia para agregados, protección ante respuestas tardías y URL
  reproducible. Pruebas Java primero; escenarios frontend antes del cambio.
  Sin builds, `tsc`, instalaciones, commits ni validación funcional inventada.
- S5. Fuera de alcance: ubicación, empresas/vehículos habilitados, escalado
  horizontal, filtro global de fiscalización y aceptación externa P1/P5.

## Tasks

| ID | Specs | Ruta / responsabilidad | Estado | Evidencia |
| --- | --- | --- | --- | --- |
| T1 | S1–S5 | ADR-0020, contratos, pruebas/tablero-autoridad.md y tests ReporteActividad* | completada (escrita, no ejecutada) | Contrato/ADR y escenarios definidos; 6 tests servicio, 1 DAO, 1 SQL y 1 recurso nuevos antes de código |
| T2 | S1,S3,S4 | carga-api, ReporteActividadBean/DAO/Recurso | completada (sin ejecución) | Sobrecarga compatible, filtro SQL parametrizado, metadatos y catálogo exclusivos de reporte |
| T3 | S1,S2,S4 | api/reportes.ts, ReporteActividad.tsx, IndicadoresFiscalizacion.tsx | completada (sin ejecución) | Selector/URL, validación JSON, rótulos aplicados y CSV por ámbitos; impresión conserva avisos |
| T4 | S1–S5 | docs, revisión de diff y comprobaciones estáticas | completada (estática) | diff --check, 147 enlaces, 39 imports, UTF-8/LF y ejemplo JSON correctos; runtime pendiente |

## Log

- Solicitud original: «aplica tu recomendacion». Alcance confirmado por referencia
  a recomendación inmediatamente anterior: filtro solo de actividad y fiscalización
  general explícitamente separada.
- Exploración: árbol limpio; tipos de carga ya modelados. `GuiaServicioBean`
  restringe catálogo a CIUDADANO: se agrega lectura específica de reporte sin
  cambiar sus permisos. Fiscalización consulta por fechas y exporta junto a actividad.
- Control: ninguna ejecución Java/TypeScript/SQL acredita aún el comportamiento.

- T1: pruebas escritas primero y escenarios frontend definidos. Fallo esperado antes
  del código: sobrecarga, catálogo y metadatos inexistentes. `git diff --check`
  correcto; NO se ejecutó rojo/verde ni compilación. T2 iniciada.

- T2: implementados contrato aditivo, validación, catálogo y CTE filtrada. Sin
  cambios en GuiaServicioBean, fiscalización ni esquema. Revisión estática y
  `git diff --check` correctos; SQL/Hibernate/roles reales pendientes. T3 iniciada.

- T3: filtro opcional y catálogo independiente con carga/vacío/error/reintento.
  Respuesta validada por filtro; rótulos/CSV salen de metadatos aplicados. Aviso
  de fiscalización general imprimible y protección ante respuestas tardías.
  Sin runner frontend ni ejecución visual. T4 iniciada; se añade cobertura
  complementaria del mapeo de catálogo DAO (sin ejecución).

- T4: 21 archivos revisados estáticamente; 147 enlaces locales y 39 imports sin
  destinos ausentes; ejemplo JSON de actividad parseado. SQL original sin filtro
  y RolesAllowed conservados por comparación textual. 30 pruebas declaradas
  (10 nuevas: 9 antes de código, 1 complemento DAO); ninguna ejecutada.
- Una comprobación inicial global de ejemplos JSON encontró un ejemplo no estricto
  preexistente en HEAD fuera de actividad; no se amplió alcance ni se corrigió.
- Entrega local: T1–T4 implementadas/revisadas según evidencia indicada. Aceptación
  funcional, compilación/tipos, HTTP/JSON-B, Hibernate/PostgreSQL, navegador/PDF y
  coordinación P1/P5 PENDIENTES. Ubicación, habilitaciones y escalado no tocados.
