# ADR-0019 — Registro de eventos de viaje: procesamiento sincrónico, idempotencia y conflictos

**Estado:** Aceptada por el usuario para la política central de conflictos e inicio/baja,
el 2026-10-31. Implementación local y coordinación/aceptación compartida con Federico
pendientes de acreditar; no se presume aprobación individual de P3 ni entrega del móvil.
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
   Se comparan guía, tipo, instante, detalle normalizado, coordenadas y bytes de foto;
   no `recibidoEn` ni `tieneFoto`, que son metadatos de respuesta. El instante de
   registros nuevos se normaliza a microsegundos (precisión de PostgreSQL);
   offsets equivalentes y espacios externos del detalle no crean otro contenido.
3. **El central decide.** El estado de la guía es la fuente de verdad:
   `ASIGNADA --INICIO--> EN_CURSO --(CARGA_PARCIAL | DESCARGA_PARCIAL | INCIDENTE)--> EN_CURSO --FIN--> FINALIZADA`.
   Un evento que el estado no admite se rechaza con `409` y el motivo. El teléfono lo marca como
   rechazado, conserva el contenido y se lo muestra al chofer sin reintento automático
   infinito. La cola móvil sincroniza por guía en orden; los errores temporales de
   transporte no se confunden con un conflicto definitivo. Esta conducta del móvil
   es una entrega P3 pendiente: su código no está en este repositorio.
   Un INICIO nuevo se rechaza con `409` si el vehículo está actualmente dado de baja,
   aunque el evento offline indique una hora anterior a la baja. No se reconstruye
   habilitación histórica ni se autoriza una reactivación retroactiva desde el móvil.
   Un reenvío idéntico ya aceptado no se vuelve a validar como inicio nuevo.
4. **Concurrencia:** para INICIO se bloquea explícitamente **Vehículo → Guía**
   con `PESSIMISTIC_WRITE`, releyendo estado y asignación tras esperar. Si cambió
   la asignación se rechaza el intento, sin bloquear el vehículo nuevo fuera de orden.
   DELETE y PUT de baja toman el mismo vehículo antes de comprobar `EN_CURSO`.
   Si el inicio confirma primero, se rechaza la baja; si la baja confirma primero,
   se rechaza el nuevo inicio. Los demás eventos conservan la exclusión por guía;
   una baja no debe impedir reenviar un evento ya aceptado ni cerrar un viaje en curso.
   Esta entrega revalida la baja administrativa actual; no cambia las reglas de
   permiso/ITV de creación ni acuerda una nueva política temporal para esas vigencias.
   La inserción, transición y aviso FIN pertenecen a la misma transacción JTA.
   La unicidad global de `id_evento` se resuelve con inserción atómica y lectura del
   ganador: no basta bloquear una guía si dos guías distintas usan el mismo ID.
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
- **Solo consulta previa + `persist`:** simplifica el DAO, pero el bloqueo por guía no
  evita una colisión del ID global entre guías distintas. Se elige `INSERT ... ON CONFLICT`
  como en P4, con casts explícitos para foto/detalle/coordenadas nulos. No se cambia esquema.
- **Reconstruir validez histórica del inicio offline:** evita rechazar un inicio ocurrido
  antes de una baja posterior, pero exige historial/versionado de habilitaciones y una
  política temporal adicional. No se elige para esta entrega.
- **Foto como `multipart/form-data`** o subida aparte: evita el ~33 % extra del base64, pero obliga a
  un segundo pedido y complica la cola del teléfono, que tendría que coordinar dos envíos por evento.

## Consecuencias

- El `FIN` llama a `AvisoFinDeViajeLocal` en la misma transacción; la cadena real
  base/broker/Balanzas y el rollback requieren validación en contenedor, no se dan por ejecutados.
- Se agregan `ConflictoException` (`409`) y `RecursoAjenoException` (`403`) a `carga-api`, que otros
  frentes pueden reutilizar.
- La guía cambia de estado desde P3 (`Guia.aplicarEvento`). La tabla `guia` es compartida: si P2
  agrega modificación o cancelación de guías, hay que revisar estas transiciones.
- Pendiente: endpoint para descargar la foto de un evento (lo necesitarán la web o P5 como evidencia).
- C14-E para que el funcionario lea el historial sigue pendiente; esta decisión no
  amplía roles del GET/POST ni cierra fase 5. Escenarios y límites de ejecución en
  [aceptación ADR-0019](../../pruebas/p2-p3-eventos-conflictos.md).
- Caracterizar antes del despliegue los reenvíos de eventos anteriores con fracción
  submicrosegundo: el redondeo previo de la base puede diferir de la normalización
  explícita nueva. No se sobrescriben originales ni se promete compatibilidad de
  ese borde sin comprobar datos/serialización reales.
