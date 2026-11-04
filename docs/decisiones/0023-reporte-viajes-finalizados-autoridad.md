# ADR-0023 — Viajes finalizados por evento FIN en el tablero de autoridad

**Estado:** Aceptada para la recomendación solicitada por el usuario; ejecución y aceptación integrada pendientes.
**Fecha:** 2026-11-04

## Contexto y confirmación

Tras probar el filtro de carga, el usuario consultó por viajes efectivamente
realizados. Se recomendó separar Guías registradas por creación de Viajes
finalizados por el instante de FIN aceptado por el central. El usuario pidió
«aplica tu recomendacion». No implica aprobación individual de otros frentes.

## Decisión

- Conservar ADR-0020 y `/reportes/actividad` como consulta predeterminada por
  creación. Añadir modo Viajes finalizados y GET `/reportes/viajes-finalizados`,
  agregado exclusivo AUTORIDAD, sin entregar eventos ni ampliar permisos P3.
- Usar `evento_viaje.tipo='FIN'` y su `instante`, no `recibido_en`, creación de
  guía ni fechas previstas. Una guía creada fuera del período puede terminar
  dentro. El estado FINALIZADA sin FIN no permite inferir cuándo terminó.
- Cada guía representa un viaje y cuenta una vez. FIN es terminal según ADR-0019;
  se agrupa por guía usando el menor instante FIN para impedir doble conteo ante
  filas repetidas. La reducción ocurre antes del rango, manteniendo un único día
  por guía incluso al consultar períodos diferentes; no altera eventos originales.
- Calendario Montevideo y límites de ADR-0020; tipo de carga declarado en guía,
  totales, empresas distintas, evolución y rubros. Estados mostrados son actuales
  de las guías seleccionadas, no históricos. Sin duración ni carga real medida.
- Misma forma de DTO con `criterioTemporal=FIN_VIAJE`; `totalGuias` expresa aquí
  guías distintas con FIN (viajes), y `empresasConGuias` sus empresas distintas.
  No renombrar campos ni cambiar respuesta de consumidores anteriores.
- Selector y filtros en URL; conservar nombres/criterio aplicado en pantalla,
  tablas y CSV/PDF. Los eventos offline recibidos después pueden actualizar
  períodos anteriores: no es reporte congelado ni constatación física del viaje.
- Fiscalización mantiene su consulta general por detección del caso. No se
  interpreta tasa de confirmación como proporción sobre viajes finalizados.

## Alternativas y límites

- Filtrar FINALIZADA sobre guías creadas en el período: más pequeño, pero mide
  otra población y excluye viajes creados antes. No responde a la consulta pedida.
- Cambiar el criterio del endpoint previo: rompe significado publicado. Se
  prefiere endpoint aditivo y lógica de calendario/completado reutilizada.
- Fecha de recepción: más fácil de observar en el servidor, pero traslada viajes
  offline al período de sincronización. Se usa instante aceptado del evento.
- No se cambia esquema, productor P3, motor, JMS, ABM ni autenticación. P5 conserva
  responsabilidad funcional; no se acepta globalmente ADR-0004/0022 ni se cierra F06.
- El costo de agregación sobre eventos debe medirse con datos representativos;
  no se promete rendimiento ni se agregan índices sin evidencia.

## Validación

Pruebas primero, sin compilar ni ejecutar por restricción de builds. Validación
SQL/JDBC/Hibernate, roles WildFly, JSON-B, navegador y aceptación compartida
pendientes en [matriz](../../pruebas/tablero-autoridad.md).
[Seguimiento ODD](../../odd/tasks/autoridad-viajes-finalizados.md).
