# Comprobación del ABM de vehículos del responsable

Alcance: [ADR-0012](decisiones/0012-abm-vehiculos-del-responsable.md).
Este documento describe escenarios, NO resultados de ejecución.

## Prueba primero y límites

- Añadir regresiones JUnit en `VehiculoServicioBeanAutorizacionTest`: baja propia
  con guía EN_CURSO rechazada sin mutación; tampoco el administrador ni un PUT
  con habilitado=false pueden eludirla; baja sin guía en curso permitida;
  reactivación propia sin alterar otros datos; reactivación ajena/sin vínculo
  denegada; vehículo inexistente; fechas ausentes como null.
- Los dobles comprueban decisiones, parámetros y efectos; no ejecutan JPQL ni
  interceptores. Antes del cambio, la baja no consulta guías y modifica el estado.
- `VehiculoRecursoTest` comprueba la ruta/PUT declarados y que la reactivación
  delega el ID y propaga denegaciones al mapper. No acredita respuesta 204,
  serialización ni seguridad real: comprobarlo en los escenarios de integración.
- `carga-front/package.json` no define runner de tests. No instalar uno ni
  presentar escenarios manuales como pruebas automatizadas.
- Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.

## Escenarios pendientes en navegador y servidor reales

1. Ingresar como responsable aprobado. Desde Mi empresa, abrir Vehículos y por
   URL directa `/mi-empresa/vehiculos`. Empresa fija; pestañas Choferes/Vehículos.
2. Probar carga, lista vacía, error/reintento y red lenta. Salir/cambiar de pantalla
   durante una consulta no debe presentar resultados tardíos ni permitir envíos
   repetidos. Revisar móvil, teclado, foco de edición/cancelación y errores accesibles.
3. Alta con todos los campos: matrícula, marca/modelo, peso vacío, capacidades kg/m³,
   permiso y fechas e ITV. Comprobar mayúsculas, duplicado, fechas inválidas,
   rangos, peso negativo, capacidades nulas/no positivas y campos obligatorios.
4. Editar un vehículo activo y otro dado de baja. La edición conserva habilitación;
   no debe reactivar inadvertidamente. Verificar fechas opcionales ausentes.
5. En datos de prueba controlados, una guía EN_CURSO del mismo vehículo impide
   DELETE y PUT con habilitado=false, conservando TODOS los campos. Una guía
   EN_CURSO de otro vehículo no bloquea la baja. CREADA/ASIGNADA/FINALIZADA no
   bloquean. No inferir estado por fechas pasadas/futuras.
6. Baja sin guía EN_CURSO: confirmar/cancelar; confirmar deshabilita sin eliminar
   registros. Reactivar recupera habilitación, conservando permiso, ITV y capacidades;
   repetir la reactivación es inocuo. La vigencia de circulación se valida al asignar.
7. Sin sesión: 401; sin rol/vínculo o vínculo revocado: 403. Alterar empresaId/id
   para listar, crear, editar, dar de baja o reactivar recursos ajenos: denegado en
   servidor. Administrador conserva alcance global, no elude la regla EN_CURSO.
8. Cero vínculos responsables activos: sin formulario ni mutaciones. Más de una
   empresa responsable activa: mostrar inconsistencia y no escoger la primera.
9. Validar JPQL real y roles/JTA/locks en WildFly/PostgreSQL con transacciones
   independientes coordinadas. Baja y modificación/reactivación del mismo vehículo
   deben serializarse. Al integrar P3, el inicio de viaje también debe tomar ese
   bloqueo y volver a verificar habilitación antes de cambiar EN_CURSO: el ABM no
   puede garantizar ese flujo todavía ausente del central inspeccionado.
10. Verificar regresión JSF (rutas existentes, mismo DTO y nuevos rechazos seguros),
    asignación de guías (baja excluida; reactivación no elude permiso/ITV) y expiración
    de sesión durante mutaciones. No reintentar automáticamente POST/PUT/DELETE
    cuando el resultado de red sea incierto: actualizar y comprobar primero.
