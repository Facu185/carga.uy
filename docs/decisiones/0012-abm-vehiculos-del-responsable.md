# ADR-0012 — Vehículos del responsable en Mi empresa

**Estado:** Aceptada para el alcance solicitado por el usuario
**Fecha:** 2026-10-22

## Contexto

Existen ABM EJB/REST/JSF de vehículos, pero el portal ciudadano React no tiene
su pantalla. El usuario confirmó: sección dentro de Mi empresa, una empresa por
responsable, todos los campos actuales sin adjuntos, baja impedida con guía en
curso y posibilidad de reactivar. Esta decisión acotada no acepta globalmente
ADR-0004, no sustituye JSF ni modifica la autenticación de ADR-0011.

## Decisión

- Incorporar `/mi-empresa/vehiculos` en la SPA existente. Resolver la empresa por
  los vínculos propios `RESPONSABLE/ACTIVO`, sin selector ni empresa editable.
  Si no hay exactamente una empresa responsable activa, no habilitar operaciones;
  no escoger arbitrariamente la primera. No se modifica el circuito P1 de vínculos.
- Mantener autorización en el EJB por principal y vínculo, con la excepción
  administrativa ya existente. No ampliar roles ni confiar en IDs del navegador.
- La baja es lógica (`habilitado=false`). Se rechaza cuando existe una guía del
  vehículo con estado persistido `EN_CURSO`, tanto por DELETE como por modificación
  del DTO. `CREADA`, `ASIGNADA` y `FINALIZADA` no bloquean esta regla. No inferir
  inicio/finalización por las fechas previstas ni modificar estados de guías.
- Reactivar mediante una operación específica que solo establece
  `habilitado=true`: `VehiculoServicio.reactivar(id)` y
  `PUT /carga/api/vehiculos/{id}/reactivar`, sin cuerpo y con respuesta 204.
  No renueva permisos/ITV; la asignación de guías mantiene sus validaciones.
- Modificación, baja y reactivación toman bloqueo pesimista de escritura sobre
  el vehículo dentro de JTA, compatible con el bloqueo usado al crear guías.
  Todo futuro proceso P3 que inicie un viaje debe coordinar la misma exclusión,
  volver a verificar habilitación y actualizar el estado en la misma transacción.
- Conservar firmas/rutas previas, DTO y errores `400 {"error":"mensaje"}` para
  negocio. La nueva restricción de baja es la evolución acordada por el usuario y
  también afecta a los consumidores JSF/REST existentes; coordinar con P2/P3 antes
  de integrar con otros frentes. No cambiar tablas ni migraciones aplicadas.

## Alternativas y consecuencias

- **Completar OIDC JSF:** requiere otro destino/puente de identidad; queda fuera
  del alcance confirmado para Mi empresa en React.
- **Reactivar reenviando el DTO completo:** no requiere endpoint nuevo, pero una
  fila antigua del navegador puede sobrescribir datos ajenos a la reactivación.
  Se elige una operación específica, a costa de ampliar el contrato de forma aditiva.
- **Bloquear también guías asignadas:** restringe viajes futuros; no fue la regla
  solicitada. No se incorpora silenciosamente.
- No se agregan adjuntos, dependencias ni infraestructura de tests frontend.
  JUnit verifica decisiones con dobles; JPQL, seguridad/JTA/locks y navegador
  requieren validación real pendiente. No se ejecutan builds.
- En el código central inspeccionado no existe todavía el flujo P3 que cambia
  las guías a `EN_CURSO`/`FINALIZADA`. La regla consulta el estado almacenado y no
  completa ese flujo como parte del ABM. Ver [pruebas](../pruebas-abm-vehiculos.md).
