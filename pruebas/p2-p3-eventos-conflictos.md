# ADR-0019 — Conflictos e inicio/baja P2/P3

Política central aprobada por el usuario el 2026-10-31 en
[ADR-0019](../docs/decisiones/0019-registro-de-eventos-de-viaje.md).
Pruebas unitarias y escenarios definidos antes del cambio productivo.
**Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
No se acredita acuerdo individual de Federico ni comportamiento de un móvil cuyo
código no está en este repositorio. La coordinación compartida sigue pendiente.

## Regresiones unitarias (sin ejecución)

- `EventoViajeBeanTest`: interacción de locks Vehículo → Guía, relectura de baja
  y asignación, vehículo ausente, INICIO offline con baja actual, reenvío idéntico
  tras baja/finalización y cambios de instante/detalle/coordenadas/foto con mismo ID.
- Normalización del detalle, offsets equivalentes y microsegundos de PostgreSQL.
- Inserción atómica con otro ganador idéntico/diferente; no aplicar una transición
  ni emitir FIN si no se insertó un evento nuevo. Fallos de inserción/aviso propagados.
- `EventoViajeDAOTest`: SQL parametrizado, `ON CONFLICT (id_evento) DO NOTHING`,
  casts para nulos y resultado insertado/no insertado; no ejecuta la consulta.
- Conservar pruebas P2 de rechazo `EN_CURSO` por DELETE y PUT y de autorización.

Se conservan los 11 métodos de prueba existentes de `EventoViajeBeanTest` y se
añaden 17 allí más 2 en `EventoViajeDAOTest`: **19 regresiones nuevas escritas**,
ninguna ejecutada. El caso de asignación inicial nula que cambia al releer tampoco
puede iniciar sin haber tomado la exclusión del vehículo.

Los dobles simulan relecturas/colisiones secuenciales: NO son carreras reales,
no prueban el aislamiento, el tipo JDBC de los nulos ni el rollback JTA/JMS.

## Comprobaciones estáticas realizadas

- `git diff --check` correcto. En el conjunto acumulado de 26 archivos (incluye
  la entrega C14-P anterior) se comprobaron formato LF/salto final y 206 enlaces
  locales con fragmentos; 7 archivos Java con delimitadores balanceados, 56 imports
  propios localizados y los 9 parámetros vinculados de la inserción nativa.
- Revisión de consumidor REST, roles CHOFER intactos, firmas y usos del DAO; no
  quedan llamadas al método `guardar` sustituido por la inserción atómica.
- Sin builds, compilación, ejecución SQL, aplicación, HTTP ni pruebas automatizadas
  ejecutadas. Estas comprobaciones no validan tipos Java ni seguridad real.

## Integración pendiente — WildFly, PostgreSQL y broker reales

Preparar fixtures sintéticos y transacciones independientes. Coordinar ambos
pedidos con barreras observables del entorno de pruebas, no con esperas arbitrarias.
Registrar versión, identidad por rol, transacciones, respuesta, estado final y
efectos persistidos/JMS, sin tokens, fotos ni datos personales en la evidencia.

| Escenario | Resultado a acreditar |
| --- | --- |
| INICIO confirma antes que DELETE de baja | Evento único, guía EN_CURSO y vehículo habilitado; baja rechazada con error de negocio vigente P2 (400). |
| DELETE confirma antes que INICIO | Vehículo de baja, guía sin iniciar, INICIO 409 y ningún evento/aviso nuevo. |
| Mismas dos carreras con PUT `habilitado=false` | Mismas invariantes; no eludir la regla por otra operación. |
| Baja o inicio aborta tras obtener exclusión | El contendiente relee el estado confirmado; sin cambios parciales. |
| Vehículo distinto | No introducir un bloqueo global ni mezclar empresas/guías. |
| Cambio de asignación durante la espera | Rechazar el intento tras releer; no tomar otro vehículo fuera del orden acordado. |
| Dos reenvíos iguales de una guía | Una inserción/transición; 201 y 200; FIN emite un solo aviso. |
| Mismo ID simultáneo en guías/vehículos distintos | Índice global decide ganador; perdedor 409 sin transición, aviso ni 500 por unicidad. |
| Mismo ID y contenido distinto | 409 sin sobrescribir original, incluidos foto y ubicación. |
| Foto/ubicación/detalle nulos o presentes | SQL ejecutable y JSON consistente; bytes privados no aparecen en historial/respuestas. |
| Timestamp con fracción de nueve dígitos y reenvío equivalente | Persistencia y comparación estables a microsegundos; caracterizar también datos anteriores a la normalización. |
| FIN y fallo al publicar JMS o después de publicar antes del commit | Rollback de inserción, estado y mensaje; el reintento no encuentra un FIN parcialmente confirmado. |
| Chofer ajeno, sesión vencida y funcionario sin CHOFER | 403/401 según contrato existente; no habilitar escritura ni lectura de historial al funcionario por este cambio. |
| Fin → aviso → Balanzas → consulta C14-P | Integración real y estado degradado con nodo caído; no confundir períodos previstos con instantes reales. |

## Obligaciones móviles pendientes (Federico)

Cola persistente por guía, envío ordenado, mismo ID/cuerpo al reintentar y
confirmación 200/201. Un conflicto de negocio conserva el evento y motivo visibles;
no borrarlo ni reintentar indefinidamente. Un fallo temporal conserva pendientes
y usa reintentos limitados, sin tratar 401/403 como indisponibilidad temporal.

Caso de aceptación explícito: INICIO capturado a las 10:00, baja confirmada a las
11:00 y sincronización a las 12:00 → INICIO 409, sin reconstruir habilitación pasada.
Esta política no autoriza al celular a reactivar el vehículo ni declara que el
evento físico no ocurrió; la evidencia local debe conservarse.

## Relación con fase 5

C14-P ya tiene consumidor local, pendiente de validación. C14-E de lectura para
funcionario sigue pendiente de entrega/acuerdo con Federico; resolver este ADR
no amplía el GET de historial, no implementa esa pantalla ni cierra F14.
