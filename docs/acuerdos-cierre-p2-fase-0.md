# Registro de acuerdos — Cierre P2, fase 0

**Fecha:** 2026-10-22. **Base de código inspeccionada:** `c3bc924`.
**Estado:** cierre parcial; elección de SPA confirmada, contratos nuevos y
políticas indicados como propuesta/pendiente, sin implementación productiva.

Este registro ejecuta la [fase 0](plan-cierre-p2.md#fase-0--resolver-decisiones-que-condicionan-la-implementación).
No sustituye [contratos publicados](contratos.md), [frentes](frentes.md) ni ADR.
La solicitud del usuario confirma la recomendación de interfaz; no se registran
aprobaciones ni compromisos de P1/P3/P4/P5 que no se hayan recibido.

## 1. Acuerdos y bloqueos por tramo

| ID | Decisión o entrega | Estado y responsable | Condición de salida |
| --- | --- | --- | --- |
| D0-01 | P2 implementa área F12–F14; F04 permanece P5. | Confirmado por el usuario; límite registrado. | Una integración de casos requiere acuerdo separado con P5. |
| D0-02 | Ampliar SPA; conservar JSF e identidad P1. | Confirmado por el usuario; [ADR-0013](decisiones/0013-area-funcionario-spa-p2.md). | Elección de tecnología cerrada; navegación compartida aún por coordinar. |
| D0-03 | Principal → vínculo empresarial, no `usuario.empresa_id`. | ADR-0005 alineado documentalmente con código/contrato. | No se cambia ningún permiso; el criterio de tipo sigue en D0-04. |
| D0-04 | Tipo de vínculo para crear guías y consultar sus catálogos. | Pendiente P1/P2. | Aceptar una de las alternativas de §3 y registrar ADR/contrato antes de cambiar lógica. |
| D0-05 | Perfil F10: campos, pertenencia y operación específica. | Propuesta P2; confirmar con responsable P2, coordinar identidad P1 y portal P3. | Aprobar §4 y contrato C10 antes de implementar fase 2. |
| D0-06 | Contratos y matriz actor/operación F10/F13/F14. | Base observada y borradores en §2/§5; no acordados como conjunto. | Productor y consumidor confirman cada contrato; publicación definitiva antes de consumo. |
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
| Consultar/editar perfil propio F10 | No existe operación específica | Propuesta en §4; no ampliar `modificar` administrativo para resolverla. |
| Listar/gestionar vehículos | ADMINISTRADOR, CIUDADANO, CHOFER | No administrador requiere RESPONSABLE/ACTIVO por empresa; administrador tiene excepción. FUNCIONARIO no puede leer ni mutar por este servicio actual. |
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

## 4. D0-05: propuesta de perfil F10 para aprobación

- Roles de entrada propuestos: CIUDADANO o CHOFER con vínculo RESPONSABLE/ACTIVO
  comprobado en el EJB para la empresa; no basta rol o ID del navegador.
- Mantener el recorrido de una empresa responsable activa de Mi empresa. Ante
  ausencia o ambigüedad no elegir la primera ni cambiar el modelo global P1.
- Campos editables propuestos: `nombre`, `razonSocial`, `numeroEmpresa`,
  `direccion`. Nombre obligatorio y número positivo/único; reutilizar validaciones
  existentes. No imponer nuevas obligatoriedades a razón social/dirección sin acuerdo.
- Solo lectura/no mutables por esta operación: `id`, `fechaAlta`, `activa`,
  vínculos y bajas. Un payload manipulado nunca debe modificarlos.
- Preferir DTO de entrada específico con esos cuatro campos y una operación de
  perfil frente a ampliar el ABM administrativo. Costo: contrato y pruebas nuevos;
  beneficio: evita exponer campos administrativos por asignación indiscriminada.
- Empresa ajena o vínculo revocado/no responsable: denegar sin mutación.
  Confirmar además el tratamiento de empresa inactiva antes de fijarlo como regla.

## 5. Registro de contratos, productores y consumidores

**C12 existente** se documenta en contratos.md. **C10/C13/C14 son borradores para
acuerdo, NO rutas ni interfaces disponibles.** Las firmas EJB/DTO definitivas se
publicarán en carga-api y contratos.md antes de implementar sus consumidores.

| ID | Productor / responsable | Consumidor y coordinación | Frontera y estado |
| --- | --- | --- | --- |
| C01 | P1: identidad, perfil y vínculos | SPA P2, navegación P3 | Reutilizar OIDC web → `/ingreso` → `/mi-cuenta`, Bearer y sesión existentes; acordar entrada al área nueva, sin modificar callback todavía. |
| C10 / F10 | P2: negocio/EJB/REST de empresa | Mi empresa P2/P3; P1 para vínculo | Propuestos GET/PUT `/carga/api/empresas/{id}/perfil`; ID selecciona y EJB autoriza. GET/PUT 200 con perfil, sin paginación; entrada PUT limitada a §4. DTO de lectura, empresa inactiva y errores de ausencia pendientes de aprobación. |
| C12 / F12 | P2: `EmpresaServicio` / `EmpresaRecurso` | Área SPA P2; coordinar alta/solicitudes con P1 | Existentes GET con `nombre`, POST 201, PUT `/{id}` 200, DELETE `/{id}` 204; DTO `EmpresaDTO`. Sin GET por ID ni paginación actuales. No inventar un endpoint de detalle ni añadir paginación al contrato viejo sin acuerdo. |
| C13 / F13 | P2: nueva lectura vehicular por operación | Área SPA P2; permisos con P1 | Propuesto GET `/carga/api/vehiculos/consulta` para FUNCIONARIO/ADMINISTRADOR, sin mutaciones; `empresaId` obligatorio, matrícula opcional, página/tamaño. DTO paginado con datos vehiculares acordados. Fecha/zona de referencia para vigencia pendientes. |
| C14 / F14 | P2: nueva consulta de guías | Área SPA P2; fuentes P3/P4 | Propuesto GET `/carga/api/guias/consulta` para FUNCIONARIO/ADMINISTRADOR, `empresaId` obligatorio, período previsto y estado opcionales, página/tamaño. Detalle/asignaciones y DTO por acordar; no modificar `/guias/empresa`. |
| C14-E | P3: estado e historial reales | Composición F14 P2 | Contrato de lectura, transiciones, orden y campos pendientes de entrega/acuerdo. Separar previsto, persistido e historial; no reconstruir eventos. |
| C14-P | P4: pesajes/recorridos | Composición F14 P2 | GET diario existente devuelve `List<PesadaRegistradaDTO>`; consulta por guía y metadatos por acordar. Nunca asociar por matrícula/día en el navegador ni atribuir `guiaId=null` a un viaje. |
| CFIN | P4: `AvisoFinDeViajeLocal`; P3 lo invoca | P3 → adaptador P4; P2 integra consulta | Contrato existente ADR-0010; integración del emisor P3 pendiente de acreditar, no se da por entregada. |

Para los borradores C13/C14 se propone página base 0, tamaño predeterminado 20,
rango 1–100, desplazamiento dentro de entero de 32 bits y respuesta
`{registros, pagina, tamano, total}`. C13 ordenaría por matrícula/id; C14 por
inicio/id descendentes. Confirmar semántica de filtros (igualdad/búsqueda de
matrícula, límites de período y selección de estados) antes de crear consultas.
No deducir estado de la fecha prevista ni descartar históricos sin asignación.

Errores propuestos de C10/C13/C14: `400` para datos/filtros/paginación inválidos
y negocio (incluido número duplicado); `401` sin sesión válida, `403` para rol o
pertenencia insuficiente. Mantener `{"error":"mensaje"}`, correlación y fechas
ISO-8601, incluida la excepción actual de `EmpresaDTO.fechaAlta`.
El tratamiento de recurso inexistente y orden autorización/existencia deben
cerrarse expresamente antes de publicar; no introducir `404/409/422` ni el nuevo
sobre del estándar en contratos vigentes por efecto de este borrador.

## 6. Coordinaciones pendientes y evidencia exigida

| Dueño | Confirmación solicitada, aún no recibida | Aceptación que permitirá acreditar |
| --- | --- | --- |
| P1/P2 | D0-04 y autorización/empresa inactiva de C10; recorrido C12 sin duplicar solicitudes. | Identidades sintéticas por rol/vínculo, rechazo directo REST y ausencia de mutaciones ajenas. |
| P1/P3 | Entrada al área operativa desde gub.uy y navegación compartida. | Sesión válida/vencida, 401/403 y regresión de accesos P1. |
| P3/móvil | Si hay edición de guía, campos/estados, política offline y contrato C14-E. | Eventos duplicados/tardíos y conflictos con resultados acordados; no añadir edición/cancelación implícita. |
| P2/P3 | Inicio toma exclusión del mismo vehículo, revalida habilitación y cambia estado en JTA; acordar orden de locks. | Carreras reales inicio vs DELETE/PUT, rollback y vehículo distinto; mocks no bastan. |
| P3/P4 | Emisión CFIN y consulta C14-P por guía, varios días, paginación/disponibilidad. | Fin → aviso → pesajes → consulta, nodo caído y guías sucesivas con asociación `[inicio, fin)`. |
| Equipo/P5 | Dueño F15 y alcance de eventual integración de casos F04. | Reparto registrado, datos públicos delimitados; bloque P2 solo si se le asigna. |

La fase 1 conserva las matrices y procedimientos existentes. Esta entrega solo
contrasta fuentes y documenta decisiones/borradores: no escribe pruebas de lógica,
no ejecuta aplicación, builds ni tests, no instala herramientas y no declara
seguridad, concurrencia o integración verificadas.
