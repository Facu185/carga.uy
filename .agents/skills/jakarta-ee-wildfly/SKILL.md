---
name: jakarta-ee-wildfly
description: >-
  Implementa cambios del central carga.uy en Jakarta EE y WildFly. Usar al modificar
  servicios EJB/CDI, recursos JAX-RS/JSF, transacciones JTA o dependencias entre
  carga-api, carga-ejb, carga-web y carga-ear. No aplicar recetas de Spring Boot.
---

# Jakarta EE y WildFly en carga.uy

## Contexto y límites

Lee [AGENTS.md](../../../AGENTS.md) y las guías previas indicadas en
[CODEX](../../../CODEX/README.md). Esta skill complementa el
[flujo de implementación](../../../CODEX/flujo-implementacion.md), no cambia políticas.
Para una revisión solicitada, conserva el alcance de solo lectura del auditor;
esta skill no activa auditorías ni autoriza correcciones.

## Procedimiento

1. Identifica el recorrido recurso/backing bean → interfaz pública → servicio →
   persistencia/integración, con ayuda del [mapa](../../../CODEX/mapa-arquitectura.md).
   Lee los POM afectados y la configuración del servidor antes de cambiar dependencias.
   Consulta release, runtime y versiones reales; no confundas la imagen JDK con
   `maven.compiler.release`.
2. Sitúa cada responsabilidad sin migrar la arquitectura:

   | Responsabilidad | Lugar |
   | --- | --- |
   | Contratos y DTO compartidos | `carga-api` |
   | Negocio, autorización y transacciones | Servicios de `carga-ejb` |
   | Adaptación HTTP, SOAP y presentación JSF | `carga-web` |
   | Empaquetado y composición | `carga-ear` |

   Mantén `web → api ← ejb`; nodos y simuladores no dependen de EJB concretos.
   Reutiliza `EntityManager` o DAO según el caso existente: no impongas un repositorio
   por entidad ni una interfaz por clase.
3. Delimita las operaciones que deben confirmar o revertir juntas. Sigue la
   invocación por el contenedor, los atributos transaccionales y las excepciones
   hasta el límite que maneja el error. Una captura que devuelve éxito puede
   ocultar el fallo: determina el resultado contractual y el rollback necesario.
   No presupongas que una llamada interna atraviesa un proxy EJB.
4. Usa la inyección CDI/EJB existente. No construyas servicios gestionados con
   `new` en producción ni copies estereotipos, repositorios o configuración Spring.
   Conserva excepciones P4 de seguridad según los ADR, no muevas roles por uniformidad.
5. Escribe primero la prueba de comportamiento/regresión y realiza el cambio mínimo.
   Justifica abstracciones mediante una variación o dependencia concreta, no mediante
   etiquetas SOLID. Contrasta errores/DTO con los consumidores antes de cambiarlos.

## Fuentes según el cambio

- Reglas Java: [calidad](../../../docs/Estandares/code-quality.md).
- Fronteras: [contratos](../../../docs/contratos.md) y
  [discrepancias conocidas](../../../CODEX/contexto-proyecto.md).
- JTA, JMS y datasource: [configure.cli](../../../docker/central/configure.cli) y
  [persistence.xml](../../../carga-ejb/src/main/resources/META-INF/persistence.xml).
- Errores y correlación: [observabilidad](../../../docs/Estandares/error-handling.md).

## Comprobación y entrega

Desde la raíz, `git diff --check` y revisión del diff; no ejecutes objetivos Maven
que compilen, `javac`, builds Docker ni pruebas/linters que compilen. Enumera pruebas escritas,
pendientes de ejecución y garantías que requieren WildFly real (roles, JTA, locks,
broker). No atribuyas esas garantías a mocks ni a la presencia de anotaciones.
