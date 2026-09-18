# ADR-0003 — Opcionales elegidos: PostGIS y LDAP

**Estado:** Aceptada
**Fecha:** 2026-09-18

## Contexto

La letra ofrece requerimientos opcionales que suman puntos. Somos de la carrera **Tecnólogo**, así
que necesitamos 2 puntos, no todos los disponibles.

## Decisión

PostGIS (georreferenciación) y LDAP (usuarios internos).

## Alternativas consideradas

Los otros opcionales resuelven problemas que el sistema no tiene todavía, o exigen infraestructura
que habría que aprender y desplegar aparte, sin apoyarse en nada de lo ya hecho.

## Consecuencias

- **PostGIS** no agrega trabajo extra real: el sistema necesita igual calcular la distancia al
  recorrido esperado para la regla de desvío de ruta. Ya está la extensión habilitada, las columnas
  `geometry` generadas y los índices GiST en la migración V1.
- **LDAP** se acopla al frente P1, que ya está construyendo autenticación y roles; no toca al resto
  del equipo.
- Ambos quedan acotados a un frente cada uno, así un opcional atrasado no bloquea a los demás.
