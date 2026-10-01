# ADR-0004 — Interfaces web en React consumiendo la API REST del central

**Estado:** Propuesta
**Fecha:** 2026-09-30

## Contexto

La letra pide un frontoffice web para ciudadanos y funcionarios y un backoffice web para
autoridades y administradores, ambos en el componente central. No fija la tecnología de la interfaz.

La base del grupo prevé JSF en `carga-web` (`index.xhtml`, `empresas.xhtml`, `EmpresaVista`). En el
grupo se planteó usar React, y el frente P1 ya tiene un prototipo funcionando con React + REST: ingreso
con gub.uy, login del backoffice, gestión de usuarios y funcionarios, y consulta de auditoría.

Esta decisión afecta sobre todo a P2 (backoffice) y P3 (portal del ciudadano), que son los dueños de
esas pantallas.

## Decisión

- Las interfaces web se hacen como una SPA en React + TypeScript + Vite, en un módulo `carga-front`
  **fuera del EAR**.
- En `docker compose` corre un contenedor nginx propio que sirve la SPA y reenvía `/carga/api/` al
  central. Navegador y API quedan en el mismo origen, así que no hace falta CORS.
- La SPA solo consume `/carga/api/<tema>`, con las mismas reglas de `docs/contratos.md` que cualquier
  otro consumidor.
- La SPA decide qué pantallas mostrar, pero **no controla el acceso**. La autorización real la hace
  el central con `@RolesAllowed` en los EJB.
- Las páginas JSF actuales de `carga-web` quedan hasta que exista su equivalente en React. Después,
  P2 decide si se borran.

## Alternativas consideradas

- **Todo en JSF + PrimeFaces:** es lo que se ve en el curso y se integra directo con CDI y los EJB.
  Se descarta porque obliga a reescribir lo que P1 ya tiene andando, y porque la API REST igual
  tiene que existir para los nodos y para la aplicación del chofer.
- **Mixto (backoffice JSF, frontoffice React):** dos tecnologías de interfaz, dos maneras de
  manejar la sesión y el login duplicado. Mantiene el costo de las dos opciones.
- **React empaquetado dentro del WAR de `carga-web`:** un solo artefacto para desplegar, pero ata el
  build de Node a Maven y vuelve más lento `mvn clean package`. Queda como opción si el despliegue
  en la nube exige un único artefacto.

## Consecuencias

- La interfaz queda desacoplada del central. La misma API sirve para la web, los nodos y una futura
  aplicación móvil.
- Se reutiliza el prototipo de P1 en vez de rehacerlo.
- Se agrega otra cadena de herramientas (Node, npm) al CI y a `docker compose`.
- Se pierde lo que JSF da integrado, como la validación y el estado en el servidor. Las validaciones
  se repiten en el cliente, pero la que vale es la del servidor.
- El token de sesión vive en el navegador, así que hay que cuidar XSS y la expiración del token.
- **Pendiente:**
  - que P2 y P3 acepten esta decisión;
  - confirmar con el docente que no se exige JSF;
  - agregar un job de frontend al CI;
  - publicar en `docs/contratos.md` los endpoints nuevos que consuma la SPA.
