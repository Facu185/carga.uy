---
name: react-typescript-carga
description: >-
  Implementa cambios en la SPA React/TypeScript existente de carga-front. Usar al
  modificar páginas, componentes, rutas, formularios, cliente REST o estados de sesión
  de carga.uy. No migra JSF, no aplica Next.js/Server Components ni construye sitios externos.
---

# SPA de carga.uy

## Antes de editar

Lee [AGENTS.md](../../../AGENTS.md), [CODEX](../../../CODEX/README.md),
[contexto](../../../CODEX/contexto-proyecto.md) y
[ADR-0004](../../../docs/decisiones/0004-frontend-react-sobre-api-rest.md).
La SPA existe, pero eso no convierte el ADR propuesto en aceptado ni elimina JSF.
Expón contradicciones de política que afecten el cambio y espera resolución;
no reformules los estándares Java por tu cuenta.

## Procedimiento

1. Lee [package.json](../../../carga-front/package.json), rutas y componentes vecinos.
   Usa la SPA Vite/React existente; no presupongas SSR, React Compiler, Next.js,
   Server Components o nuevas bibliotecas. No añadas dependencias sin necesidad/acuerdo.
2. Traza pantalla → cliente HTTP → recurso REST → autorización del servidor.
   Consulta [contratos](../../../docs/contratos.md) antes de definir tipos o formularios.
   No inventes un endpoint porque una pantalla lo necesite.
3. Reutiliza [cliente.ts](../../../carga-front/src/api/cliente.ts),
   [tipos.ts](../../../carga-front/src/tipos.ts) y utilidades existentes. Conserva
   base `/carga/api`, Bearer, correlación, expiración de sesión, errores y `204`.
   Un genérico TypeScript no valida el JSON en ejecución; trata entradas externas
   según el riesgo sin introducir un framework de validación automáticamente.
4. Separa coordinación de carga/acciones de componentes de presentación cuando
   exista una responsabilidad reutilizable. Usa props y callbacks explícitos;
   no obligues a dividir todos los componentes ni construyas capas vacías.
   Mantén reglas de negocio y autorización en el central, no en la vista.
5. Reutiliza [AuthProvider](../../../carga-front/src/auth/AuthProvider.tsx) y
   [RutaProtegida](../../../carga-front/src/auth/RutaProtegida.tsx).
   Ocultar una ruta/botón solo controla presentación. No trates roles o IDs enviados
   por el navegador como prueba de permiso; conserva manejo diferenciado de 401/403.
   No cambies almacenamiento ni duración de tokens como refactor incidental.
6. Representa carga, vacío, éxito, validación y fallo con estados coherentes.
   Evita respuestas obsoletas al cambiar filtros/ruta y envíos duplicados accidentales;
   eso no sustituye idempotencia del servidor. No reintentes mutaciones automáticamente.
7. Conserva estilo y componentes existentes. Para formularios, usa etiquetas,
   errores asociados, foco y controles semánticos accesibles por teclado;
   no comuniques información solo por color. No introduzcas HTML no confiable.

## Pruebas y comprobaciones

Antes de cambiar lógica, busca infraestructura de tests real. Al crear esta skill
no hay script `test` en `package.json`: vuelve a comprobarlo, no inventes un comando
ni instales un runner. Si sigue ausente, documenta escenarios y la limitación de
TDD; no presentes un plan como pruebas escritas o ejecutadas.

Desde la raíz, `git diff --check`, revisión del diff y parseo estático de JSON
cuando se modifique. No ejecutes `tsc` (tampoco `--noEmit`), builds npm/Docker ni
linters que compilen. Revisa el script antes de cualquier otra validación.

Entrega archivos, contrato consumido, comprobaciones reales y escenarios pendientes
(carga/error, sesión expirada, permisos, teclado y respuesta tardía). No atribuyas
validación visual, de tipos ni de ejecución a la lectura del código.
