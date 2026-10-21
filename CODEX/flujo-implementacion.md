# Flujo de implementación

Aplica a cambios solicitados; no activa auditoría independiente. Las restricciones de [AGENTS.md](../AGENTS.md) rigen todo el flujo.

## 1. Recuperar contexto y delimitar

1. Comprueba estado/diff de Git, conserva cambios ajenos y lee el [contexto](contexto-proyecto.md), [mapa](mapa-arquitectura.md) y fuentes pertinentes de la [tabla de navegación](README.md).
2. Identifica caso de uso, frente dueño, criterios de aceptación y consumidores. Sigue el recorrido de código hasta persistencia/integración y revisa sus pruebas: no copies una clase como patrón sin contrastar estándares y limitaciones.
3. Si falta una decisión de negocio, hay contradicciones relevantes o un cambio rompe contratos, expón evidencia y alternativas. Pregunta y DETENTE hasta recibir respuesta. No elijas silenciosamente por el equipo.

## 2. Proponer el cambio mínimo

- Explica qué comportamiento cambia, en qué capa y con qué riesgo. Conserva interfaces/DTO publicados y autorización por principal/pertenencia.
- Sitúa transporte en web, negocio y transacciones en servicios; justifica abstracciones con un punto de variación real. SOLID no exige una interfaz por clase ni una arquitectura nueva.
- Coordina modificaciones de tablas, endpoints, mensajes o DTO compartidos. Si hay política arquitectónica nueva acordada, registra ADR con alternativas y consecuencias; no crees ADR para cambios mecánicos.

## 3. Diseñar pruebas ANTES del comportamiento

Strict TDD activo: especifica el fallo esperado antes de escribir lógica. Para defectos añade una prueba de regresión; cubre casos positivo/negativo y, si aplica, permisos, duplicados, concurrencia, rollback e indisponibilidad. Reutiliza JUnit 5 y las utilidades existentes; evita añadir otro framework sin necesidad.

**No ejecutar builds.** Maven `test`/`verify` recorren fases de compilación, incluso seleccionando un módulo/test; no están autorizados. Tampoco se debe usar `javac`, `tsc`, Docker con build ni scripts que compilen por debajo. No intentes eludirlo ejecutando artefactos previos como si probaran el código modificado.

Si el runner necesita compilar, redacta primero la prueba y la implementación mínima, pero informa: **“Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds”**. NO declares TDD verificado. Si ejecutar fuera indispensable para avanzar, solicita una autorización que cambie explícitamente la restricción y espera respuesta; no es un paso automático.

Para cambios exclusivamente documentales/configuración no inventes pruebas de aplicación: verifica estáticamente sintaxis y referencias.

## 4. Implementar y revisar el propio cambio

- Mantén español en nombres/comentarios según [CONTRIBUTING](../CONTRIBUTING.md), sin tildes/ñ en fuente Java; UTF-8, LF y formato existente. Explica el porqué, no repitas el código.
- Reutiliza contratos `carga-api`, inyección CDI/EJB y resiliencia existentes. No expongas entidades ni lleves negocio a componentes React/JSF/REST.
- Crea migraciones nuevas; considera reentregas, restricciones únicas y transacciones. Una prueba unitaria con un bean creado directamente no verifica roles/locks/JTA reales.
- Revisa tu diff contra aceptación y SOLID, sin lanzar el agente auditor. No mezcles refactors generales, correcciones ajenas ni cambios de herramientas.
- Actualiza documentación que realmente afectaste, sin cambiar unilateralmente estados de ADR ni ocultar discrepancias.

## 5. Comprobar y entregar

Permitido sin compilar: `git diff --check`, revisar diff y enlaces locales, parsear TOML/JSON/XML con herramientas ya disponibles. Un parse correcto no demuestra semántica ni funcionamiento del sistema. Comprueba previamente qué hace cualquier comando; no ejecutes instalaciones ni tareas con efectos desconocidos.

El [CI existente](../.github/workflows/ci.yml) documenta validaciones Java/frontend/integración, pero no habilita ejecutarlas localmente contra las instrucciones del usuario ni permite afirmar resultados remotos no consultados.

Entrega:

1. Cambios y justificación, con archivos y contratos afectados.
2. Comprobaciones ejecutadas y sus resultados reales.
3. Pruebas nuevas/modificadas, pruebas no ejecutadas y motivo; limitaciones de cobertura y runtime.
4. Riesgos/decisiones pendientes y siguiente paso. Guarda descubrimientos y resumen en Engram si está disponible.

No hagas stage, commit, push, PR ni auditoría independiente sin solicitud. Si se pide auditoría después, usa el [checklist](checklist-auditoria.md).
