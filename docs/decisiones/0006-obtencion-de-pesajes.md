# ADR-0006 — Obtención de pesajes: circuito compartido, deduplicación en la base y aviso por cola

**Estado:** Aceptada
**Fecha:** 2026-10-07

## Contexto

El caso de uso crítico "Obtener pesajes del nodo Balanzas" (frente P4) consulta a un nodo externo
que puede estar caído o lento (Sección 4.3 de la letra), guarda las pesadas y tiene que hacérselas
llegar a la detección de incumplimientos (frente P5), que todavía no existe. Lo disparan una agenda
periódica y, a mano, el panel de operación, así que dos sincronizaciones pueden correr a la vez.

## Decisión

1. **Reutilizar el `CircuitBreaker` de `ejb/integracion/`** que hizo P1 para la PDI, con el mismo
   esquema que `PdiBean`: singleton, timeout, reintentos solo ante fallas técnicas y métricas.
   Un 4xx del nodo no se reintenta ni abre el circuito: el nodo está vivo, el pedido es el que está mal.
2. **Deduplicar en la base** con `INSERT ... ON CONFLICT (id_pesada) DO NOTHING`, no con "consultar y
   después insertar".
3. **Avisar a P5 por una cola JMS** (`java:/jms/queue/pesadas`), dentro de la misma transacción que
   guarda la pesada, y solo por las pesadas nuevas.
4. **Vincular con la guía leyendo la tabla `guia`** desde `PesadaDAO`, en lugar de pedirle un método
   a `GuiaServicio`.

## Alternativas consideradas

- **Un circuit breaker propio de P4:** dos implementaciones del mismo patrón en el mismo sistema, con
  comportamientos que pueden divergir. Descartado.
- **Consultar y después insertar:** la agenda y un pedido manual pasan los dos la consulta y el
  segundo falla al insertar por la restricción única, deshaciendo toda su sincronización.
- **Llamar directo a un servicio de P5:** ata este caso de uso a que P5 esté terminado y funcionando
  en ese momento. Con la cola, los avisos esperan hasta que haya consumidor.
- **Un método en `GuiaServicio`:** todos sus métodos exigen un usuario autenticado
  (`@RolesAllowed`), y la agenda corre sin usuario.

## Consecuencias

- Con el nodo caído el central sigue `UP`, el chequeo de salud marca Balanzas como `DEGRADADO`, y tras
  varias fallas seguidas el central deja de esperar timeouts. Verificado con `docker compose stop balanzas`.
- Si la publicación en la cola falla, la transacción se deshace y la pesada se vuelve a traer en la
  próxima sincronización: no puede quedar una pesada guardada sin su aviso.
- Hasta que P5 tenga consumidor, los mensajes se acumulan en la cola `cola_pesadas`.
- La vinculación depende de cómo P2 guarda el período de la guía (ver "Pesajes P4" en
  `docs/contratos.md`).
- **Pendiente:** el disparo "al registrar el fin del viaje" depende de P3, y los endpoints de P4 todavía
  no exigen rol (igual que el resto de lo de P4; se decide junto con la tecnología del panel, ADR-0004).
