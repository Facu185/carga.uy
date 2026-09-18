# ADR-0002 — El nodo Tracking publica por REST y el central encola

**Estado:** Aceptada
**Fecha:** 2026-09-18

## Contexto

El nodo Tracking emite posiciones de la flota de forma continua. La letra pide que el procesamiento
de eventos de los nodos periféricos sea asincrónico y tolerante a picos (Secciones 3.7.3 y 4.1).

## Decisión

El nodo hace `POST /carga/api/tracking/posiciones` y el central responde `202 Accepted` apenas
encola el mensaje en `java:/jms/queue/posiciones`. Un MDB lo procesa después.

## Alternativas consideradas

- **El nodo publica directo en la cola JMS:** menos saltos, pero obliga al nodo a hablar el
  protocolo del broker y a tener las librerías del servidor de aplicaciones. El nodo representa un
  dispositivo externo; queremos que dependa solo de HTTP.
- **REST sincrónico sin cola:** un pico de eventos degrada la recepción y la pérdida de mensajes
  pasa a ser problema del nodo.

## Consecuencias

- El nodo no sabe nada de JMS: se puede escribir en cualquier lenguaje. Nuestro `nodo-tracking` no
  tiene ni una dependencia externa, solo el JDK.
- Hay un salto extra (HTTP → cola), aceptable frente al desacople que da.
- Como el nodo puede reintentar, el mismo evento puede llegar dos veces: por eso todo mensaje lleva
  `idEvento` y el MDB descarta duplicados.
