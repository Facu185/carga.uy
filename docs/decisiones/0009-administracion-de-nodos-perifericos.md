# ADR-0009 — Administración de nodos periféricos: la base manda

**Estado:** Aceptada
**Fecha:** 2026-10-14

## Contexto

La tabla `nodo_periferico` existía desde la V1, pero nadie la usaba: la dirección del nodo Balanzas
salía de la variable `NODO_BALANZAS_URL`, y cambiarla exigía reiniciar el central. La letra prevé que
el Administrador gestione los nodos periféricos (caso de uso "Gestionar nodos periféricos").

## Decisión

1. **La base es la fuente de verdad.** El central consulta la URL del nodo Balanzas **activo**. Un
   cambio de URL rige en la consulta siguiente y desactivar el nodo corta las consultas en el acto,
   sin reiniciar nada. `NODO_BALANZAS_URL` queda solo como respaldo si no hay ningún nodo Balanzas
   registrado.
2. **A lo sumo un nodo activo por tipo, garantizado por la base** (índice único parcial, V9). El
   central consulta a un solo nodo Balanzas. Para pasar a otro: se desactiva el actual (con
   fundamento) y se activa el nuevo. Un nodo nuevo nace desactivado.
3. **Desactivar exige fundamento** y queda en la auditoría: deja al central sin pesajes, y eso afecta
   la detección de exceso de carga.
4. **Se administran nodos Balanzas.** El nodo Tracking se muestra con su estado, pero se habilita con
   su token (ADR-0007): un botón de activar o desactivar no cambiaría lo que acepta la ingesta.
5. **El estado no se guarda: se verifica.** La pantalla consulta al nodo en el momento, igual que el
   panel de operación. Un estado guardado puede decir "en línea" de un nodo que se cayó hace un rato.
6. **La pantalla va en el backoffice de React**, que tiene inicio de sesión y roles, y no en el panel
   JSF, que no los tiene.

## Alternativas consideradas

- **Seguir con la variable de entorno:** cada cambio de nodo exige tocar la configuración del
  despliegue y reiniciar, y no queda rastro de quién lo hizo ni por qué.
- **Varios nodos Balanzas activos a la vez**, consultando a todos o repartiendo la carga: la letra
  describe un solo nodo de balanzas, y repartir consultas mezclaría el estado de varios nodos en un
  solo circuito.
- **Que al activar un nodo se desactive solo el anterior:** es más cómodo, pero desactivar exige un
  fundamento, y un cambio implícito lo saltearía.
- **Guardar estado y último contacto en la tabla:** escrituras constantes para un dato que igual
  conviene verificar en vivo.

## Consecuencias

- El administrador cambia de nodo Balanzas desde el backoffice, y cada cambio queda auditado.
- Si el circuito estaba abierto por la URL vieja, la nueva se prueba recién cuando el circuito pasa a
  semiabierto (como mucho `BALANZAS_CIRCUITO_ESPERA_SEG`, 30 s por defecto).
- **Pendiente:** tokens por nodo guardados en la tabla, para distinguir varios nodos Tracking (ver
  ADR-0007).
