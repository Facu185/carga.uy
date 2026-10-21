---
name: contratos-integracion
description: >-
  Preserva compatibilidad entre frentes de carga.uy al cambiar interfaces de
  carga-api, DTO, rutas/status REST, SOAP o mensajes JMS. Usar para evoluciones
  de contrato y sus consumidores; no resuelve políticas ni rompe contratos sin acuerdo.
---

# Contratos entre frentes

## Preparación

Lee [AGENTS.md](../../../AGENTS.md), [CODEX](../../../CODEX/README.md),
[contratos](../../../docs/contratos.md), [frentes](../../../docs/frentes.md) y
[discrepancias](../../../CODEX/contexto-proyecto.md). El
[estándar API](../../../docs/Estandares/api-design.md) orienta el diseño, pero
no autoriza romper el comportamiento publicado cuando difiere del contrato actual.

## Procedimiento

1. Localiza declaración, productor, consumidores y pruebas con búsquedas del símbolo,
   ruta, campo o cola. Incluye SPA, nodos y simulador cuando correspondan. No deduzcas
   compatibilidad únicamente de que compile una interfaz Java.
2. Describe el cambio antes de editar:

   | Frontera | Elementos que comparar |
   | --- | --- |
   | Interfaz EJB | Firma, excepciones, pre/postcondiciones, roles y transacción |
   | REST | Método/ruta, parámetros, DTO, status, headers y forma de error |
   | SOAP | Operación, namespaces, tipos JAXB y tratamiento de fallas |
   | JMS | Destino, JSON, propiedades, ID estable y semántica de reentrega |

3. Conserva interfaces/DTO públicos en `carga-api`; no expongas entidades JPA ni
   tipos de implementación. Sigue serialización, constructor/accesores e ISO-8601
   del contrato, incluida la excepción SOAP de fechas que allí se documenta.
4. Evalúa campos nuevos con sus valores ausentes/nulos y consumidores reales.
   No asumas que un campo adicional es inocuo si pasa a ser obligatorio. Renombrar,
   quitar, cambiar unidades/tipos, permisos o errores requiere coordinación.
5. Preserva `{"error":"mensaje"}` y los status publicados mientras no exista
   evolución acordada. Contrasta
   [Errores.java](../../../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/Errores.java),
   [NegocioExceptionMapper](../../../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/NegocioExceptionMapper.java)
   y [cliente SPA](../../../carga-front/src/api/cliente.ts); no sustituyas por el
   sobre propuesto en el estándar de forma unilateral.
6. Ante incompatibilidad o política sin resolver, presenta evidencia, consumidores
   afectados y alternativas (adaptador temporal, campo opcional o versión nueva)
   con costos. Pregunta y DETENTE; no publiques una decisión por el equipo ni
   cambies el estado de un ADR por iniciativa propia.
7. Tras acuerdo, actualiza la fuente contractual pertinente junto al cambio, sin
   duplicarla en esta skill. Conserva correlación y errores seguros; no confundas
   aceptación asíncrona con procesamiento completado.

## Validación y salida

Escribe primero pruebas de compatibilidad/regresión con payloads mínimos, campos
ausentes, errores y consumidores relevantes. Si es auditoría solicitada, limita
la salida a hallazgos y pruebas propuestas; no edites contratos ni código.

Desde la raíz, `git diff --check` y búsqueda de usos de símbolos/rutas cambiados.
No ejecutes builds ni generadores que compilen. Entrega matriz de fronteras
afectadas, compatibilidad conservada o pendiente de acuerdo y pruebas no ejecutadas.
