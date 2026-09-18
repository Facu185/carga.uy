# Cómo trabajamos en este repo

Somos 5 personas tocando el mismo código. Estas reglas existen para que nadie pise a nadie.

## 1. Ramas

- `main` siempre tiene que compilar y levantar. **Nadie commitea directo a `main`.**
- Una rama por tarea: `frente/tema`, por ejemplo `p3/registrar-evento-viaje`, `p5/desvio-ruta`.
- Antes de empezar el día: `git pull --rebase origin main`.

## 2. Commits

En español, en imperativo, explicando el *qué* (el *por qué* va en el cuerpo si hace falta):

```
Agregar validación de habilitación vigente al crear una guía
```

Un commit = un cambio con sentido. No mezclar refactor con funcionalidad nueva.

## 3. Pull Requests

- PR chico y frecuente le gana a un PR gigante al final.
- Lo revisa **alguien de otro frente** — así al menos dos personas entienden cada parte.
- Antes de pedir revisión: `mvn clean package` en verde y `docker compose up --build` levanta.

## 4. Qué NO se cambia sin avisar en el grupo

Lo que está en [docs/contratos.md](docs/contratos.md): interfaces de `carga-api`, DTO, esquema de
tablas compartidas, rutas REST ya publicadas. Si tenés que cambiarlo, avisá **antes** de mergear.

## 5. Base de datos

Cambios de esquema = migración nueva (`V<n>__descripcion.sql`). Nunca editar una ya aplicada, nunca
usar `hbm2ddl.auto=update`. Ver la sección 6 del [README](README.md).

## 6. Estilo

- Código, comentarios, nombres de clases y variables **en español** (salvo términos técnicos).
- Sin tildes ni ñ en el código fuente Java (sí en la documentación y en los textos de la interfaz):
  evita problemas de encoding entre Windows, Linux y el contenedor.
- 4 espacios, sin tabs, UTF-8, fin de línea LF. Está en `.editorconfig`, configurá el IDE para que
  lo respete.
- Comentá el *por qué*, no el *qué*. Si el comentario repite lo que dice el código, sobra.

## 7. Pruebas

La letra pide ~80% de cobertura sobre la lógica de negocio (Sección 4.5). La regla práctica: **toda
regla de negocio que escribas tiene que tener al menos una prueba que la verifique cuando se cumple
y otra cuando no.** Ejemplo: `carga-ejb/src/test/java/.../VehiculoTest.java`.

## 8. Antes de cada entrega

1. `mvn clean package` en verde.
2. `docker compose down -v && docker compose up -d --build` levanta desde cero.
3. El README refleja lo que hay.
4. El SAD actualizado con los cambios del período.
