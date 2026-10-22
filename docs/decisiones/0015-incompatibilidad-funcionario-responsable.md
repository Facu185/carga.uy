# ADR-0015 — Funcionario activo y responsabilidad empresarial incompatibles

**Estado:** Aceptada para este cambio por confirmación explícita del usuario
**Fecha:** 2026-10-22

## Contexto

El ingreso OIDC asigna CIUDADANO como rol base y agrega FUNCIONARIO cuando la
persona integra el padrón activo. Por eso, permitir una operación a CIUDADANO
no excluye funcionarios. Ocultar tarjetas de Mi cuenta no protege la API ni
impide combinaciones de datos preexistentes.

El usuario confirmó la incompatibilidad entre funcionario **activo** y vínculo
RESPONSABLE/**ACTIVO**, conservando CIUDADANO y sin ampliar la regla a CHOFER.
La aceptación corresponde a este alcance solicitado, no a una aprobación externa
de los frentes P1/P2 ni a una interpretación de normativa laboral.

## Decisión

- Conservar CIUDADANO, el ingreso gub.uy y los roles acumulativos. La decisión
  afecta a responsabilidad empresarial, no a la condición básica de ciudadano.
- Un funcionario activo no puede solicitar responsabilidad, ni obtenerla por
  aprobación de vínculo, transferencia o aprobación de alta empresarial (que
  crea un responsable). Validar a la persona destinataria, no solo al aprobador.
- Un responsable activo no puede ser dado de alta ni reactivado como funcionario,
  tanto por alta manual como por aprobación de solicitud de acceso. Pedir acceso
  no habilita el rol: esa solicitud puede quedar pendiente hasta resolver el
  vínculo; la aprobación vuelve a comprobar la incompatibilidad.
- Consultar los estados persistidos por identidad del servidor, sin confiar en
  roles del JWT ni en una lista de vínculos paginada/truncada. La incompatibilidad
  no depende de si la empresa sigue activa: importa el estado del vínculo.
- Para impedir altas/aprobaciones concurrentes incompatibles, tomar un bloqueo
  JPA PESSIMISTIC_WRITE de la misma fila Usuario antes de comprobar el estado
  opuesto y mantenerlo durante la transacción EJB. No usar bloqueos globales ni
  cambiar aislamiento. Esto requiere verificación real en WildFly/PostgreSQL.
- Si existen ambos estados activos, bloquear las operaciones **como responsable**
  (perfil propio, ABM de vehículos/choferes, creación/catálogos de guías y listado
  empresarial), sin revocar vínculos, borrar historial ni retirar funciones
  oficiales. Resolver administrativamente por los circuitos existentes de baja
  de funcionario o transferencia de responsabilidad. Se mantienen consultas de
  estado/historial de solicitudes y vínculos.
- No alterar el permiso ACTIVO sin tipo de creación/catálogos de guías (D0-04):
  la nueva prohibición se aplica a la combinación funcionario/responsable activa,
  sin convertir silenciosamente OPERADOR en RESPONSABLE ni eliminar su permiso.
- Conservar el ABM administrativo del administrador y del funcionario. La
  prohibición de ejercer responsabilidad no impide actuar por un permiso oficial.
- Mantener rutas, DTO, anotaciones de roles y sobre REST existentes. Conflictos
  de alta/aprobación: NegocioException (400). Ejercicio incompatible: denegación
  de acceso (403). No introducir migración ni revocación masiva de datos.

## Alternativas y consecuencias

- **Quitar CIUDADANO a funcionarios:** cambia el significado del rol base y puede
  romper accesos compartidos; no resuelve vínculos históricos. Se descarta.
- **Revocar responsabilidades automáticamente:** evita intervención, pero cambia
  vínculos de forma sorpresiva. Se descarta a favor de rechazar la operación y
  exigir resolución administrativa explícita, conservando auditoría e historial.
- **Solo validaciones previas sin bloqueo:** permiten que habilitación y aprobación
  concurrentes lean ambas un estado compatible. Se usa exclusión por persona.
- La política se concentra en un componente interno CDI reutilizado por P1/P2;
  no se añade interfaz pública, framework ni una nueva capa de persistencia.
- CHOFER conserva sus permisos actuales y su posible incompatibilidad con
  FUNCIONARIO requiere otro acuerdo. No confundir el ABM de choferes ejercido
  **como responsable** con los derechos del chofer sobre sus propias guías.
- Pruebas primero; sin builds. Los dobles verifican decisiones y ausencia de
  efectos previos al rechazo, no interceptores EJB, JPQL, locks o rollback reales.

## Validación pendiente

Regresiones unitarias y [matriz de integración](../../pruebas/incompatibilidad-funcionario-responsable.md).
La ejecución en contenedor y las carreras deben validarse por el equipo antes de
atribuir cierre funcional o seguridad de concurrencia a esta implementación.
