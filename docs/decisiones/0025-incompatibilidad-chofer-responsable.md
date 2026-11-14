# ADR-0025 — Exclusión entre chofer activo y responsable de empresa

**Estado:** Aceptada para el alcance confirmado por el usuario; validación real pendiente
**Fecha:** 2026-11-13

## Decisión

Una persona no puede ser chofer activo y responsable empresarial activo al mismo
tiempo. El usuario confirmó ambos sentidos y el tratamiento no destructivo de
combinaciones existentes. Esta decisión reemplaza únicamente la compatibilidad
CHOFER/responsable admitida en ADR-0014 y contratos; no sustituye ADR-0015.

- Retirar la tarjeta y formulario de solicitud de responsabilidad de Mi cuenta
  del chofer. Mantener Mi cuenta, Mis guías y sus derechos propios de conductor.
- Rechazar solicitudes de vínculo/alta empresarial, aprobaciones y transferencias
  cuyo destinatario sea chofer activo según los datos persistidos por cédula.
  No confiar en roles del JWT, que pueden ser anteriores al alta del chofer.
- Rechazar alta activa, modificación activa (incluido cambio de cédula) y
  reactivación de chofer si el destinatario tiene RESPONSABLE/ACTIVO, incluso
  en una empresa inactiva. También se aplica al administrador que registra al
  destinatario; su permiso administrativo no elimina esta regla de negocio.
- Conservar vínculos, estados e historial heredados, pero denegar su ejercicio
  como responsable con 403. No impedir derechos propios de chofer ni funciones
  oficiales. Resolver mediante baja de chofer o transferencia administrativa
  explícita de responsabilidad, sin revocación automática.
- Conservar registros inactivos, consultas de estado/historial, rutas, DTO,
  roles declarativos y el sobre `{"error":"mensaje"}`. Los conflictos de
  habilitación/solicitud conservan NegocioException (400). No modificar D0-04.

## Implementación y concurrencia

Extender la guardia interna compartida `CompatibilidadFuncionarioEmpresa`, sin
renombrarla ni introducir una interfaz pública. Reutilizar sus controles de
ejercicio desde los servicios que ya los invocan.

Ambos sentidos toman un advisory lock transaccional PostgreSQL por cédula antes
de consultar el estado opuesto y antes de modificar entidades. La clave usa un
espacio reservado `0x43485253` y el `String.hashCode()` de la cédula persistida
(trim en entradas del ABM). Una colisión solo serializa personas adicionales;
no omite la validación. No se usa un bloqueo global ni se cambia aislamiento.
El bloqueo de fila Usuario existente se conserva en el circuito de responsabilidad.
La habilitación de chofer solo toma el advisory lock, no espera esa fila Usuario.
Las mutaciones de un chofer existente toman primero PESSIMISTIC_WRITE de su fila:
la cédula y actividad usadas al reactivar no pueden quedar obsoletas por un PUT
concurrente. No se bloquea esa fila desde el circuito de responsabilidad.

Esto cubre también el alta de chofer antes de su primer login, cuando aún no hay
fila Usuario que bloquear. La exclusión depende de que todos los escritores
pasen por estos servicios y de la transacción JTA/visibilidad READ COMMITTED;
requiere prueba integrada, no queda acreditada por dobles.
La liberación del lock al terminar la transacción está documentada en
[PostgreSQL](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADVISORY-LOCKS).

## Alternativas y consecuencias

- Solo ocultar la tarjeta: permite eludir la regla llamando directamente a REST/JSF.
- Solo comprobar roles o hacer un count sin exclusión: admite tokens desactualizados
  y carreras entre habilitación de chofer y aprobación de responsabilidad.
- Bloquear únicamente Usuario: no protege el registro previo al primer login.
- Revocar/borrar automáticamente: altera datos e historial sin decisión administrativa.

Se acepta el acoplamiento a PostgreSQL ya usado por el central, sin migración ni
nueva dependencia. La matriz de [validación](../../pruebas/incompatibilidad-chofer-responsable.md)
separa pruebas unitarias escritas de seguridad, SQL, locks y rollback pendientes.
