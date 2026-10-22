# Guía de trabajo de Codex

Esta carpeta orienta al agente; NO sustituye los requisitos ni duplica sus contratos. El punto de entrada de instrucciones es [AGENTS.md](../AGENTS.md), en la raíz. Antes de implementar, lee también [contexto](contexto-proyecto.md), [arquitectura](mapa-arquitectura.md) y [flujo](flujo-implementacion.md).

## Qué consultar según la tarea

| Tarea | Fuentes que deben orientar el cambio |
| --- | --- |
| Reglas o casos de negocio | [Casos críticos](../docs/GuiaCUCrit.md), [reglas](../docs/Estandares/RULES.md), [frentes](../docs/frentes.md), pruebas del servicio afectado |
| REST, DTO, SOAP o JMS | [Contratos](../docs/contratos.md), [diseño de APIs](../docs/Estandares/api-design.md), productor y consumidor reales |
| Persistencia y transacciones | [ADR-0001](../docs/decisiones/0001-flyway-para-el-esquema.md), [migraciones](../carga-ejb/src/main/resources/db/migration/), entidades y límites EJB |
| Identidad y permisos | Caso crítico 1, contratos, paquetes `seguridad`, servicio invocado y sus pruebas; para P4, [ADR-0007](../docs/decisiones/0007-seguridad-de-nodos-y-endpoints-p4.md) |
| Guías y asignaciones | Caso crítico 2, sección Guías P2 del contrato, `GuiaServicioBean` y advertencia sobre ADR-0005 en el contexto |
| Balanzas, Tracking o recorridos | Casos críticos 4/5, contratos y ADR-0002, 0006–0010 en el [índice](../docs/decisiones/README.md) |
| Interfaz web | [ADR-0004](../docs/decisiones/0004-frontend-react-sobre-api-rest.md), [SPA](../carga-front/src/), [vistas JSF](../carga-web/src/main/webapp/) y API consumida; no confundir controles visuales con autorización |
| Configuración/despliegue | [README](../README.md), [entorno de ejemplo](../.env.example), [Compose](../docker-compose.yml), [Docker](../docker/), [CI](../.github/workflows/ci.yml) como referencias, sin ejecutar builds |
| Auditoría solicitada | [Checklist de auditoría](checklist-auditoria.md), alcance/diff pedido y fuentes anteriores pertinentes |

## Agentes en Codex para VS Code

Las definiciones están en [implementador.toml](../.codex/agents/implementador.toml) y [auditor.toml](../.codex/agents/auditor.toml). Son agentes personalizados, NO skills ni comandos slash. Heredan la configuración de modelo y permisos de la sesión; no se fija un modelo ni se amplía acceso.

Abre una nueva conversación en el proyecto para cargar `AGENTS.md` y pide, por ejemplo:

> Usa el agente implementador para implementar [tarea], preservando [contrato] y con estos criterios de aceptación: […].

> Usa el agente auditor para revisar [diff, archivos o caso de uso]. No modifiques archivos; entrega hallazgos con evidencia.

La auditoría se ejecuta **solo cuando la solicitas**. Su prohibición de editar es una instrucción de comportamiento, no un aislamiento técnico: para garantías de solo lectura se requieren permisos adecuados en la sesión.

Si tu versión/configuración de la extensión no ofrece estos agentes, utiliza las mismas guías en la conversación principal y pide explícitamente el rol. No supongas que se ejecutó un subagente: verifica su disponibilidad en tu cliente. Estas definiciones requieren comprobación funcional en VS Code; la validación de TOML no demuestra que el cliente las haya cargado.

Esquema de referencia: [AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md) y [agentes personalizados](https://learn.chatgpt.com/docs/agent-configuration/subagents).

## Skills del proyecto

El catálogo y los disparadores están en [AGENTS.md](../AGENTS.md#habilidades-especializadas-del-proyecto);
las instrucciones reutilizables viven en [`.agents/skills/`](../.agents/skills/).
No son agentes adicionales: especializan el trabajo de quien ejecuta la tarea,
incluidos `implementador` y `auditor` sin cambiar sus permisos ni su alcance.

Para invocarlas explícitamente en Codex IDE/CLI, menciona su nombre con `$`, por ejemplo:

> Usa $jakarta-testing para escribir la regresión de [defecto], sin ejecutar builds.

Las descripciones permiten selección automática según la tarea. La creación y
validación estática de los archivos no demuestran que tu cliente ya los haya
descubierto; comprueba su aparición en `/skills` en una nueva conversación.
Referencia: [skills en Codex](https://learn.chatgpt.com/docs/build-skills).

## Mantenimiento

- Actualiza el mapa si cambia un módulo o un límite y el contexto si se resuelve una discrepancia. Enlaza la fuente, no copies su contenido completo.
- No uses esta carpeta para aceptar políticas de producto: corresponde acordarlas en los contratos/ADR originales.
- Al cerrar una tarea informa evidencia, verificaciones y pendientes según [AGENTS.md](../AGENTS.md). No hagas una auditoría independiente por defecto.
