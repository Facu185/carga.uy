# ADR-0018 — Consulta de viajes C14 y preparación F14 limitada a P2

**Estado:** Aceptada para C14 y entrega parcial P2 por confirmación del usuario
**Fecha:** 2026-10-27

## Contexto

La fase 5 requiere consultar guías desde el área del funcionario. El listado
empresarial existente exige RESPONSABLE/ACTIVO y no admite funcionario. P3
conserva el estado/historial real y P4 los pesajes; sus contratos C14-E/C14-P
siguen pendientes. El usuario aprobó preparar primero la parte P2 y confirmó
explícitamente el contrato siguiente, sin atribuir aprobación a otros frentes.

## Decisión

- Servicio `ConsultaGuiasLocal` separado del servicio de creación y lectura
  empresarial: `consultar(Long empresaId, String desde, String hasta,
  String estado, Integer pagina, Integer tamano)` y
  `detalle(Long empresaId, Long guiaId)`. Solo FUNCIONARIO/ADMINISTRADOR, sin
  vínculo; autorizar en EJB antes de consultar existencia.
- GET `/carga/api/guias/consulta` y `/carga/api/guias/consulta/{id}`. Ambos
  requieren `empresaId` positivo; incluir empresas inactivas y asignaciones
  históricas ausentes o actualmente dadas de baja. No modificar contratos
  de `/guias/empresa`, creación, catálogos ni consulta del chofer.
- Período opcional como par `desde`/`hasta`, instantes ISO-8601 con offset o Z,
  con `desde < hasta`. Filtrar solapamiento previsto con `[desde, hasta)`:
  `g.inicio < hasta AND g.fin > desde`. No filtrar por la fecha heredada ni por
  inicio contenido. Un límite ausente, vacío, sin zona o inválido responde 400.
- `estado` opcional recortado; ausente/blanco no filtra. Valores exactos:
  CREADA, ASIGNADA, EN_CURSO, FINALIZADA. No deducir estado de las fechas ni
  reconstruir eventos o instantes reales.
- Página base 0, tamaño predeterminado 20, rango 1–100, desplazamiento máximo
  `Integer.MAX_VALUE`; orden `inicio DESC, id DESC`. Página fuera de resultados
  conserva el total. Conteo y filas no prometen instantánea ante escrituras.
- `PaginaConsultaGuiasDTO`: `{registros, pagina, tamano, total}`. Cada
  `ConsultaGuiaDTO`: `{guia: GuiaDTO, nombreEmpresa, matricula, nombreChofer}`;
  detalle devuelve el mismo DTO, restringido a la empresa solicitada. Nombre,
  matrícula y chofer son datos relacionados actuales, no una instantánea del
  viaje. Asignaciones ausentes y volumen histórico nulo se conservan; no se
  aplican reglas de creación a históricos ni se rellenan datos ficticios.
- Respuestas 200; errores `400 {"error":"mensaje"}` para parámetros inválidos,
  empresa/guía inexistente o guía que no pertenece a la empresa seleccionada;
  401 sin sesión válida, 403 sin permiso. Conservar mappers, Bearer y correlación.
- SPA `/funcionario/empresas/:id/viajes` y `.../viajes/:guiaId`, enlazada desde
  F12, con filtros, paginación, detalle y carga/vacío/error. Mantener el acceso
  SPA de FUNCIONARIO existente; ADMINISTRADOR conserva la consulta por API.
  Historial y pesajes se muestran como **pendientes de integración**, no vacíos
  ni como fallas de nodos acreditadas. No consultar endpoints supuestos.

## Alternativas y consecuencias

- Ampliar roles de `GuiaServicioBean` o quitar pertenencia: menos código, pero
  puede conceder creación/lecturas ajenas y rompe consumidores; descartado.
- Esperar P3/P4: entrega integrada, con mayor dependencia externa; se elige
  preparar P2 ahora, sin declarar F14 cerrado.
- Servicio/DTO de lectura separado: pequeño contrato nuevo y mapeo explícito,
  sin cambios de esquema, dependencias, eventos ni reglas CU2.

No se agregan edición/cancelación, mapas, polling ni sincronización manual.
No se aprueba la política offline ni C14-E/C14-P. Pruebas primero; no ejecutar
builds. Seguridad efectiva, JPQL, JSON y navegador requieren validación posterior.
