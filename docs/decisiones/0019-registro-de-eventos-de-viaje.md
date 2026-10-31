# ADR-0019 — Registro de eventos de viaje: procesamiento sincrónico, idempotencia y conflictos

**Estado:** Propuesta. Falta acordar con P2 la política de conflictos (punto 3) y la coordinación
entre el inicio del viaje y la baja de un vehículo (ver `docs/pendientes-funcionales-laboratorio-2026.md`, sección 8).
**Fecha:** 2026-10-21

## Contexto

El caso de uso crítico 3, "Registrar evento de viaje" (frente P3), lo usa el chofer desde el
componente móvil, que tiene que funcionar con o sin conectividad (Sección 3.3 de la letra). Eso
obliga a que el teléfono guarde los eventos y los reenvíe cuando vuelve la señal, y a que el central
tolere reenvíos. El caso de uso deja como decisión del grupo la política ante conflictos: qué pasa
si el viaje cambió en el central mientras el teléfono estaba sin señal.

El registro del fin de un viaje dispara la obtención de pesajes, que ya resolvió P4 con
`AvisoFinDeViajeLocal` (ADR-0010).

## Decisión

1. **Procesamiento sincrónico.** `POST /carga/api/eventos-viaje` valida, guarda y responde en el
   acto: `201` evento nuevo, `200` reenvío, `400` datos inválidos, `403` guía ajena o inexistente,
   `409` el estado del viaje no lo admite. Lo asincrónico (los pesajes) ya lo cubre la cola de P4.
2. **Idempotencia por `idEvento`**, generado en el teléfono (UUID). Un reenvío devuelve el evento
   ya registrado, sin duplicarlo, sin volver a cambiar el estado de la guía y sin volver a avisar el
   fin de viaje. Un `idEvento` reutilizado para un evento distinto es un `409`.
3. **El central decide.** El estado de la guía es la fuente de verdad:
   `ASIGNADA --INICIO--> EN_CURSO --(CARGA_PARCIAL | DESCARGA_PARCIAL | INCIDENTE)--> EN_CURSO --FIN--> FINALIZADA`.
   Un evento que el estado no admite se rechaza con `409` y el motivo. El teléfono lo marca como
   rechazado, se lo muestra al chofer y no lo reintenta.
4. **Concurrencia:** se bloquea la fila de la guía (`PESSIMISTIC_WRITE`) mientras se procesa un
   evento, así los eventos de un mismo viaje se procesan de a uno. El índice único de `id_evento`
   queda como red de seguridad.
5. **Ubicación y foto opcionales.** Latitud y longitud van juntas y se guarda la geometría generada
   (migración `V10`, misma convención que `posicion_tracking`). La foto viaja en base64 dentro del
   JSON, tiene que ser JPEG y tiene un tope de tamaño (`EVENTOS_FOTO_MAX_BYTES`, 2 MB por defecto).
   El historial no devuelve las fotos, solo indica si hay.

## Alternativas consideradas

- **Encolar con JMS y responder `202`**, como la ingesta de Tracking: absorbe picos, pero el teléfono
  no se enteraría de los rechazos (estado inválido, guía ajena) sin otra consulta, y la política de
  conflictos se volvería invisible para el chofer. El volumen de eventos de choferes es mucho menor
  que el de posiciones.
- **Aceptar cualquier evento como registro**, sin validar el estado: más permisivo, pero el estado
  del viaje podría contradecir a sus eventos, y las reglas de fiscalización usan ese estado.
- **`INSERT ... ON CONFLICT`** como en `PosicionDAO`: con el bloqueo de la guía no hace falta, y con
  PostgreSQL complica pasar `null` en la foto desde una consulta nativa.
- **Foto como `multipart/form-data`** o subida aparte: evita el ~33 % extra del base64, pero obliga a
  un segundo pedido y complica la cola del teléfono, que tendría que coordinar dos envíos por evento.

## Consecuencias

- El CU4 se dispara de punta a punta: el `FIN` llama a `AvisoFinDeViajeLocal` en la misma transacción.
- Se agregan `ConflictoException` (`409`) y `RecursoAjenoException` (`403`) a `carga-api`, que otros
  frentes pueden reutilizar.
- La guía cambia de estado desde P3 (`Guia.aplicarEvento`). La tabla `guia` es compartida: si P2
  agrega modificación o cancelación de guías, hay que revisar estas transiciones.
- Pendiente: endpoint para descargar la foto de un evento (lo necesitarán la web o P5 como evidencia).
