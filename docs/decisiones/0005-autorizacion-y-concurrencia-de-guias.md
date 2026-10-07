# ADR-0005 — Autorización y concurrencia al crear guías

**Estado:** Aceptada  
**Fecha:** 2026-10-06

## Contexto

P2 permite a un responsable crear guías para una empresa y a un chofer consultar únicamente sus propias guías. P1 ya autentica el principal y persiste la relación opcional `usuario.empresa_id`; no define un nuevo rol de responsable de empresa.

La creación puede competir entre pedidos simultáneos para el mismo vehículo o chofer. Una simple consulta de conflictos seguida por un `INSERT` no es atómica.

## Decisión

La autorización del responsable se resuelve exclusivamente como:

`principal autenticado -> usuario.cedula -> usuario.empresa_id`.

Por tanto, para P2 `usuario.empresa_id` **es la autorización operativa** para crear guías de esa empresa. Un principal sin esa asociación se rechaza. No se agregan ni reinterpretan roles de P1: el endpoint conserva el rol de ciudadano ya definido y la pertenencia a la empresa limita los recursos que puede usar.

El chofer se resuelve por su cédula desde el principal autenticado; el cliente no envía un identificador para consultar `/guias/mis-guias`.

La creación bloquea pesimistamente vehículo y chofer antes de consultar solapamientos, dentro de la transacción EJB. Los períodos son semiabiertos `[inicio, fin)` y la vigencia del extremo final se evalúa en `fin.minusNanos(1)`, que respeta la precisión de `Instant`.

## Alternativas consideradas

- Crear un rol `RESPONSABLE_EMPRESA`: descartado porque cambia el alcance y el modelo de roles entregado por P1.
- Confiar sólo en una consulta de conflictos: descartado por la condición de carrera entre consulta e inserción.
- Probar concurrencia EJB real con el Testcontainers actual: no es posible sin desplegar WildFly/EJB; la infraestructura actual sólo levanta PostgreSQL para migraciones. Se prueban unitariamente la estrategia de bloqueo/consulta y se deja la concurrencia del contenedor para una prueba de integración futura con servidor Java EE.

## Consecuencias

La asociación en `usuario.empresa_id` debe administrarse de forma controlada: otorgarla concede capacidad para crear guías para esa empresa. La garantía de concurrencia depende de invocar el bean mediante EJB, para que exista transacción y `PESSIMISTIC_WRITE`; una invocación directa en una prueba unitaria no reproduce los locks de base.
