# ADR-0017 — Datos de identidad por UserInfo e integración con ID Uruguay testing

**Estado:** Aceptada para el alcance confirmado por el usuario
**Fecha:** 2026-10-23

## Contexto

El cliente OIDC existente validaba el `id_token` pero también esperaba cédula,
nombre y correo dentro de él. El CU1 describía ese origen. La documentación
[oficial de ID Uruguay](https://www.gub.uy/agencia-gobierno-electronico-sociedad-informacion-conocimiento/comunicacion/publicaciones/documentacion-tecnica-id-uruguay/documentacion-tecnica-id-uruguay/id-0)
describe la obtención de datos personales por UserInfo con `access_token` Bearer
y exige que su `sub` coincida exactamente con el del `id_token`.

El usuario confirmó completar UserInfo, alinear la documentación y preparar
testing, conservando el cierre local actual. Esto no atribuye aceptación externa
a P1/P3 ni acredita pruebas reales. Las credenciales y URIs de testing no
habilitan producción.

## Decisión

- Conservar Authorization Code + PKCE, `state`, `nonce`, firma RS256, emisor,
  audiencia y vigencia del `id_token`; validarlo ANTES de consultar UserInfo.
- Descubrir `userinfo_endpoint` y consultar mediante GET con Bearer en header,
  nunca en URL. Mantener timeouts y no seguir redirecciones HTTP con credenciales.
- Exigir igualdad exacta entre ambos `sub` y documento no vacío en UserInfo.
  La validación de cédula sigue en `IngresoGubUyBean`; no se inventan políticas
  para documentos extranjeros ni nuevos niveles de identidad.
- Leer nombre/correo de UserInfo como opcionales, conservando la semántica
  existente. No usar claims personales del `id_token` como fallback silencioso.
- Una falla de UserInfo impide el ingreso; no se confunde con la degradación
  permitida por PDI, que ocurre después de verificar la identidad.
- Conservar interfaces/DTO, rutas REST, roles y destinos web/móvil. No persistir
  tokens del proveedor ni añadir logout federado; el cierre sigue siendo local.
- Mantener simulador en Compose base y ofrecer override explícito para testing,
  con secreto obligatorio externo al repositorio y redirect registrado exacto.

## Alternativas consideradas

- **Solo variables de entorno:** menor cambio, pero dependería de datos personales
  dentro del `id_token`, no acreditados contra el proveedor real.
- **UserInfo con fallback al token:** menos fallos aparentes, pero ocultaría
  respuestas inválidas o permisos incompletos del proveedor. Descartada.
- **Logout federado ahora:** requeriría retener/proteger información del proveedor
  y ampliar el ciclo de sesión. Fuera del alcance confirmado.

## Consecuencias

Una consulta remota adicional por ingreso, con timeout de 10 segundos y conexión
de 5 segundos; no se reintenta automáticamente el canje del código de un solo uso.
El despliegue sigue requiriendo secreto vigente y prueba completa manual.
Las pruebas con proveedor local no acreditan compatibilidad real, efectos JTA
ni seguridad del contenedor. Véase la [guía de testing](../integracion-id-uruguay-testing.md).
