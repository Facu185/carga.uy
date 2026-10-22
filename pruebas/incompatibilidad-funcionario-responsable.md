# ADR-0015 — Validación de incompatibilidad funcionario/responsable

Matriz especificada antes de la implementación. **PENDIENTE DE EJECUCIÓN**:
no se ejecutan builds ni se atribuyen locks, seguridad EJB o JTA a dobles unitarios.
Acuerdo: [ADR-0015](../docs/decisiones/0015-incompatibilidad-funcionario-responsable.md).

## Escenarios REST y datos persistidos

Usar entorno aislado, identidades sintéticas y sesiones reales sin publicar tokens
ni documentos de identidad en evidencia. Comprobar estados en una transacción
nueva, no solo el HTTP recibido. No modificar producción ni fixtures compartidos.

| Escenario | Resultado esperado |
| --- | --- |
| Funcionario activo con CIUDADANO solicita vínculo o alta de empresa | 400 con error de negocio; sin nueva solicitud/vínculo ni auditoría de éxito. |
| Funcionario activo destinatario de aprobación de vínculo o transferencia | 400; solicitud pendiente y responsable anterior intacto, sin revocación ni éxito auditado. Probar aprobador funcionario y administrador. |
| Solicitud de alta empresarial previa de persona ahora funcionario | 400 al aprobar; no crea empresa ni responsable, solicitud sigue pendiente. |
| Responsable activo intenta alta manual, reactivación o aprobación de solicitud funcionario | 400; estado funcionario/roles/dependencia/solicitud intactos. Un vínculo pendiente/revocado/rechazado u OPERADOR no equivale a RESPONSABLE/ACTIVO. |
| Responsable activo cuyo vínculo queda fuera de los primeros 200 registros, o cuya empresa está inactiva | También impide habilitación; no se decide a partir de listados truncados o empresa activa. |
| Datos heredados con ambas condiciones activas | Perfil propio, ABM vehicular/de choferes, creación y catálogos empresariales de guías y listado de guías empresariales denegados con 403; no se revoca vínculo ni se quita FUNCIONARIO. |
| Datos heredados y sesión anterior que no incluye FUNCIONARIO | Mismo bloqueo por padrón persistido; no depende de claims antiguos. |
| Funciones oficiales de funcionario, incluso con datos heredados | Mantiene consulta/edición/baja administrativa de empresas y resolución de solicitudes de otros usuarios compatibles. No se bloquea el login ni su perfil personal. |
| Resolución explícita del conflicto | Tras baja funcionario, reingresar si el token anterior incluía FUNCIONARIO (P1 lo invalida), y comprobar ejercicio responsable. Tras transferencia explícita, puede habilitarse funcionario. Sin borrados ni revocaciones automáticas. |
| Ciudadano/chofer responsable sin funcionario activo | Conserva permisos anteriores. No cambiar guías propias del chofer ni su alta/asignación de dominio. |
| Funcionario con vínculo OPERADOR y sin responsabilidad activa | No cambia D0-04: permisos ACTIVO sin tipo de creación/catálogos de guías se conservan; la incompatibilidad no resuelve esa política pendiente. |
| Sin sesión / rol insuficiente | Se conservan 401/403 declarativos. REST mantiene `{"error":"mensaje"}`; no publicar detalles internos. |

## Concurrencia y rollback (requiere WildFly/PostgreSQL)

1. Preparar una persona sin funcionario activo ni responsabilidad activa y una
   solicitud de vínculo pendiente. Dos transacciones independientes intentan
   habilitar funcionario y aprobar responsabilidad para **esa misma persona**.
2. Coordinar una barrera de integración controlada justo después de que una tome
   el lock Usuario, antes de mutar. La otra debe esperar el mismo lock. Liberar la
   primera, confirmar y verificar que la segunda relee el estado y rechaza.
   Repetir invirtiendo el orden y con aprobación de alta/transferencia y aprobación
   de solicitud funcionario/reactivación. No usar sleeps como prueba de exclusión.
3. Verificar por una transacción nueva que NUNCA se confirmaron ambos estados
   activos. Si la primera revierte, la segunda puede avanzar; no debe bloquearse
   globalmente a otras identidades.
4. Inyectar fallo controlado de auditoría después de una habilitación/aprobación
   compatible: verificar rollback de roles, funcionario, solicitud, empresa y
   vínculos afectados. Retirar la inyección. No registrar éxitos parciales.

## Evidencia disponible

- Regresiones JUnit preparadas antes del comportamiento: política interna, alta y
  reactivación manual/por solicitud, solicitudes/aprobación/transferencia de empresa
  y bloqueo del ejercicio empresarial. Los dobles comprueban que el rechazo
  ocurre antes de mutar/auditar éxito; no ejecutan JPQL, autorización EJB ni JTA.
- **Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
- No se actualizaron imágenes ni se ejecutaron estos escenarios contra la app
  local; probar un artefacto previo no validaría los cambios de fuente.
