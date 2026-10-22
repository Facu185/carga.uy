# ADR-0014 — Perfil propio de empresa con autorización específica

**Estado:** Aceptada para F10 por confirmación explícita del usuario
**Fecha:** 2026-10-22

## Contexto

El usuario aprobó D0-05/C10 al pedir aplicar la recomendación para la fase 2.
`EmpresaServicio.modificar` es administrativo y modifica también `fechaAlta`;
ampliar sus roles concedería capacidades ajenas al perfil propio.

## Decisión

- Publicar GET/PUT `/carga/api/empresas/{id}/perfil`: 200 con `EmpresaDTO`.
  El ID selecciona; el servidor autoriza por principal, rol CIUDADANO/CHOFER,
  vínculo RESPONSABLE/ACTIVO y empresa activa. Sin permiso, empresa inactiva o
  inexistente: 403 indistinguible; sin sesión: 401; datos inválidos/negocio: 400.
- Entrada `EditarPerfilEmpresaDTO` con nombre, razón social, número y dirección.
  No contiene identidad, fecha de alta, estado ni vínculos. Solo esos cuatro
  atributos se asignan; no se usa merge ni se invoca modificación administrativa.
- `PerfilEmpresaServicioLocal` delimita esas dos operaciones y su EJB específico;
  no se amplía `EmpresaServicio` ni su interfaz remota/SOAP. Se reutiliza el DAO
  P1 de comprobación de responsable activo, sin modificar identidad ni vínculos.
- Nombre obligatorio, número positivo/único y longitudes existentes del esquema
  (200/200/300 para nombre/razón social/dirección). Razón social/dirección siguen
  opcionales. No se recalcula fechaAlta, aunque sea nula en datos heredados.
- PUT bloquea la empresa antes de mutarla y fuerza flush dentro de JTA. Se
  comprueba duplicado y se traduce también una violación de unicidad PostgreSQL
  detectada al flush a negocio; el rollback real requiere prueba de contenedor.
- Integrar `/mi-empresa/perfil` en la SPA existente. Resolver una única empresa
  responsable activa, sin escoger arbitrariamente ante ambigüedad; conservar
  rutas de Choferes/Vehículos y JSF. Es alcance F10 acotado, no aceptación general
  de ADR-0004 ni cambio de D0-04 sobre guías.

## Alternativas y consecuencias

- Ampliar el ABM existente evita una interfaz, pero mezcla autorizaciones y
  campos administrativos. Se descarta a favor de una frontera de perfil estrecha.
- Aceptar EmpresaDTO como entrada reduce tipos, pero expone campos no editables.
  Se usa DTO específico a costa de un contrato adicional.
- Permitir lectura de empresas inactivas requeriría autorización histórica distinta;
  se elige denegar ambas operaciones, conforme al acuerdo confirmado.
- No se cambia esquema, roles P1, TTL, solicitudes ni auditoría administrativa.
  La publicación del contrato permite coordinar P1/P3; no inventa aprobaciones
  externas ni valida el despliegue compartido.
- Backend: pruebas antes de implementar, sin ejecutar compilación. La SPA no
  tiene runner de tests; los escenarios de navegador se documentan antes del
  formulario, sin presentarlos como tests automatizados.
