---
name: jms-idempotencia-resiliencia
description: >-
  Trabaja los flujos asíncronos y fallas de integración de carga.uy. Usar al modificar
  ingesta Tracking, MDB/JMS, pesajes, fin de viaje, deduplicación, saturación,
  reintentos o CircuitBreaker; no autoriza rediseñar el broker ni hacer auditorías.
---

# Mensajería e integraciones de carga.uy

## Antes de intervenir

Lee [AGENTS.md](../../../AGENTS.md), [CODEX](../../../CODEX/README.md) y el
[contrato](../../../docs/contratos.md). Delimita productor, frontera HTTP,
transacción, cola, consumidor, persistencia y efecto posterior. Diferencia lo
descrito por el ADR de lo implementado y de lo probado en ejecución.

## Diseñar el cambio

1. Identifica la cola y el DTO reales. Conserva `TextMessage` con JSON, fechas
   ISO-8601 y correlación; no sustituyas por `ObjectMessage`. El nodo Tracking
   publica HTTP al central, no directamente al broker.
2. Define el punto en que se confirma aceptación. Tracking responde `202` después
   de encolar transaccionalmente, no después de procesar ni antes de aceptar el
   mensaje. Verifica la frontera EJB/JTA y el manejo de fallos, no solo el código HTTP.
3. Especifica la clave estable de deduplicación del flujo: `idEvento`, `idPesada`
   u otra acordada. No inventes un identificador universal ni lo regeneres al
   reintentar. Combina deduplicación atómica en base y efectos en su transacción;
   consultar y luego insertar no basta frente a dos consumidores.
4. Para una pesada nueva, conserva la coherencia entre persistencia y aviso JMS;
   una ya existente no genera otro aviso. El consumidor sigue tolerando reentregas.
   Para fin de viaje, conserva el aviso dentro de la transacción llamadora y la
   consulta remota posterior en el MDB. No acredites integración P3/P5 solo porque
   exista el adaptador P4.
5. Conserva límites de lote, contrapresión, `503`/`Retry-After` y política sin
   descarte según ADR-0008. Lee umbrales/configuración vigentes; no los dupliques
   como constantes nuevas ni confundas objetivos de volumen con capacidad medida.
6. En HTTP/SOAP saliente, identifica timeout, condiciones reintentables, presupuesto
   de reintentos y circuito compartido. No reintentes efectos no idempotentes ni
   multipliques reintentos entre capas. Mantén el resultado degradado del contrato:
   un nodo caído no equivale necesariamente a una lista vacía o a una caída central.
7. Sigue errores hasta rollback, reentrega y DLQ. No captures un fallo para aparentar
   éxito ni agregues reenvíos infinitos. No cambies política de mensajes irrecuperables
   sin acuerdo. Conserva la causa y correlación sin registrar payloads sensibles.

## Fuentes por flujo

| Cambio | Fuentes a consultar |
| --- | --- |
| Tracking REST → JMS | [ADR-0002](../../../docs/decisiones/0002-ingesta-de-tracking-por-rest-mas-jms.md), [ADR-0008](../../../docs/decisiones/0008-volumen-y-saturacion-de-tracking.md) |
| Pesajes, circuito y aviso | [ADR-0006](../../../docs/decisiones/0006-obtencion-de-pesajes.md), [ADR-0009](../../../docs/decisiones/0009-administracion-de-nodos-perifericos.md) |
| Fin de viaje y recorrido | [ADR-0010](../../../docs/decisiones/0010-integraciones-de-p4-con-p3-y-p5.md) |
| Broker/JTA/reentregas | [configure.cli](../../../docker/central/configure.cli) |
| Código y pruebas | Recorridos del [mapa](../../../CODEX/mapa-arquitectura.md) y tests de `IngestaTrackingBean`, `SincronizacionPesajesBean`, `CircuitBreaker` |

## Evidencia mínima

Escribe primero pruebas del efecto único, duplicado, fallo entre efectos y nodo
caído que correspondan al cambio. Distingue doble de JMS de integración real
base/broker. Desde la raíz, `git diff --check`; no ejecutes builds, runners que
compilen ni despliegues. Declara rojo/verde pendiente y garantías JTA/reentrega
sin verificar; no prometas entrega exactamente una vez por deduplicar una tabla.
