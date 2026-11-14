# Validar exclusión entre chofer y responsable

Alcance confirmado en [ADR-0025](../docs/decisiones/0025-incompatibilidad-chofer-responsable.md).
Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.
La SPA no dispone de script `test`; los casos de navegador son escenarios pendientes.

## Regresiones unitarias preparadas

- `CompatibilidadFuncionarioEmpresaTest`: identidad persistida, orden del bloqueo,
  incompatibilidad inversa, registros inactivos y ejercicio heredado sin mutación.
- `SolicitudesEmpresaIncompatibilidadTest`: token CIUDADANO anterior al alta del
  chofer, solicitudes, ambas aprobaciones y transferencia sin revocar al anterior;
  ciudadano compatible y aprobación después de baja del chofer.
- `ChoferServicioIncompatibilidadTest`: alta activa, cambio de cédula y reactivación
  incompatibles también por administrador; registros inactivos, baja administrativa
  y habilitación compatible. Los dobles no ejecutan SQL, JTA ni interceptores.

## Pendiente en navegador y servidor real

| Caso | Resultado esperado |
| --- | --- |
| Mi cuenta CHOFER, solo o con CIUDADANO | Sin tarjeta, botón ni formulario de responsabilidad; conserva datos y Mis guías |
| Ciudadano sin chofer ni responsabilidad | Conserva solicitud, carga, error y reintento existentes |
| Funcionario o responsable compatible | Conserva navegación y operaciones anteriores |
| Chofer con vínculo responsable heredado | Navegación de chofer; funciones de responsable devuelven 403, sin revocación ni pérdida de historial |
| REST/JSF directo con chofer activo y JWT ciudadano anterior | Solicitar vínculo/alta: 400; no solicitud ni auditoría de éxito |
| Aprobar alta, vínculo o transferencia hacia chofer activo | 400; solicitud pendiente, sin empresa nueva ni revocación del anterior |
| Alta/PUT activo o reactivación de responsable como chofer | 400; identidad, actividad, nombre y empresa anteriores intactos |
| Registro inactivo, baja de chofer o transferencia hacia ciudadano compatible | Permitido según los permisos existentes; historial conservado |
| Operaciones propias del chofer y funciones oficiales | Permisos previos conservados; no convertir la regla en prohibición general del rol CHOFER |

## Carreras y rollback: pendientes

En WildFly/PostgreSQL, usar transacciones independientes coordinadas, no sleeps:

1. A toma el lock por cédula al habilitar un chofer; B intenta aprobar
   responsabilidad para esa cédula y queda esperando. Confirmar A y comprobar
   rechazo de B, sin efectos parciales. Repetir invirtiendo el ganador.
2. Repetir con agregar activo, PUT que activa/cambia cédula y reactivación;
   responsabilidad por aprobar alta, aprobar vínculo y transferencia.
3. Repetir sin Usuario inicial: A registra chofer; B crea su identidad mediante
   primer login y luego intenta aprobar responsabilidad antes del commit de A.
4. Forzar rollback del ganador: el otro flujo debe poder seguir tras liberarse
   el lock si el estado final es compatible. Comprobar aislamiento y ausencia de
   datos/resoluciones/revocaciones parciales.

No ejecutado: falta acreditar la consulta nativa a través del proveedor JPA,
su adhesión a JTA, visibilidad tras espera y seguridad declarativa en contenedor.
