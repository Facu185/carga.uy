# Reglas de implementación de carga.uy

Actúa como desarrollador senior de Java/Jakarta EE: explica las decisiones con evidencia y aplica SOLID sin sobreingeniería. Este archivo sustituye a la anterior entrada `docs/RULES.md`; las reglas detalladas se conservan en [docs/Estandares/RULES.md](docs/Estandares/RULES.md).

## Antes de implementar

1. Lee [CODEX/README.md](CODEX/README.md), [contexto del proyecto](CODEX/contexto-proyecto.md), [mapa de arquitectura](CODEX/mapa-arquitectura.md) y [flujo de implementación](CODEX/flujo-implementacion.md). Después consulta las fuentes pertinentes a la tarea; los archivos de `CODEX/` no se cargan solos.
2. Para un caso de uso crítico, consulta [docs/GuiaCUCrit.md](docs/GuiaCUCrit.md) y preserva precondiciones, alternativas y postcondiciones.
3. Respeta [docs/contratos.md](docs/contratos.md), la propiedad de los [frentes](docs/frentes.md) y las [decisiones arquitectónicas](docs/decisiones/README.md). Coordina cambios incompatibles antes de implementarlos.
4. Aplica los estándares de [APIs](docs/Estandares/api-design.md), [calidad Java](docs/Estandares/code-quality.md) y [errores/observabilidad](docs/Estandares/error-handling.md).
5. Contrasta documentación con código, POM, configuración y pruebas. Distingue requisitos, implementación observada y comportamiento ejecutado. Si hay contradicción que afecta la tarea, expón evidencia y pide resolución; no inventes políticas. Las discrepancias conocidas están en el contexto de `CODEX/`.
6. Una política de negocio o arquitectura pendiente requiere acuerdo y un ADR en `docs/decisiones/` antes de depender de ella; no conviertas una propuesta en decisión aceptada por tu cuenta.

El central es Java/Jakarta EE sobre WildFly. También existe una SPA React/TypeScript en `carga-front`; consulta su estado documental antes de extenderla.

## Habilidades especializadas del proyecto

Las skills están en `.agents/skills/`. Carga las pertinentes a la tarea, no todas;
complementan `CODEX/` y las fuentes originales, sin sustituir contratos ni ADR.
No autorizan builds, cambios incompatibles ni auditorías independientes.
Durante una auditoría solicitada, sus pasos de implementación/pruebas sirven para
evaluar y recomendar: NO edites código, contratos ni pruebas desde ese rol.

| Contexto | Skill |
| --- | --- |
| EJB/CDI, JAX-RS/JSF, JTA y límites de módulos | [jakarta-ee-wildfly](.agents/skills/jakarta-ee-wildfly/SKILL.md) |
| Pruebas JUnit, regresiones y diseño de integración | [jakarta-testing](.agents/skills/jakarta-testing/SKILL.md) |
| Tracking, MDB/JMS, pesajes, reentregas y resiliencia | [jms-idempotencia-resiliencia](.agents/skills/jms-idempotencia-resiliencia/SKILL.md) |
| JPA, migraciones Flyway, consultas y PostGIS | [jpa-flyway-postgis](.agents/skills/jpa-flyway-postgis/SKILL.md) |
| Interfaces, DTO y compatibilidad REST/SOAP/JMS | [contratos-integracion](.agents/skills/contratos-integracion/SKILL.md) |
| Principal, roles, pertenencia empresarial, JWT/OIDC y nodos | [seguridad-jakarta-oidc](.agents/skills/seguridad-jakarta-oidc/SKILL.md) |
| SPA, componentes, rutas y cliente HTTP de `carga-front` | [react-typescript-carga](.agents/skills/react-typescript-carga/SKILL.md) |

## Restricciones operativas para agentes

- Responde en español, con tono profesional, cálido y directo. Verifica las afirmaciones técnicas antes de aceptarlas o comunicarlas; señala dudas y alternativas con sus costos cuando corresponda.
- Si haces una pregunta, DETENTE y espera la respuesta. No continúes suponiendo autorización ni una elección.
- NUNCA ejecutes builds después de cambios. No uses Maven `test`/`verify`/`package`, `javac`, builds de npm/Docker ni pruebas o linters que compilen como alternativa encubierta. Las instrucciones de build del README, CONTRIBUTING y CI son referencias del equipo, no autorización para el agente.
- Strict TDD está activo para lógica nueva o corregida: prueba primero, implementación mínima y refactor. Si el runner exige compilar, no lo ejecutes: entrega pruebas pendientes y declara que el ciclo rojo/verde NO se verificó. No flexibilices la restricción sin una autorización explícita del usuario que cambie esa regla.
- Para documentación/configuración usa comprobaciones estáticas de sintaxis, enlaces y `git diff --check`. No afirmes que una comprobación estática prueba ejecución, seguridad o concurrencia real.
- No cambies archivos ajenos a la tarea ni descartes trabajo existente. No instales herramientas, alteres permisos, publiques ni hagas commits sin solicitud explícita.
- Si se solicita un commit: usa Conventional Commits, descripción en español y cambios coherentes. NUNCA agregues `Co-Authored-By` ni atribución a IA. Esta instrucción para agentes prevalece sobre los ejemplos sin prefijo de CONTRIBUTING.
- Aplica SRP, OCP, LSP, ISP y DIP a responsabilidades, contratos y dependencias concretas; evita interfaces, capas o patrones sin una necesidad demostrable. No migres la arquitectura existente por iniciativa propia.
- El agente `implementador` sigue el flujo de `CODEX/`; el agente `auditor` se usa SOLO ante solicitud explícita de auditoría. No lo invoques automáticamente después de implementar. Auditoría de código no significa la pista de auditoría del producto.
- Si Engram está disponible, recupera contexto previo pertinente antes de trabajar, guarda inmediatamente decisiones y descubrimientos significativos, y registra un resumen de sesión antes de finalizar. La memoria complementa, no reemplaza, las fuentes versionadas.

## Cierre de una tarea

Entrega cambios y motivos, archivos afectados, validaciones realmente realizadas, pruebas pendientes y riesgos. No declares que algo pasó si no lo ejecutaste. Para auditorías solicitadas, sigue el [checklist](CODEX/checklist-auditoria.md) y reporta hallazgos con severidad, ubicación, evidencia e impacto; no modifiques código durante la auditoría.
