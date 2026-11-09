# ADR-0024 — Vista previa orientativa de guías con IDE Uruguay y ORS

**Estado:** Aceptada para el flujo solicitado por el usuario; implementación local escrita, validación real pendiente.
**Fecha:** 2026-11-08

## Contexto y confirmación

El usuario pidió implementar sugerencias IDE, cálculo ORS por peso total y mapa
para el responsable al crear guías, reutilizando el mapa existente. Excluyó
dimensiones y carga por eje. Dispone de clave ORS; no se accede a su valor.

## Decisión acotada

- Añadir una vista previa en la SPA existente: IDE candidates → find → puntos;
  ORS driving-hgv → recorrido. Backend intermediario con clave privada. Leaflet
  1.9.4 y OSM como el panel JSF, sin reutilizar acceso a flota ni cambiar el panel.
- Peso considerado = peso vacío registrado del vehículo + carga declarada,
  convertido de kg a toneladas en servidor. Solo peso; no modelar remolques ni
  inferir dimensiones/ejes. No equivale a permiso ni garantía de camino apto.
- Contrato nuevo `/planificacion-guias` de lectura/cálculo, sin escrituras; solo
  CIUDADANO con RESPONSABLE/ACTIVO en empresa activa, compatibilidad ADR-0015 y
  pertenencia del vehículo verificadas por principal en EJB. No unifica permisos
  heredados de CU2 ni resuelve D0-04. Sin excepciones administrativas nuevas.
- Resolver la selección IDE antes de usar coordenadas. Autocompletado limitado
  a calles, portales y esquinas; las coordenadas manuales previas se conservan.
  Una calle sin número se identifica como ubicación aproximada, no portal exacto.
- Vista previa, no trazado persistido ni nueva condición para crear guía. Se
  mantienen texto/coordenadas de CU2 y todas sus validaciones. No se modifica
  histórico de tracking, descargos ni cálculo de desvíos P5.
- Fallos del proveedor se muestran separados del formulario; no hay éxito ficticio,
  ruta recta de reemplazo ni certificación por ausencia de datos. Cambiar texto,
  puntos, vehículo o carga invalida inmediatamente el resultado anterior.
- Timeouts, límites, sin redirecciones ni reintentos automáticos; circuitos
  separados para IDE/ORS. URL actual HeiGIT, configurable por entorno del servidor.
  Nunca exponer token, URL autenticada ni cuerpo crudo de error del proveedor.

## Alternativas y consecuencias

- IDE + Overpass no calcula itinerarios. Overpass queda fuera de esta entrega.
- Guardar trazado y usarlo en fiscalización requeriría contrato/decisión propios;
  no es necesario para la guía visual solicitada.
- Instalar otra biblioteca de mapas añade dependencias innecesarias. Se conserva
  Leaflet fijado por CDN como el mapa actual; su disponibilidad sigue siendo externa.
- La cobertura de restricciones OSM/ORS en Uruguay no está acreditada. Mostrar
  siempre alcance orientativo y atribuciones, sin prometer transitabilidad legal.
- P2/P3/P5 conservan sus responsabilidades; solicitud del usuario no acredita
  aceptación externa ni acepta globalmente ADR-0004/0022.

## Validación

Pruebas Java escritas primero, sin builds. HTTP/JSON-B/roles reales, frontend,
cuotas/clave y recorridos locales pendientes en la
[matriz](../../pruebas/planificacion-guia-ide-ors.md).
[Seguimiento](../../odd/tasks/planificacion-guia-ide-ors.md).
