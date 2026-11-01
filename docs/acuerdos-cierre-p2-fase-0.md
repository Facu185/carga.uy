# Registro de acuerdos — Cierre P2, fase 0

**Fecha:** 2026-10-22. **Base original inspeccionada:** `c3bc924`.
**Actualización C13:** `da10af0` más cambios locales previos y de fase 4.
**Actualización C14:** 2026-10-27, contrato y entrega parcial P2 confirmados por el
usuario en [ADR-0018](decisiones/0018-consulta-viajes-funcionario-p2.md).
**Actualización de responsabilidades:** 2026-10-31, reparto comunicado por Roibeth
y contraste estático sobre `a62ceff`; detalle en §7.
**Estado:** cierre parcial; SPA, F10/C10, F13/C13 y C14 P2 confirmados por el usuario.
F10, F12, F13 y la parte P2 de F14 cuentan con implementación local, sin validación
ejecutada ni aceptación compartida. C14-P ya está publicado e implementado por P4;
su consumidor en el detalle SPA se implementó localmente por solicitud del usuario,
sin validación ejecutada ni aceptación conjunta. C14-E para
funcionario y las demás políticas indicadas como propuesta/pendiente no quedan
aprobados por el reparto. Posteriormente el usuario aprobó aplicar la recomendación
de ADR-0019: política central e inicio/baja implementados localmente, con coordinación
P3/móvil y validación real aún pendientes.

