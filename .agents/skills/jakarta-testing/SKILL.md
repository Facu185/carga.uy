---
name: jakarta-testing
description: >-
  Diseña y escribe pruebas JUnit 5 y regresiones de carga.uy con Strict TDD.
  Usar al cambiar lógica Java, pruebas de servicios, contratos o integración
  WildFly/PostgreSQL/JMS. Respeta la prohibición de builds; no es una skill Go.
---

# Pruebas Jakarta de carga.uy

## Condición de trabajo

Lee [AGENTS.md](../../../AGENTS.md), [CODEX](../../../CODEX/README.md) y el
[flujo de pruebas](../../../CODEX/flujo-implementacion.md). Strict TDD exige prueba
primero; la prohibición de builds impide verificar rojo/verde si el runner compila.
No ejecutes Maven `test`/`verify`, `javac`, builds ni artefactos previos como sustituto.
No instales un framework para superar esa restricción.

## Elegir evidencia

| Afirmación que se quiere comprobar | Evidencia necesaria |
| --- | --- |
| Validación o regla determinista | Prueba unitaria de entradas, resultado y efectos |
| Serialización/status/compatibilidad | Prueba de contrato con productor y consumidor |
| Rol aplicado, principal o interceptores EJB | Integración atravesando el contenedor |
| Lock, carrera, unicidad o rollback real | Integración con transacciones y base reales |
| Commit base/JMS o reentrega | Integración con contenedor y broker configurados |

Un bean creado directamente o un proxy falso puede verificar una decisión o una
interacción; NO demuestra seguridad declarativa, atomicidad ni exclusión concurrente.

## Prueba primero

1. Deriva el escenario del caso crítico/contrato y describe qué falla antes del cambio:
   entrada, identidad, estado inicial, resultado esperado y efectos prohibidos.
   En un defecto, el escenario debe reproducir la causa, no solo el texto del parche.
2. Lee el test vecino y el POM del módulo. Reutiliza JUnit 5, dobles y utilidades
   existentes; no supongas Mockito, Arquillian o un runner frontend instalado.
3. Escribe el test ANTES de la implementación. Comprueba resultados y efectos
   observables; evita fijar detalles internos salvo que sean parte de la garantía.
   Usa reloj/datos controlados si el comportamiento depende del tiempo.
4. Selecciona bordes pertinentes: ausencia de principal, rol o vínculo; período
   `[inicio, fin)`; duplicado secuencial y simultáneo; nodo caído; fallo entre efectos;
   redelivery. No añadas casos ajenos solo por completar una lista.
5. Implementa lo mínimo y revisa estáticamente el test y la lógica. Para concurrencia,
   diseña dos operaciones coordinadas sobre transacciones independientes, no una
   secuencia de llamadas ni una espera arbitraria como prueba de carrera.

## Referencias de trabajo

- [GuiaServicioBeanTest](../../../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/servicio/GuiaServicioBeanTest.java):
  escenarios de negocio y dobles; no prueba los locks reales del contenedor.
- [MigracionesContraPostgresTest](../../../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/persistencia/MigracionesContraPostgresTest.java):
  integración Testcontainers etiquetada `integracion`; no arrancarla bajo la restricción.
- [POM](../../../pom.xml) y [CI](../../../.github/workflows/ci.yml): configuración del equipo,
  no autorización para ejecutar ni evidencia de resultados remotos.

## Validar sin compilar y entregar

Desde la raíz, ejecuta `git diff --check` y revisa imports, firmas, fixtures y
aserciones contra el código actual. Es una comprobación estática, no un test verde.

Entrega archivo/test, fallo esperado, nivel de evidencia y ejecución pendiente.
Usa la frase **“Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de
builds”** cuando corresponda. Si no hay entorno que pruebe una garantía, indícalo
como pendiente; no presentes un diseño de integración como test ejecutado.
