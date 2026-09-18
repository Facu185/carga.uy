# ADR-0001 — El esquema lo gobierna Flyway, no Hibernate

**Estado:** Aceptada
**Fecha:** 2026-09-18

## Contexto

Somos 5 personas agregando tablas y columnas en paralelo. Hibernate puede generar el esquema solo
(`hbm2ddl.auto=update`), que es cómodo al principio.

## Decisión

`hbm2ddl.auto=none`. El esquema se define en `carga-ejb/src/main/resources/db/migration/` y lo
aplica Flyway al arrancar (`Migraciones.java`).

## Alternativas consideradas

- **`hbm2ddl.auto=update`:** la base de cada uno termina distinta según qué ramas haya corrido, los
  cambios no quedan versionados, no sabe borrar ni renombrar, y el día de la entrega no hay forma de
  reconstruir la base desde cero de manera confiable.
- **Script SQL único versionado a mano:** funciona, pero nadie sabe si su base ya está al día.

## Consecuencias

- Cambiar el esquema cuesta un archivo más (`V<n>__descripcion.sql`).
- **No se puede editar una migración ya aplicada**: Flyway guarda un hash y falla. Se corrige con
  una migración nueva.
- A cambio: la base se reconstruye igual en la máquina de cualquiera, en CI y en la nube, y el
  historial de cambios del esquema queda en el repositorio.