Este registro ejecuta la [fase 0](plan-cierre-p2.md#fase-0--resolver-decisiones-que-condicionan-la-implementación).
No sustituye [contratos publicados](contratos.md), [frentes](frentes.md) ni ADR.
El usuario confirmó la recomendación de interfaz y posteriormente el alcance
D0-05/C10 de fase 2, C13 de fase 4 y C14 limitado a P2 de fase 5; no se registran
aprobaciones ni compromisos de P1/P3/P4/P5 que no se hayan recibido.

## 1. Acuerdos y bloqueos por tramo

| ID | Decisión o entrega | Estado y responsable | Condición de salida |
| --- | --- | --- | --- |
| D0-01 | P2 implementa área F12–F14; F04 permanece P5. | Confirmado por el usuario; límite registrado. | Una integración de casos requiere acuerdo separado con P5. |
| D0-02 | Ampliar SPA; conservar JSF e identidad P1. | Confirmado por el usuario; [ADR-0013](decisiones/0013-area-funcionario-spa-p2.md). | Elección de tecnología cerrada; navegación compartida aún por coordinar. |
| D0-03 | Principal → vínculo empresarial, no `usuario.empresa_id`. | ADR-0005 alineado documentalmente con código/contrato. | No se cambia ningún permiso; el criterio de tipo sigue en D0-04. |
| D0-04 | Tipo de vínculo para crear guías y consultar sus catálogos. | Pendiente P1/P2. | Aceptar una de las alternativas de §3 y registrar ADR/contrato antes de cambiar lógica. |
| D0-05 | Perfil F10: campos, pertenencia y operación específica. | Confirmado por el usuario; [ADR-0014](decisiones/0014-perfil-propio-empresa.md) y C10 publicados. | Implementación local preparada; coordinar despliegue compartido P1/P3 y acreditar ejecución. |
| D0-06 | Contratos y matriz actor/operación F10/F13/F14. | F10/C10, F13/C13 y C14 P2 aprobados por el usuario y publicados; C14-P publicado e implementado, C14-E para funcionario pendiente. | Federico entrega la lectura autorizada del historial; Roibeth integra historial y pesajes. Confirmar aceptación de C14-P y acreditar integración; la entrega P2 no cierra F14. |
| D0-07 | Edición de guía y conflictos offline. | Política central de ADR-0019 aceptada por el usuario e implementada localmente; Federico con Roibeth coordinan entrega/aceptación. | Acreditar móvil con cola ordenada y conflictos visibles, duplicados y rechazo de INICIO con baja actual. Edición/cancelación, campos y versionado de guía no quedan autorizados. |
| D0-08 | Estado/historial, inicio/baja y aviso de fin. | P3 registra eventos e invoca P4 al FIN; inicio local usa Vehículo → Guía y revalida baja/asignación. Federico conserva la entrega de lectura C14-E. | Publicar C14-E para funcionario y acreditar roles, carreras reales DELETE/PUT, rollback y fin → aviso → pesajes. La aprobación del usuario no sustituye aceptación compartida. |
| D0-09 | Pesajes asociados a una guía, varios días y disponibilidad. | P4 entrega C14-P por guía; consumidor de Roibeth implementado localmente tras solicitar aplicar la recomendación. | Se consume el contrato vigente: tope de 500 sin paginación y metadatos, carga/vacío/error/parcialidad y `guiaId` conservado. Acreditar consulta y aceptación compartida; no se declara validado. |
| D0-10 | F15 y eventual interfaz F04. | F15 sin dueño confirmado; F04 sigue P5. | Registrar reparto y aceptación; no incluir desarrollo P2 por omisión. |

**Desbloqueo por bloque:** fase 1 puede preparar/registrar validación de la base
existente sin esperar offline. Fase 2 requiere D0-05/C10; fase 3 requiere C12 y
coordinación de navegación P1/P3; fase 4 requiere C13; fase 5 P2 cuenta con C14
aprobado y consumidor C14-P local con los límites publicados, autorizado por el usuario;
la lectura C14-E para funcionario requiere acuerdo y entrega de P3. Fase 6 requiere
D0-04 si cambia permisos y D0-07–09.
Cerrar un bloque no significa cerrar P2 integrado.

## 2. Matriz actor/operación: implementación observada

Lectura estática de anotaciones y controles EJB; NO prueba de autorización en
WildFly. Roles acumulados pueden habilitar otros recorridos: los rechazos al
funcionario de esta tabla se refieren a una identidad con **solo** ese rol.

| Operación | Actor/rol admitido por el código | Pertenencia y límite observado |
| --- | --- | --- |
| Listar/buscar empresas | CIUDADANO, CHOFER, FUNCIONARIO, AUTORIDAD, ADMINISTRADOR | Listado general por nombre opcional; sin filtro de vínculo ni paginación. No confundir con consulta de perfil propio. |
| Alta/modificación/baja administrativa de empresa | FUNCIONARIO, ADMINISTRADOR | `EmpresaServicioBean`; no habilita al responsable ciudadano. Baja desactiva, revoca vínculos activos y registra auditoría; rollback real pendiente. |
| Consultar/editar perfil propio F10 | CIUDADANO, CHOFER | Nuevo contrato específico: RESPONSABLE/ACTIVO y empresa activa; consulta/edición ajena, inactiva o inexistente denegada. No amplía `modificar` administrativo. |
| Listar/gestionar vehículos | ADMINISTRADOR, CIUDADANO, CHOFER | No administrador requiere RESPONSABLE/ACTIVO por empresa; administrador tiene excepción. FUNCIONARIO no puede leer ni mutar por este servicio actual. |
| Consulta vehicular F13/C13 | FUNCIONARIO, ADMINISTRADOR | Nueva operación solo lectura por empresa, sin vínculo; incluye inactivos/bajas y devuelve vigencias a fecha de servidor. No amplía permisos del ABM. |
| Crear guía | CIUDADANO | Vínculo ACTIVO, sin filtro de tipo, empresa activa y vehículo/chofer de esa empresa. |
| Catálogos de vehículos/choferes para guía | CIUDADANO | Vínculo ACTIVO, sin filtro de tipo; vehículos habilitados/choferes activos. No extender esta condición a todos los catálogos: rubros/tipos no filtran vínculo. |
| Listar guías de empresa | CIUDADANO, CHOFER | RESPONSABLE/ACTIVO de la empresa solicitada; no admite funcionario ni excepción administrativa. Paginación 0/20, tamaño 1–100. |
| Consultar guías C14 (listado/detalle) | FUNCIONARIO, ADMINISTRADOR | Lectura separada sin vínculo por empresa; incluye inactivas e históricos sin asignaciones. No amplía creación ni listado empresarial. |
| Consultar guías propias del chofer | CHOFER | Chofer resuelto por cédula del principal, no ID del cliente. |
| Consultar historial de eventos P3 | CHOFER | GET actual restringido a la guía asignada al chofer autenticado; no habilita al funcionario. La lectura C14-E para F14 está pendiente. |
| Listar pesajes guardados / sincronizar manualmente | FUNCIONARIO, ADMINISTRADOR | Recurso P4 protegido; matrícula/fecha. La UI F14 no debe agregar sincronización manual como requisito. |
| Consultar pesajes por guía C14-P | FUNCIONARIO, ADMINISTRADOR | Solo lee pesadas ya vinculadas a esa guía, de todos los días, hasta 500 y con metadatos. No sincroniza ni incluye `guiaId=null`. |

Fuentes:
[EmpresaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/EmpresaServicioBean.java),
[VehiculoServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/VehiculoServicioBean.java),
[GuiaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/GuiaServicioBean.java),
[ConsultaGuiasBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/ConsultaGuiasBean.java),
[EventoViajeBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/EventoViajeBean.java),
[VinculoEmpresaUsuarioDAO](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/persistencia/VinculoEmpresaUsuarioDAO.java),
[PesajeRecurso](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/PesajeRecurso.java).

## 3. D0-04: decisión de permisos todavía necesaria

- **Recomendada:** exigir RESPONSABLE/ACTIVO también al crear guías y consultar
  catálogos empresariales. Alinea CU2 con listado y ABM; restringe usuarios hoy
  admitidos, por lo que requiere acuerdo P1/P2, análisis de consumidores y regresión.
- **Alternativa:** conservar ACTIVO para crear/catálogos y RESPONSABLE/ACTIVO para
  gestionar/listar. Evita cambiar consumidores actuales, pero obliga a justificar
  qué operaciones tiene cada tipo de vínculo y aceptar la asimetría explícitamente.

Ninguna se aplica en esta fase. El cambio documental de ADR-0005 no elige una.

## 4. D0-05: perfil F10 aprobado por el usuario

- Roles de entrada aprobados: CIUDADANO o CHOFER con vínculo RESPONSABLE/ACTIVO
  comprobado en el EJB para la empresa; no basta rol o ID del navegador.
- Mantener el recorrido de una empresa responsable activa de Mi empresa. Ante
  ausencia o ambigüedad no elegir la primera ni cambiar el modelo global P1.
- Campos editables aprobados: `nombre`, `razonSocial`, `numeroEmpresa`,
  `direccion`. Nombre obligatorio y número positivo/único; reutilizar validaciones
  existentes. No imponer nuevas obligatoriedades a razón social/dirección sin acuerdo.
- Solo lectura/no mutables por esta operación: `id`, `fechaAlta`, `activa`,
  vínculos y bajas. Un payload manipulado nunca debe modificarlos.
- Usar DTO de entrada específico con esos cuatro campos y una operación de
  perfil frente a ampliar el ABM administrativo. Costo: contrato y pruebas nuevos;
  beneficio: evita exponer campos administrativos por asignación indiscriminada.
- Empresa ajena o vínculo revocado/no responsable: denegar sin mutación.
  Se confirmó denegar consulta y edición de empresas inactivas o inexistentes
  con 403 indistinguible. ID no positivo/datos inválidos: 400; sin sesión: 401.
  Detalle contractual en [C10 publicado](contratos.md#perfil-propio-f10--c10).

## 5. Registro de contratos, productores y consumidores

**C12 existente**, **C10**, **C13** y **C14 P2 aprobados por el usuario** se documentan
en contratos.md. C13 queda registrado en [ADR-0016](decisiones/0016-consulta-vehicular-funcionario.md)
y C14 en [ADR-0018](decisiones/0018-consulta-viajes-funcionario-p2.md).
**C14-E para funcionario sigue pendiente. C14-P ya está publicado e implementado**
en [Pesajes P4](contratos.md#pesajes-p4); el usuario solicitó aplicar la recomendación
y se implementó su consumidor SPA con el límite vigente. Aceptación conjunta y
validación en ejecución siguen pendientes. No se atribuye integración real acreditada.

| ID | Productor / responsable | Consumidor y coordinación | Frontera y estado |
| --- | --- | --- | --- |
| C01 | P1: identidad, perfil y vínculos | SPA P2, navegación P3 | Entrada local añadida en Mi cuenta para FUNCIONARIO → `/funcionario/empresas`, conservando OIDC web → `/ingreso` → `/mi-cuenta`, Bearer y callback. Aceptación compartida P1/P3 pendiente; no se atribuye aprobación externa por añadir el enlace. |
| C10 / F10 | P2: `PerfilEmpresaServicioLocal` / EJB / REST | Mi empresa P2, integración compartida P3; P1 para vínculo | Aprobado por el usuario y publicado: GET/PUT `/carga/api/empresas/{id}/perfil`, 200 `EmpresaDTO`; PUT `EditarPerfilEmpresaDTO` limitado a §4. Inactiva/inexistente/sin permiso: 403. Implementación local preparada, sin ejecución acreditada. |
| C12 / F12 | P2: `EmpresaServicio` / `EmpresaRecurso` | Área SPA P2; coordinar alta/solicitudes con P1 | Existentes GET con `nombre`, POST 201, PUT `/{id}` 200, DELETE `/{id}` 204; DTO `EmpresaDTO`. Consumidor local `/funcionario/empresas` integra GET/PUT/DELETE, sin GET por ID, paginación ni POST de alta nuevo. Bandeja responsable P1 reutilizada, distinta del alta empresarial; coordinación y ejecución pendientes. |
| C13 / F13 | P2: `ConsultaVehiculosLocal` / EJB / REST | Área SPA P2; aceptación integrada con P1/P3 pendiente | Aprobado por el usuario: GET `/carga/api/vehiculos/consulta` solo FUNCIONARIO/ADMINISTRADOR, sin vínculo ni mutaciones; `empresaId` obligatorio, matrícula parcial literal sin caso, página/tamaño. Incluye bajas y empresas inactivas; DTO con fecha servidor America/Montevideo y vigencias inclusivas separadas de baja. Implementación local, sin ejecución acreditada. |
| C14 / F14 | P2: `ConsultaGuiasLocal` / EJB / REST | Área SPA P2; consumidor C14-P local, C14-E pendiente | Aprobado por el usuario: GET `/carga/api/guias/consulta` y `/{id}` para FUNCIONARIO/ADMINISTRADOR sin vínculo, `empresaId` obligatorio; listado con solapamiento previsto, estado y paginación. DTO/listado/detalle publicados; `/guias/empresa` intacto. Implementación local y pruebas escritas, sin ejecución. |
| C14-E | P3 / Federico: estado e historial reales | Roibeth: composición F14 P2 | P3 ya actualiza el estado persistido y ofrece GET `/carga/api/eventos-viaje?guiaId=` solo para CHOFER y guía propia. Federico habilita y acuerda la lectura para funcionario; Roibeth la muestra. Separar previsto, persistido e historial; no reconstruir eventos ni ampliar escritura al abrir lectura. |
| C14-P | P4: pesajes por guía | Roibeth: composición F14 P2 | GET `/carga/api/pesajes/guia/{id}` y `PesajesDeGuiaDTO` publicados e implementados; FUNCIONARIO/ADMINISTRADOR, varios días, hasta 500 sin paginación, `truncado` y `nodoDisponible`. Consumidor SPA local con carga/vacío/error/parcialidad, sin sincronización manual y con validación pendiente. Nunca asociar por matrícula/día en el navegador ni atribuir `guiaId=null` a un viaje. |
| CFIN | P4: `AvisoFinDeViajeLocal`; P3 lo invoca | P3 → adaptador P4; P2 integra consulta | Contrato existente ADR-0010; `EventoViajeBean` ya invoca el adaptador al FIN. Presencia observada en código, sin acreditar transacción, broker ni cadena completa en ejecución. |

Para C13 se aprobaron página base 0, tamaño predeterminado 20, rango 1–100,
desplazamiento dentro de entero de 32 bits y orden matrícula/id; la respuesta
agrega fechaReferencia/zonaHoraria al sobre paginado. Detalle en
[C13 publicado](contratos.md#consulta-vehicular-f13--c13).
Para C14 se aprobó `{registros, pagina, tamano, total}` con esos límites y orden
inicio/id descendentes; período opcional como par de instantes ISO-8601 con zona,
solapamiento `[desde, hasta)` y estados persistidos exactos CREADA/ASIGNADA/EN_CURSO/FINALIZADA.
No deducir estado de la fecha prevista ni descartar históricos sin asignación.

Errores aprobados para C13 y C14: `400` para datos/filtros/paginación inválidos
y negocio; `401` sin sesión válida, `403` para rol o
pertenencia insuficiente. Mantener `{"error":"mensaje"}`, correlación y fechas
ISO-8601, incluida la excepción actual de `EmpresaDTO.fechaAlta`.
Para C13, empresa inexistente responde 400 tras autorización por rol; C14 aplica
el mismo orden, y responde 400 para empresa/guía inexistente o guía de otra empresa.
No introducir `404/409/422` ni el nuevo
sobre del estándar en contratos vigentes por efecto de este borrador.

## 6. Coordinaciones pendientes y evidencia exigida

| Dueño | Coordinación o entrega pendiente | Aceptación que permitirá acreditar |
| --- | --- | --- |
| P1/P2 | D0-04 y recorrido C12 sin duplicar solicitudes; integración de C10 aprobado por el usuario. | Identidades sintéticas por rol/vínculo, rechazo directo REST y ausencia de mutaciones ajenas. |
| P1/P3 | Entrada al área operativa desde gub.uy y navegación compartida. | Sesión válida/vencida, 401/403 y regresión de accesos P1. |
| Federico con Roibeth (P3/P2) | Coordinar entrega y aceptación de ADR-0019 aprobado por el usuario; conducta del móvil pendiente. Alcance de edición de guía sigue sin aprobar. | Eventos duplicados/tardíos y conflictos con resultados acordados; no añadir edición/cancelación implícita. |
| Federico con Roibeth (P3/P2) | Acreditar exclusión local Vehículo → Guía, relectura de baja/asignación y coherencia JTA; inicio nuevo con baja actual devuelve 409. | Carreras reales inicio vs DELETE/PUT, rollback y vehículo distinto; mocks no bastan. |
| Federico → Roibeth (P3/P2) | Entregar lectura C14-E autorizada para funcionario e integrarla en el detalle de viaje. | Historial real, orden/campos acordados y autorización directa REST, sin habilitar escritura al funcionario. |
| Roibeth con P4 y P3 | Validar el consumidor C14-P local y acreditar CFIN ya invocado en código; aceptar límites/disponibilidad en integración. | Fin → aviso → pesajes → consulta, nodo caído y guías sucesivas con asociación `[inicio, fin)`, sin confundir período previsto e instantes reales. |
| Equipo/P5 | Dueño F15 y alcance de eventual integración de casos F04. | Reparto registrado, datos públicos delimitados; bloque P2 solo si se le asigna. |

La fase 1 conserva las matrices y procedimientos existentes. El registro original
de fase 0 fue documental. La posterior fase 2 implementa F10 y añade pruebas
descritas en [aceptación del perfil](../pruebas/p2-perfil-empresa.md), sin ejecutar
aplicación, builds ni tests ni declarar seguridad/concurrencia/integración verificadas.

## 7. Reparto de responsabilidades comunicado el 2026-10-31

Fuente: imagen y listado compartidos por **Roibeth**, quien se identifica como
responsable de la interfaz P2. Se registra el reparto comunicado, no una aceptación
individual recibida de cada integrante ni una aprobación de políticas pendientes.
Las observaciones de código se distinguen de lo informado por el equipo; no se
certifican aquí el despliegue, las trazas móviles ni el estado de implementación de P5.

| Responsabilidad | Responsable según el reparto | Relación con el cierre P2 y condición pendiente |
| --- | --- | --- |
| Acordar política de conflictos e inicio con vehículo dado de baja. | Federico con Roibeth. | D0-07/D0-08 y fase 6. Tras este reparto, el usuario aprobó aplicar ADR-0019: central autoritativo, idempotencia estricta e inicio/baja con exclusión común implementados localmente. Coordinación P3/móvil y pruebas reales pendientes; no se atribuye aprobación individual a Federico. |
| Mostrar historial de eventos al funcionario en el detalle de viaje. | Federico habilita la lectura; Roibeth la muestra. | C14-E, fase 5. Acordar contrato y autorización; el GET actual solo permite al chofer consultar su guía. No ampliar los permisos de registro de eventos al abrir la lectura. |
| Mostrar pesajes en el detalle de viaje. | Roibeth. | C14-P, fase 5. Consumidor local implementado con carga, vacío, error y datos parciales, respetando `truncado` y los metadatos de disponibilidad. Validación y aceptación compartida pendientes. |
| Descargar la foto de un evento como evidencia. | Federico; consumidor previsto P5. | Entrega de evidencia P3/P5; no añade por sí sola una pantalla ni descarga obligatoria a P2. Acordar contrato y permisos antes de consumirla. |
| Iniciar la traza en el celular y enviar `traceparent` para el flujo móvil → central → nodo. | Federico; el reparto informa que central → Balanzas ya está trazado por P4. | Dependencia transversal de observabilidad, no implementación de la pantalla F14. Acreditar continuidad de la traza de extremo a extremo; el tramo existente no se validó en esta revisión. |
| Automatizar pruebas de idempotencia de la sincronización móvil. | Federico. | Entrega P3/móvil y evidencia para integración/aceptación; no sustituye las pruebas de consumo y permisos de F14 ni las carreras reales de fase 6. |
| Resolver ingreso real con gub.uy desde el celular. | Joaquín Poblete (P1), junto con el profesor. | Dependencia de identidad para aceptación integrada. Según lo informado, la redirect registrada usa `localhost` e ID Uruguay rechaza el cliente; confirmar registro y causa con P1/proveedor, sin atribuirla a P2 ni asumirla resuelta. |
| Habilitar HTTPS entre el móvil y el central. | Todo el equipo, con el despliegue en la nube. | Dependencia transversal de despliegue/seguridad. El reparto informa uso actual de `http://IP:8080`; acreditar el canal desplegado, no darlo por resuelto desde la interfaz P2. |
| Implementar reglas, casos y uso de fotos/pesajes como evidencia. | P5. | Fiscalización permanece P5; no se reasigna a Roibeth por integrar F14. El reparto informa falta de código P5, sin verificación de ese estado en esta actualización. |

**Orden de avance de Roibeth:** validar el consumidor local C14-P con los límites
del contrato publicado; consumir historial cuando Federico entregue C14-E autorizado; acordar
con Federico la aceptación y validación de ADR-0019 como bloque de fase 6. Las lecturas de fase 5
no necesitan esperar toda la política offline, pero tampoco cierran su integración.
Completar los escenarios, la validación real de roles/sesión/navegador y la aceptación
compartida sigue pendiente; mostrar datos no equivale a cerrar F14.

Referencias: [fase 5](plan-cierre-p2.md#fase-5--preparar-monitoreo-de-viajes-y-pesajes-f14-propiedad-de-interfaz-p2),
[fase 6](plan-cierre-p2.md#fase-6--integrar-ejecución-real-offline-y-concurrencia),
[ADR-0019](decisiones/0019-registro-de-eventos-de-viaje.md) y
[matriz C14 P2](../pruebas/p2-consulta-viajes.md).
