---
name: seguridad-jakarta-oidc
description: >-
  Trabaja identidad y autorización de carga.uy en Jakarta Security/EJB, JWT Nimbus,
  OIDC gub.uy y tokens de nodos. Usar al cambiar autenticación, principal, roles,
  vínculo empresarial o acceso a recursos. No inicia una auditoría global ni migra algoritmos.
---

# Identidad y permisos de carga.uy

## Contexto obligatorio

Lee [AGENTS.md](../../../AGENTS.md), [CODEX](../../../CODEX/README.md), el caso
crítico de ingreso en [GuiaCUCrit](../../../docs/GuiaCUCrit.md),
[contratos](../../../docs/contratos.md) y [discrepancias](../../../CODEX/contexto-proyecto.md).
Trabaja sobre el cambio solicitado; una auditoría independiente requiere petición.

## Seguir la identidad hasta el recurso

1. Distingue credenciales: JWT de sesión del central, `id_token` del proveedor
   OIDC y token compartido de nodo no son intercambiables. Lee el POM y el código
   Nimbus/configuración existentes antes de aplicar recetas genéricas o ejemplos JS.
2. Sigue `Authorization: Bearer` desde
   [MecanismoJwt](../../../carga-web/src/main/java/uy/edu/fing/tse/carga/web/seguridad/MecanismoJwt.java)
   a `AutenticacionLocal`, principal/roles notificados al contenedor y servicio
   invocado. Deriva usuario/roles del servidor, nunca de un DTO o parámetro del cliente.
   Conserva controles de sesión vigente y cambio de clave temporal del flujo afectado.
3. Verifica autorización por operación y por objeto: un rol válido no acredita
   pertenencia empresarial. En guías, resuelve `VinculoEmpresaUsuario` activo desde
   el principal; no restaures `usuario.empresa_id` de la documentación desactualizada.
   Para chofer, no aceptes una identidad de chofer elegida por el cliente.
4. Aplica las excepciones deliberadas de
   [ADR-0007](../../../docs/decisiones/0007-seguridad-de-nodos-y-endpoints-p4.md):
   recursos REST P4 protegidos como EJB, servicios internos usados por agenda sin
   usuario. No muevas controles sin analizar todos los llamadores. El panel JSF
   abierto es una limitación conocida, no un modelo seguro que replicar.

## Según la credencial modificada

| Credencial | Aspectos a contrastar y conservar |
| --- | --- |
| JWT central | Algoritmo permitido y clave configurada, firma, caducidad y validez de sesión; claims exigidos por el contrato real |
| OIDC gub.uy | Flujo state/nonce/PKCE, callback/destino, firma y claims del proveedor, claves confiables y degradación acordada |
| Token de nodo | `X-Nodo-Token`, comparación segura y rechazo sin configuración; nunca enviarlo como JWT en `Authorization` |

Lee [ClienteOidcGubUy](../../../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/seguridad/ClienteOidcGubUy.java)
y [FiltroTokenNodo](../../../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/FiltroTokenNodo.java)
cuando corresponda. No conviertas TTL, algoritmo, almacenamiento o rotación
genéricos en políticas nuevas sin acuerdo y análisis de consumidores.

## Pruebas y entrega

Escribe primero casos positivo/negativo pertinentes: sin credencial, inválida o
vencida, rol insuficiente, recurso de otra empresa, vínculo inactivo, state/nonce
incorrecto o nodo sin secreto. No pongas secretos reales, tokens ni datos personales
en fixtures, logs, respuestas o memoria. Usa valores sintéticos.

Desde la raíz, `git diff --check` y revisión del recorrido completo; sin builds,
instalaciones ni llamadas a identidad real. Conserva 401/403 del contrato. Reporta
pruebas pendientes y qué controles necesitan el contenedor/proveedor para comprobarse:
anotaciones y dobles no acreditan seguridad en ejecución.
