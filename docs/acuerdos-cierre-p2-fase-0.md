# Registro de acuerdos — Cierre P2, fase 0

**Fecha:** 2026-10-22. **Base original inspeccionada:** `c3bc924`.
**Actualización C13:** `da10af0` más cambios locales previos y de fase 4.
**Estado:** cierre parcial; SPA, F10/C10 y F13/C13 confirmados por el usuario.
F10, F12 y F13 cuentan con implementación local, sin validación ejecutada ni
aceptación compartida. F14/C14 y las demás políticas indicadas como
propuesta/pendiente siguen sin aprobación.

Este registro ejecuta la [fase 0](plan-cierre-p2.md#fase-0--resolver-decisiones-que-condicionan-la-implementación).
No sustituye [contratos publicados](contratos.md), [frentes](frentes.md) ni ADR.
El usuario confirmó la recomendación de interfaz y posteriormente el alcance
D0-05/C10 de fase 2 y C13 de fase 4; no se registran
aprobaciones ni compromisos de P1/P3/P4/P5 que no se hayan recibido.

## 1. Acuerdos y bloqueos por tramo

| ID | Decisión o entrega | Estado y responsable | Condición de salida |
| --- | --- | --- | --- |
| D0-01 | P2 implementa área F12–F14; F04 permanece P5. | Confirmado por el usuario; límite registrado. | Una integración de casos requiere acuerdo separado con P5. |
| D0-02 | Ampliar SPA; conservar JSF e identidad P1. | Confirmado por el usuario; [ADR-0013](decisiones/0013-area-funcionario-spa-p2.md). | Elección de tecnología cerrada; navegación compartida aún por coordinar. |
| D0-03 | Principal → vínculo empresarial, no `usuario.empresa_id`. | ADR-0005 alineado documentalmente con código/contrato. | No se cambia ningún permiso; el criterio de tipo sigue en D0-04. |
| D0-04 | Tipo de vínculo para crear guías y consultar sus catálogos. | Pendiente P1/P2. | Aceptar una de las alternativas de §3 y registrar ADR/contrato antes de cambiar lógica. |
| D0-05 | Perfil F10: campos, pertenencia y operación específica. | Confirmado por el usuario; [ADR-0014](decisiones/0014-perfil-propio-empresa.md) y C10 publicados. | Implementación local preparada; coordinar despliegue compartido P1/P3 y acreditar ejecución. |
| D0-06 | Contratos y matriz actor/operación F10/F13/F14. | F10/C10 y F13/C13 aprobados por el usuario y publicados; F14/C14 sigue como borrador. | Resolver C14 antes de su consumidor y acreditar aceptación integrada; no se cierra el conjunto por aprobar C13. |
| D0-07 | Edición de guía y conflictos offline. | Pendiente P2/P3/móvil. | Confirmar si hay cambios permitidos, campos/estados, versión y tratamiento de eventos tardíos/repetidos; ADR antes de implementar. |
| D0-08 | Estado/historial, inicio/baja y aviso de fin. | Dependencias por confirmar con P3; adaptador P4 existente. | P3 confirma contrato/entrega, exclusión compartida y llamada a P4; no hay compromiso acreditado. |
| D0-09 | Pesajes asociados a una guía, varios días y disponibilidad. | Pendiente P2/P4. | Acordar consulta/autorización, paginación y metadatos; conservar significado de `guiaId`. |
| D0-10 | F15 y eventual interfaz F04. | F15 sin dueño confirmado; F04 sigue P5. | Registrar reparto y aceptación; no incluir desarrollo P2 por omisión. |

**Desbloqueo por bloque:** fase 1 puede preparar/registrar validación de la base
existente sin esperar offline. Fase 2 requiere D0-05/C10; fase 3 requiere C12 y
coordinación de navegación P1/P3; fase 4 requiere C13; fase 5 requiere C14 y los
contratos de sus fuentes. Fase 6 requiere D0-04 si cambia permisos y D0-07–09.
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
| Consultar guías propias del chofer | CHOFER | Chofer resuelto por cédula del principal, no ID del cliente. |
| Listar pesajes guardados / sincronizar manualmente | FUNCIONARIO, ADMINISTRADOR | Recurso P4 protegido; matrícula/fecha. La UI F14 no debe agregar sincronización manual como requisito. |

Fuentes:
[EmpresaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/EmpresaServicioBean.java),
[VehiculoServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/VehiculoServicioBean.java),
[GuiaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/GuiaServicioBean.java),
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

**C12 existente**, **C10 aprobado** y **C13 aprobado por el usuario** se documentan
en contratos.md. C13 queda registrado en [ADR-0016](decisiones/0016-consulta-vehicular-funcionario.md).
**C14 sigue como borrador para acuerdo, NO ruta ni interfaz disponible.**
Sus firmas EJB/DTO definitivas se publicarán antes de implementar su consumidor.

| ID | Productor / responsable | Consumidor y coordinación | Frontera y estado |
| --- | --- | --- | --- |
| C01 | P1: identidad, perfil y vínculos | SPA P2, navegación P3 | Entrada local añadida en Mi cuenta para FUNCIONARIO → `/funcionario/empresas`, conservando OIDC web → `/ingreso` → `/mi-cuenta`, Bearer y callback. Aceptación compartida P1/P3 pendiente; no se atribuye aprobación externa por añadir el enlace. |
| C10 / F10 | P2: `PerfilEmpresaServicioLocal` / EJB / REST | Mi empresa P2, integración compartida P3; P1 para vínculo | Aprobado por el usuario y publicado: GET/PUT `/carga/api/empresas/{id}/perfil`, 200 `EmpresaDTO`; PUT `EditarPerfilEmpresaDTO` limitado a §4. Inactiva/inexistente/sin permiso: 403. Implementación local preparada, sin ejecución acreditada. |
| C12 / F12 | P2: `EmpresaServicio` / `EmpresaRecurso` | Área SPA P2; coordinar alta/solicitudes con P1 | Existentes GET con `nombre`, POST 201, PUT `/{id}` 200, DELETE `/{id}` 204; DTO `EmpresaDTO`. Consumidor local `/funcionario/empresas` integra GET/PUT/DELETE, sin GET por ID, paginación ni POST de alta nuevo. Bandeja responsable P1 reutilizada, distinta del alta empresarial; coordinación y ejecución pendientes. |
| C13 / F13 | P2: `ConsultaVehiculosLocal` / EJB / REST | Área SPA P2; aceptación integrada con P1/P3 pendiente | Aprobado por el usuario: GET `/carga/api/vehiculos/consulta` solo FUNCIONARIO/ADMINISTRADOR, sin vínculo ni mutaciones; `empresaId` obligatorio, matrícula parcial literal sin caso, página/tamaño. Incluye bajas y empresas inactivas; DTO con fecha servidor America/Montevideo y vigencias inclusivas separadas de baja. Implementación local, sin ejecución acreditada. |
| C14 / F14 | P2: nueva consulta de guías | Área SPA P2; fuentes P3/P4 | Propuesto GET `/carga/api/guias/consulta` para FUNCIONARIO/ADMINISTRADOR, `empresaId` obligatorio, período previsto y estado opcionales, página/tamaño. Detalle/asignaciones y DTO por acordar; no modificar `/guias/empresa`. |
| C14-E | P3: estado e historial reales | Composición F14 P2 | Contrato de lectura, transiciones, orden y campos pendientes de entrega/acuerdo. Separar previsto, persistido e historial; no reconstruir eventos. |
| C14-P | P4: pesajes/recorridos | Composición F14 P2 | GET diario existente devuelve `List<PesadaRegistradaDTO>`; consulta por guía y metadatos por acordar. Nunca asociar por matrícula/día en el navegador ni atribuir `guiaId=null` a un viaje. |
| CFIN | P4: `AvisoFinDeViajeLocal`; P3 lo invoca | P3 → adaptador P4; P2 integra consulta | Contrato existente ADR-0010; integración del emisor P3 pendiente de acreditar, no se da por entregada. |

Para C13 se aprobaron página base 0, tamaño predeterminado 20, rango 1–100,
desplazamiento dentro de entero de 32 bits y orden matrícula/id; la respuesta
agrega fechaReferencia/zonaHoraria al sobre paginado. Detalle en
[C13 publicado](contratos.md#consulta-vehicular-f13--c13).
Para C14 se propone `{registros, pagina, tamano, total}` con esos límites y orden
inicio/id descendentes; confirmar límites de período y selección de estados antes de crear consultas.
No deducir estado de la fecha prevista ni descartar históricos sin asignación.

Errores aprobados para C13 y propuestos para C14: `400` para datos/filtros/paginación inválidos
y negocio; `401` sin sesión válida, `403` para rol o
pertenencia insuficiente. Mantener `{"error":"mensaje"}`, correlación y fechas
ISO-8601, incluida la excepción actual de `EmpresaDTO.fechaAlta`.
Para C13, empresa inexistente responde 400 tras autorización por rol; C14 aún
requiere cerrar recurso inexistente y orden autorización/existencia antes de publicar.
No introducir `404/409/422` ni el nuevo
sobre del estándar en contratos vigentes por efecto de este borrador.

## 6. Coordinaciones pendientes y evidencia exigida

| Dueño | Confirmación solicitada, aún no recibida | Aceptación que permitirá acreditar |
| --- | --- | --- |
| P1/P2 | D0-04 y recorrido C12 sin duplicar solicitudes; integración de C10 aprobado por el usuario. | Identidades sintéticas por rol/vínculo, rechazo directo REST y ausencia de mutaciones ajenas. |
| P1/P3 | Entrada al área operativa desde gub.uy y navegación compartida. | Sesión válida/vencida, 401/403 y regresión de accesos P1. |
| P3/móvil | Si hay edición de guía, campos/estados, política offline y contrato C14-E. | Eventos duplicados/tardíos y conflictos con resultados acordados; no añadir edición/cancelación implícita. |
| P2/P3 | Inicio toma exclusión del mismo vehículo, revalida habilitación y cambia estado en JTA; acordar orden de locks. | Carreras reales inicio vs DELETE/PUT, rollback y vehículo distinto; mocks no bastan. |
| P3/P4 | Emisión CFIN y consulta C14-P por guía, varios días, paginación/disponibilidad. | Fin → aviso → pesajes → consulta, nodo caído y guías sucesivas con asociación `[inicio, fin)`. |
| Equipo/P5 | Dueño F15 y alcance de eventual integración de casos F04. | Reparto registrado, datos públicos delimitados; bloque P2 solo si se le asigna. |

La fase 1 conserva las matrices y procedimientos existentes. El registro original
de fase 0 fue documental. La posterior fase 2 implementa F10 y añade pruebas
descritas en [aceptación del perfil](../pruebas/p2-perfil-empresa.md), sin ejecutar
aplicación, builds ni tests ni declarar seguridad/concurrencia/integración verificadas.
