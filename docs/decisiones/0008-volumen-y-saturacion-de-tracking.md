# ADR-0008 — Volumen objetivo de Tracking y política de saturación

**Estado:** Aceptada
**Fecha:** 2026-10-14

## Contexto

La letra (Sección 3.7.3) pide estimar y justificar el volumen de eventos de posición que la plataforma
tiene que soportar, considerando picos de al menos tres veces ese volumen (por ejemplo, cuando un
grupo de vehículos recupera cobertura y descarga lo acumulado). Ese volumen es el que se usa en la
prueba de performance (Sección 4.3). También pide definir qué hace el sistema ante saturación
(encolado, descarte selectivo o reducción de frecuencia), justificar el broker elegido, la estrategia
de distribución de los mensajes y la política de entrega (Sección 4.1).

## Decisión

### 1. Volumen objetivo

| Factor | Valor | Justificación |
|---|---|---|
| Flota total | 30.000 camiones | Orden de magnitud declarado por el MTOP ([nota de 2018](https://mundomaritimo.cl/noticias/editor/32484)). |
| Circulando a la vez | 10 % → 3.000 vehículos | Supuesto: no toda la flota está en ruta al mismo tiempo (carga/descarga, descanso, unidades paradas). Es el factor más incierto. |
| Frecuencia de reporte | 1 posición cada 30 s | Valor habitual en rastreo de flotas; alcanza para detectar desvíos de ruta. |
| **Volumen base** | **100 eventos/s** | 3.000 / 30 s ≈ 8,6 millones de posiciones por día. |
| **Pico** | **300 eventos/s** | ×3, lo que exige la letra. |

La prueba de performance busca además el punto de quiebre, así que el margen real por encima de
300 ev/s queda medido.

### 2. Política de saturación: encolar y, si no alcanza, frenar al productor. Nunca descartar.

- **Picos cortos — encolar.** La cola `cola_posiciones` absorbe el pico; si se llena la memoria, Artemis
  pagina a disco (`address-full-policy=PAGE` en `configure.cli`). No se pierde nada; solo crece la
  latencia un rato.
- **Saturación sostenida — 503 con `Retry-After`.** Si las posiciones pendientes llegan al umbral alto
  (por defecto **18.000**, aproximadamente un minuto de pico), la ingesta responde `503` con
  `Retry-After` (por defecto **30 s**) y no encola nada. Se vuelve a aceptar recién cuando la cola baja
  al umbral bajo (por defecto **9.000**): con dos umbrales el sistema no oscila entre aceptar y
  rechazar. El dispositivo conserva las posiciones y reintenta; ya tiene que poder guardarlas para
  operar sin cobertura.
- El tamaño de la cola se lee por JMX como mucho una vez por segundo (`MonitorColaPosiciones`). Si no
  se puede leer, **no** se rechaza: es preferible aceptar a cortar la ingesta por no poder medir.
- Umbrales y espera se cambian sin tocar el código: `TRACKING_COLA_UMBRAL_ALTO`,
  `TRACKING_COLA_UMBRAL_BAJO`, `TRACKING_REINTENTAR_SEG`.

### 3. Envío por lote

`POST /carga/api/tracking/posiciones/lote` recibe hasta 500 posiciones (`TRACKING_LOTE_MAX`). Es el
caso del dispositivo que recupera cobertura: un pedido en vez de cientos. Las posiciones válidas se
encolan en una sola transacción (todas o ninguna); las inválidas se informan en la respuesta con su
índice y motivo, y no se reintentan.

### 4. Broker, distribución y entrega

- **Broker:** ActiveMQ Artemis, el embebido en WildFly. Ya está integrado y participa de las
  transacciones JTA, y no suma infraestructura al despliegue en Elastic Cloud.
- **Distribución:** consumidores en competencia (el pool de instancias de `PosicionMDB`) sobre una única
  cola, sin agrupar por matrícula. El orden de la ruta se reconstruye por `instante` y los duplicados
  los descarta la base (`ON CONFLICT`), así que no hace falta procesar cada vehículo en secuencia;
  agrupar limitaría el paralelismo.
- **Entrega:** al menos una vez, con consumidor idempotente, lo que da efecto de exactamente una vez.
  El `202` se devuelve cuando el mensaje persistente ya está confirmado en la cola (la ingesta corre
  en una transacción); si el MDB falla, el broker reentrega hasta 5 veces con espera creciente y
  después lo manda a la DLQ.

## Alternativas consideradas

- **Descarte selectivo** (por ejemplo, quedarse con una de cada N posiciones bajo carga): descartado.
  Las posiciones son evidencia de la regla de desvío de ruta; perder puntos puede ocultar un desvío
  real. En un sistema de fiscalización, demorar es mejor que perder.
- **Solo encolar, sin límite:** la cola crecería sin techo en una saturación sostenida, con la latencia
  de detección creciendo hasta volverse inútil y el disco del broker como único límite.
- **Kafka:** daría particiones y reprocesamiento del historial, pero exige un clúster aparte. Para
  300 ev/s no se justifica.
- **Agrupar mensajes por matrícula (`JMSXGroupID`):** garantiza que un mismo vehículo lo procese siempre
  el mismo consumidor y en orden. No se necesita hoy (ver distribución). Queda disponible si la
  detección de incumplimientos (P5) pasa a necesitar procesamiento secuencial por vehículo.

## Consecuencias

- El sistema no pierde posiciones aceptadas, ni en picos ni ante fallas de la base.
- En saturación sostenida el costo lo absorben los dispositivos, que reintentan más tarde. Nuestro
  nodo Tracking de demostración todavía no reintenta: lo hará el generador de carga.
- Métricas para observarlo: `carga_tracking_cola_pendientes`, `carga_tracking_saturada` y
  `carga_tracking_posiciones_total{resultado="saturada"}`.
- El 10 % de flota en ruta es un supuesto; si la prueba de performance o nueva información lo
  contradicen, se ajustan los números y los umbrales por variables de entorno.
- **Pendiente:** retención de `posicion_tracking` (al volumen base son millones de filas por día).
