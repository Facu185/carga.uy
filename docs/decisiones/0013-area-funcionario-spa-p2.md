# ADR-0013 — Área operativa del funcionario en la SPA existente

**Estado:** Aceptada para el alcance P2 solicitado por el usuario
**Fecha:** 2026-10-22

## Contexto

El usuario asignó a P2 la interfaz F12–F14 y solicitó comenzar la fase 0 del
[plan de cierre](../plan-cierre-p2.md), aplicando su recomendación preferida:
ampliar la SPA existente. Esta aceptación corresponde a esa elección y alcance;
no acredita acuerdos de permisos o contratos con los demás frentes.

La SPA ya tiene ingreso gub.uy, sesión y cliente REST. ADR-0011 conserva el ABM
JSF con cuentas internas, pero no proporciona un destino OIDC JSF.
`Funcionarios.tsx` administra accesos P1; no es el área operativa de P2.

## Decisión

- Implementar F12–F14 en `carga-front`, con React/TypeScript/Vite existentes,
  reutilizando autenticación, cliente REST y componentes. No incorporar framework,
  dependencias ni otro mecanismo de sesión como parte implícita de este acuerdo.
- Acceder como funcionario mediante el ingreso gub.uy de P1. Coordinar con P1/P3
  la navegación desde el portal; no crear cuentas internas ni un puente OIDC JSF
  para este recorrido. Las rutas nuevas de interfaz se concretarán al implementar.
- P2 compone empresas, vehículos/permisos/ITV y viajes/pesajes. P3 conserva
  eventos, fotos, deduplicación, estado e historial; P4 conserva pesajes,
  posiciones, recorridos y el adaptador de fin de viaje.
- Mantener `Funcionarios.tsx` y los circuitos de solicitudes/vínculos de P1.
  No trasladar F04/casos de P5 ni asignar F15 por esta decisión.
- Conservar todas las vistas JSF existentes. Esta excepción acotada al recorrido
  P2 no acepta globalmente ADR-0004, no migra el backoffice y no modifica la regla
  general de herramientas Java fuera de la SPA ya existente.
- Autorizar cada operación en el servidor. La consulta vehicular del funcionario
  no concede mutaciones; la consulta de guías no elimina el vínculo responsable
  exigido por el endpoint empresarial actual.

## Alternativas consideradas

- **JSF con OIDC:** reutiliza ABM, pero exige un puente/destino acordado con P1,
  además de comprobar navegación, postbacks y AJAX. No se elige para F12–F14.
- **Aceptar toda la migración de ADR-0004:** reduce la coexistencia a futuro,
  pero excede el pedido y sus acuerdos pendientes. No se decide aquí.
- **SPA acotada (elegida):** reutiliza infraestructura existente y evita un segundo
  recorrido de identidad; mantiene el costo de conservar también JSF.

## Consecuencias y condiciones de implementación

- Se cierra la elección de interfaz, NO toda la fase 0. Permisos, campos de F10,
  contratos nuevos y coordinaciones pendientes constan en el
  [registro de fase 0](../acuerdos-cierre-p2-fase-0.md).
- Antes de cada consumidor nuevo deben acordarse y publicarse contratos de
  lectura/edición con sus productores. No ampliar roles de clases completas para
  facilitar una pantalla ni presentar fixtures como integración real.
- El `GET /pesajes` actual devuelve pesadas persistidas, no disponibilidad del
  nodo. Una lista vacía no permite afirmar que Balanzas respondió sin resultados.
- La SPA no tiene script `test` en su `package.json`. Su estrategia de pruebas
  queda pendiente de acuerdo; no se instala un runner ni se atribuye TDD ejecutado.
- Esta fase es documental: no modifica comportamiento ni acredita navegador,
  seguridad de contenedor, JTA o integración. No se ejecutan builds.
