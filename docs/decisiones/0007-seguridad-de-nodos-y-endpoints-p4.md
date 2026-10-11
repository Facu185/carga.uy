# ADR-0007 — Autenticación de los nodos periféricos y roles en los endpoints de P4

**Estado:** Aceptada
**Fecha:** 2026-10-11

## Contexto

Cualquiera que llegara al central podía mandar posiciones a `/carga/api/tracking/posiciones`, y
cualquiera que llegara al nodo Balanzas podía consultar las pesadas de cualquier vehículo. Los
endpoints de P4 (`/monitor`, `/pesajes`) tampoco pedían sesión.

La convención del grupo es autorizar con `@RolesAllowed` en los EJB. Pero los servicios de P4
(`MonitorBean`, `SincronizacionPesajesBean`) también los usan el panel JSF, que no tiene inicio de
sesión, y la agenda automática, que corre sin usuario.

## Decisión

1. **Los nodos se autentican con un token compartido** en el header `X-Nodo-Token`, en los dos
   sentidos: Tracking ante el central y el central ante Balanzas. No en `Authorization`, porque ese
   header lo procesa `MecanismoJwt` en todos los pedidos y rechazaría un token que no es un JWT. La
   comparación es en tiempo constante y sin token configurado no se acepta a nadie.
2. **Los recursos REST de P4 son EJB `@Stateless` con `@RolesAllowed(ADMINISTRADOR, FUNCIONARIO)`.**
   El control lo hace el contenedor, como en el resto del central, y el rechazo pasa por el mismo
   `AccesoDenegadoMapper` (401/403). Los servicios internos quedan sin rol porque los usan el panel y
   la agenda.
3. **El mapa del panel recibe sus datos por JSF**, en el mismo auto-refresco, en vez de pedirlos a
   `/carga/api/monitor/mapa`: así ese endpoint puede exigir rol sin dejar el mapa sin datos.

## Alternativas consideradas

- **JWT de servicio para los nodos:** necesita emitir y rotar tokens para máquinas. Más trabajo
  sin ganancia real con un solo nodo de cada tipo.
- **TLS mutuo (certificados de cliente):** es lo que correspondería en producción, pero exige montar
  una infraestructura de certificados que excede el alcance del laboratorio.
- **`@RolesAllowed` en `MonitorBean` y `SincronizacionPesajesBean`:** rompe el panel y la agenda.
  `@RunAs` resolvería la agenda, pero no el panel.
- **Dejar `/monitor/mapa` público para el panel:** expone las posiciones de la flota, que es lo más
  sensible que maneja P4.

## Consecuencias

- La ingesta y el nodo Balanzas solo aceptan a quien tiene el token. El script de punta a punta lo
  verifica en el CI (401 sin token o con uno inventado, 202 con el correcto).
- Un token compartido identifica al **tipo** de nodo, no a cada nodo: con varios nodos de Tracking,
  todos usan el mismo. Para distinguirlos hace falta un token por nodo en `nodo_periferico`, que
  queda para la administración de nodos.
- Rotar un token es cambiar la variable en los dos lados y reiniciar.
- **Limitación conocida:** el panel JSF sigue sin inicio de sesión. Se resuelve al pasarlo al
  backoffice autenticado (ADR-0004).
