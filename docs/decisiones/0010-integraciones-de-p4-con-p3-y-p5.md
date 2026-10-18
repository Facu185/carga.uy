# ADR-0010 — Integraciones de P4 con P3 (fin de viaje) y con P5 (recorrido)

**Estado:** Aceptada
**Fecha:** 2026-10-18

## Contexto

El caso de uso crítico "Obtener pesajes del nodo Balanzas" se dispara **al finalizar un viaje** y
periódicamente (SAD, sección 3.6). El disparo periódico ya existía (`AgendaPesajesBean`). El de fin de
viaje depende de P3, que registra el evento, y P3 todavía no tiene código.

La regla de desvío de ruta de P5 necesita el recorrido de cada vehículo durante su viaje, a partir de
las posiciones que guarda la ingesta de Tracking (P4).

## Decisión

1. **Fin de viaje por cola.** P3 llama a `AvisoFinDeViajeLocal.avisar(...)`, que encola el aviso dentro
   de su transacción. `FinDeViajeMDB` lo consume y sincroniza los pesajes de cada día del viaje. El
   contrato queda definido ahora, así que P3 solo tiene que hacer una llamada.
2. **Recorrido como servicio local y API REST.** `RecorridoLocal` no exige rol, porque P5 lo llama desde
   procesos sin usuario (un MDB o una agenda). `/carga/api/recorridos` sí exige rol, igual que el resto
   de la API de P4 (ADR-0007).
3. **La distancia la calcula PostGIS** (`ST_MakeLine` + `ST_Length` sobre `geography`), en metros
   sobre la superficie terrestre.
4. **Topes:** 7 días por consulta por fechas y 10.000 puntos por respuesta, con aviso de truncado.

## Alternativas consideradas

- **Que P3 llame directo a la sincronización:** el registro del fin del viaje esperaría la consulta a
  Balanzas (hasta unos segundos con reintentos), y fallaría si el nodo está caído. Con la cola, P3
  termina en el acto.
- **Evento CDI en vez de cola:** es más simple, pero corre en el mismo hilo y en la misma transacción
  que P3, con el mismo problema que la llamada directa.
- **Que P5 consulte la tabla de posiciones por su cuenta:** duplicaría la consulta y los topes, y
  ataría a P5 al esquema de una tabla de P4.
- **Devolver el recorrido simplificado** (`ST_Simplify`) en vez de truncarlo: para dibujarlo serviría,
  pero para la regla de desvío se perderían justo los puntos que pueden mostrar el desvío.

## Consecuencias

- Cuando P3 registre el fin de un viaje, los pesajes de ese vehículo se obtienen sin esperar a la
  agenda.
- P5 tiene la evidencia de recorrido lista para usar.
- El disparo de punta a punta se prueba cuando P3 haga la llamada; hasta entonces está cubierto con
  pruebas unitarias y con el consumidor desplegado escuchando la cola.
