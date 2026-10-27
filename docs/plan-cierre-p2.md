# Plan por fases — Cierre de P2 y área del funcionario

**Fecha:** 22 de octubre de 2026. **Línea base observada:** `c3bc924`, árbol limpio antes de esta planificación.
**Estado:** fase 0 parcial, fase 1 preparada y fases 2, 3, 4 y parte P2 de fase 5
implementadas localmente al 2026-10-27, sin validación funcional ejecutada.
El usuario confirmó SPA, F10/C10, F13/C13 y C14 limitado a P2;
los demás permisos y contratos pendientes (incluidas fuentes P3/P4) no quedan autorizados.
Véase el [registro de acuerdos de fase 0](acuerdos-cierre-p2-fase-0.md).

## 1. Alcance y decisiones de partida

Este plan organiza [pendientesP2](pendientesP2.md). El usuario confirmó que **P2
implementará también la pantalla del funcionario**: gestión de empresas F12,
consulta vehicular F13 y composición del monitoreo F14. La asignación de interfaz
está confirmada; la tecnología SPA quedó confirmada al iniciar fase 0.
F10/C10, F13/C13 y C14 P2 fueron aprobados posteriormente; fuentes C14-E/C14-P
e integración real F14 requieren acuerdo y entrega.

| Responsabilidad | Dueño y límite |
| --- | --- |
| Perfil empresarial F10, ABM F11, empresas F12, consulta F13, guías/asignaciones CU2 | P2; integrar cambios del portal ciudadano con P3. |
| Interfaz operativa del funcionario F12–F14 | P2; coordinar navegación compartida con P1/P3. |
| Identidad gub.uy, roles, solicitudes y vínculos | P1; reutilizar sus flujos y contratos. |
| Eventos, fotos, deduplicación, estado e historial reales; aviso de fin | P3; P2 consume e integra, no sustituye al productor. |
| Posiciones, recorridos, pesajes y adaptador de fin de viaje | P4; consumir las integraciones existentes. |
| Motor, casos, descargos, resolución, parámetros y reportes | P5. La eventual interfaz de casos F04 dentro del área del funcionario requiere delimitar reparto con P5; no se reasigna implícitamente. |
| Validación pública F15 | Sin asignación confirmada; resolver reparto, no incluir desarrollo obligatorio. |

La página actual [Funcionarios.tsx](../carga-front/src/pages/backoffice/Funcionarios.tsx)
administra **accesos de funcionarios** y pertenece al recorrido P1. No es el área
operativa F12–F14 y no se reemplazará para construirla.

### Elección de interfaz confirmada en fase 0

El usuario pidió aplicar la recomendación preferida el 2026-10-22. Se registra
en [ADR-0013](decisiones/0013-area-funcionario-spa-p2.md), aceptada solo para
F12–F14 P2; la alternativa se conserva como justificación, no como decisión pendiente.

- **Elegida: ampliar la SPA existente**, con un área del funcionario accesible
  desde gub.uy. Reutiliza sesión, cliente REST y componentes; exige acordar el
  alcance de navegación compartida y completar contratos de lectura. No acepta globalmente ADR-0004.
- **Alternativa: conservar ese recorrido en JSF.** Reutiliza ABM, pero necesita
  coordinar con P1 un destino/puente OIDC que ADR-0011 no proporciona, además de
  comprobar navegación, postbacks y AJAX. No crear cuentas internas como atajo.

Se conservan las vistas JSF existentes en ambas opciones. No hay migración general,
framework nuevo ni cambio de autenticación aprobado por este acuerdo acotado.

## 2. Secuencia y entregas

### Estado contrastado con el código al 2026-10-27

Actualización F13 sobre `da10af0` y posterior entrega local C14 P2 de fase 5,
sin ejecutar builds, pruebas ni aplicación. `[x]`
significa código, prueba escrita o acuerdo documental presente según el texto;
NO acredita ejecución. Se separan las partes implementadas de su validación.

| Fase | Parte ya presente | Parte que falta para completar la fase |
| --- | --- | --- |
| 0 | Asignación P2, elección SPA, ADR-0005 alineado y F10/C10, F13/C13 y C14 P2 aprobados/publicados. | Permisos de guías, fuentes C14-E/C14-P, coordinaciones P1/P3/P4, política offline y reparto F15. |
| 1 | ABM F11 existente, matrices y cinco tests HTTP opt-in escritos. | Ejecutar y registrar validación REST/navegador/JSF y ABM empresarial/choferes; priorizar defectos reproducidos. |
| 2 | Backend C10, formulario de perfil propio y 20 tests Java escritos. | Validar REST/navegador e integración del portal compartido P1/P3. |
| 3 | Área SPA `/funcionario/empresas`, entrada Mi cuenta, listado/búsqueda, detalle, edición y baja C12; bandeja de responsables P1 reutilizada; ocho regresiones Java y matriz de escenarios preparados. | Validación real de permisos/expiración, navegador, administrador/JSF y rollback JTA; aceptación compartida P1/P3 y coordinación del alta empresarial P1. Vehículos/Viajes quedan para fases 4/5. |
| 4 | C13/ADR-0016 aprobado; EJB/REST de solo lectura y pantalla F13 con filtros/paginación y fecha servidor America/Montevideo. Catorce pruebas unitarias nuevas y escenarios escritos; regresión HTTP F11 conservada. | Ejecutar pruebas y matriz F13 en contenedor/navegador, autorización, JPQL, filtros literales y referencia temporal; aceptación integrada P1/P3. |
| 5 | C14/ADR-0018 aprobado; consulta P2 paginada y detalle separados de CU2, pantalla Viajes enlazada desde F12. Veinte pruebas Java y matriz escritas, sin ejecución; fuentes P3/P4 indicadas como pendientes. | Ejecutar pruebas/matriz C14 en contenedor/navegador; acordar e integrar estado/historial P3 y pesajes por guía P4. |
| 6 | Adaptador P4 de aviso de fin y protección P2 de baja vehicular existentes. | Productor real P3, integración de fin, política offline y pruebas reales de concurrencia/asociación de pesajes. |
| 7 | Plan, pendientes, contratos, ADR y procedimientos actualizados para los cambios locales. | Aceptación integral ejecutada con evidencia, cobertura medida, dependencias resueltas y cierre documental final. |

**Ninguna fase está cerrada integralmente:** las fases 0–5 tienen avance parcial;
las fases 6–7 conservan bases reutilizables, pero sus entregas siguen pendientes.

### Orden y dependencias

| Fase | Resultado | Dependencia de entrada |
| --- | --- | --- |
| 0 | Alcance, permisos y contratos acordados | Revisión con P1/P2/P3/P4; P5 solo donde corresponda. |
| 1 | F11 y núcleo existente con validación inicial registrada | Entorno aislado del equipo; no exige esperar toda la integración P3. |
| 2 | F10: perfil propio consultable y editable con pertenencia comprobada | Acuerdo F10 de fase 0 y coordinación del portal. |
| 3 | Área del funcionario y gestión de empresas F12 | Elección de interfaz, acceso P1 y contrato empresarial acordados. |
| 4 | Consulta de vehículos, permiso e ITV F13 | Área de fase 3 y contrato de lectura autorizado. |
| 5 | F14: consultas P2 e interfaz de viajes preparada | Área de fase 3 y C14 aprobado; fuentes P3/P4 pendientes explícitas, requieren acuerdo antes de consumirlas. |
| 6 | Estado real, conflictos offline y concurrencia integrados | Entregas P3/móvil y contratos P4; política aceptada antes de codificar. |
| 7 | Aceptación integral y cierre documental con evidencia | Fases anteriores acreditadas; dependencias abiertas visibles. |

**Orden recomendado:** 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7.
Tras fase 0, F10 y el recorrido funcionario pueden avanzar de forma independiente;
si P3 aún no entrega eventos, se puede preparar F14 sin declararlo cerrado.
La validación comienza en fase 1 y se repite sobre los cambios, no se pospone toda
al final. No se fijan fechas sin conocer disponibilidad de equipo y dependencias.

## Fase 0 — Resolver decisiones que condicionan la implementación

**Objetivo:** evitar construir pantallas sobre permisos o políticas no acordados.

- [x] Registrar la asignación P2 del área F12–F14 y delimitar la eventual integración de casos con P5.
  D0-01: F04 permanece P5; cualquier integración requiere acuerdo separado.
- [x] Acordar SPA acotada o JSF con OIDC; documentar la decisión arquitectónica
  pertinente sin convertir ADR-0004 en aceptada por iniciativa del agente.
  D0-02: elección explícita del usuario registrada en ADR-0013; navegación P1/P3 pendiente.
- [ ] Acordar con P1/P2 qué vínculo habilita cada operación. Hoy crear guías y
  consultar sus catálogos comprueba `ACTIVO` sin tipo; el listado empresarial y
  el ABM vehicular exigen `RESPONSABLE/ACTIVO`. No unificar permisos silenciosamente.
- [x] Alinear ADR-0005 con `VinculoEmpresaUsuario`, sin restaurar `usuario.empresa_id`.
  D0-03: alineación documental; no modifica el criterio ACTIVO/RESPONSABLE pendiente.
- [x] Definir campos editables de F10: nombre, razón social, número y dirección;
  excluir estado, fecha de alta, vínculos y bajas de la edición del responsable.
  D0-05/C10 confirmado por el usuario y registrado en ADR-0014.
- [x] Acordar y publicar matriz, campos, interfaces, DTO, rutas y errores F10/C10
  antes de su consumidor, según ADR-0014.
- [x] Acordar y publicar C13/F13 con roles, filtros, paginación, vigencias y errores
  antes del consumidor; confirmado por el usuario en ADR-0016.
- [x] Acordar matriz de actor/operación y contrato F14/C14 P2 antes de su consumidor,
  según ADR-0018. C14-E/C14-P pendientes, sin aprobación implícita.
- [ ] Confirmar con P3/móvil si hay cambios de guía que requieran resolver
  conflictos offline y qué campos/estados comprenden. Acordar política en ADR y
  contratos; no agregar edición/cancelación como requisito supuesto.
- [ ] Obtener compromiso de P3 para estado/historial, bloqueo común al iniciar
  viaje y llamada de fin a P4. Confirmar con P4 cómo consultar pesajes de una guía.
- [ ] Resolver el reparto F15: si P2 recibe una parte, añadir un bloque separado
  con campos públicos, contrato y aceptación; en caso contrario registrar dueño.

**Entregable:** decisiones/ADR acordados, matriz de permisos y lista de contratos
con productores, consumidores y responsables. Puede cerrarse por bloque para no
detener F10/F12 por una política offline que solo afecta otra dependencia.
**Salida:** ninguna implementación comienza dependiendo de una decisión pendiente;
el desacuerdo bloquea solo el tramo afectado y se consulta antes de modificar otro frente.

**Avance registrado:** [acuerdos, matriz observada y borradores de contratos](acuerdos-cierre-p2-fase-0.md).
Se publicaron en contratos.md las operaciones empresariales PUT/DELETE ya existentes.
Fase 0 **NO cerrada**: F10/C10, F13/C13 y C14 P2 están aprobados por el usuario y publicados;
fuentes C14-E/C14-P, permisos de guías, navegación compartida, offline y reparto F15
siguen abiertos. La fase 0 original fue documental; la fase 2 posterior sí implementa F10.

## Fase 1 — Acreditar la base existente, sin rehacer F11

**Cobertura:** F11 y primera pasada de P2-04.

- [x] Disponer de la base F11 implementada: ABM del responsable, baja lógica
  protegida por EN_CURSO y reactivación específica sin renovar permiso/ITV.
  Es implementación previa reutilizada, no evidencia de aceptación de fase 1.
- [x] Reutilizar [matriz P2](../pruebas/p2-cierre.md),
  [integración aislada](../pruebas/p2-integracion.md),
  [pruebas vehiculares](pruebas-abm-vehiculos.md) y [regresión JSF](pruebas-abm-jsf.md).
  Trazabilidad y estado inicial por escenario en el [registro de fase 1](../pruebas/p2-fase-1.md).
- [x] Preparar suite HTTP opt-in F11 para 204/reactivación, ABM, datos heredados,
  rechazo EN_CURSO por DELETE/PUT e identidad/vínculo: cinco tests escritos,
  **sin ejecución ni acreditación funcional**. No se modificó la lógica existente.
- [ ] Equipo: validar alta/edición/baja/reactivación, duplicados, datos heredados,
  fechas nulas, aislamiento empresarial, vínculo revocado y acceso directo REST.
- [ ] Acreditar 204 de reactivación por HTTP, sin sobrescribir datos ni renovar
  permiso/ITV; confirmar rechazo de baja por DELETE y PUT ante `EN_CURSO`.
- [ ] Validar cargas, vacíos, errores, teclado/foco, sesión vencida, doble envío y
  respuestas tardías; repetir asignación de guías con vehículos dados de baja/reactivados.
- [ ] Verificar ABM empresarial y de choferes y registrar defectos reproducibles.
  Para cada corrección, escribir primero su regresión y mantener cambios acotados.

**Salida:** resultados iniciales por escenario y versión, con defectos priorizados.
Solo tras acreditar los escenarios P2 ejecutados podrá declararse F11 **validado
parcialmente** mientras falte la carrera inicio/baja con P3. Hoy sigue pendiente
esa validación; no inferir que un viaje terminó por su fecha prevista.

**Avance:** fase 1 abierta. La preparación y el contraste estático no convierten
F11 en validado: no hay resultados HTTP, de navegador ni JUnit ejecutados.
El equipo debe proveer despliegue/fixtures aislados y registrar evidencia de los
escenarios F1-01–12; la carrera con inicio P3 permanece como dependencia de fase 6.

## Fase 2 — Completar el perfil propio de empresa (F10 / P2-01)

- [x] Escribir primero pruebas de responsable válido, empresa ajena, vínculo
  ausente/revocado/no responsable, datos inválidos y número duplicado.
- [x] Acordar y exponer consulta/edición del perfil propio, resolviendo identidad y
  pertenencia en EJB. Preferir una operación específica frente a ampliar sin más
  `modificar`: delimita campos y evita habilitar acciones administrativas; añade
  contrato, por lo que requiere coordinación.
- [x] Reutilizar DTO/validaciones adecuados y actualizar solo campos acordados;
  un cuerpo manipulado no debe cambiar estado, fecha de alta ni vínculos.
- [x] Implementar formulario local en Mi empresa; conservar Choferes/Vehículos,
  estados de carga/guardado/error y tratamiento de sesión vencida.
- [ ] Acreditar la integración del portal compartido con P1/P3 en el despliegue del equipo.
- [ ] Validar por REST y navegador; mantener aparte la modificación administrativa.

**Salida:** responsable guarda su perfil y ve el resultado persistido; un actor
ajeno no puede hacerlo ni manipulando ID/payload ni invocando REST directamente.

**Avance:** backend C10 y `/mi-empresa/perfil` preparados según ADR-0014; pruebas
escritas antes de la lógica Java, sin ciclo rojo/verde ejecutado. La SPA carece de
runner de tests; los escenarios no equivalen a automatización frontend. Ver
[aceptación y límites](../pruebas/p2-perfil-empresa.md). La implementación presente
NO acredita aún esta salida ni cierra la validación de fase 1.

## Fase 3 — Construir el área del funcionario y empresas (F12 / P2-02)

- [x] Disponer de listado/búsqueda y modificación empresarial administrativos
  en los servicios existentes, con roles de funcionario/administrador para mutaciones.
- [x] Publicar los contratos PUT/DELETE empresariales observados en código.
- [x] Añadir navegación local gub.uy → Mi cuenta → `/funcionario/empresas` →
  `/funcionario/empresas/:id`, solo para FUNCIONARIO; callback y sesión P1 intactos.
  El detalle usa GET del listado, sin inventar GET individual. Vehículos enlaza
  a C13 de fase 4 y Viajes a C14 P2 de fase 5, sin integración real P3/P4.
- [x] Preparar primero ocho regresiones Java adicionales y escenarios de permisos
  por operación, identidad insuficiente/vencida, errores y administrador.
- [ ] Ejecutar esos escenarios en contenedor/navegador; las anotaciones y dobles
  no acreditan autorización real, sesión vencida ni regresión administrativa/JSF.
- [x] Consumir listado/búsqueda y modificación empresarial existentes desde la
  nueva interfaz operativa del funcionario; el servicio existente no sustituye la pantalla.
- [x] Integrar modificación administrativa de cinco campos y baja confirmada 204,
  sin reactivar ni reenviar mutaciones automáticamente. Reutilizar la bandeja P1
  en `/funcionario/solicitudes-responsables`; es de vínculos, NO de altas empresariales.
- [ ] Coordinar alta empresarial con P1, sin un POST/formulario paralelo ni dar
  por equivalente una solicitud de responsabilidad y una solicitud de alta.
- [x] Mantener la implementación existente de baja empresarial: desactivación,
  revocación de vínculos activos y llamada de auditoría en la operación EJB.
- [ ] Acreditar conservación de esa baja desde la nueva interfaz y rollback real
  de empresa, vínculos y auditoría ante fallo.
- [x] Añadir navegación por rol reutilizando RutaProtegida; no cambia permisos EJB
  ni impone gub.uy al administrador de los recorridos anteriores.
- [ ] Acreditar navegación compartida con P1/P3, identidad real en el contenedor
  y acceso directo a operaciones.

**Salida:** un funcionario autenticado por gub.uy consulta y modifica empresas
existentes con sus permisos, sin otra identidad administrativa ni pérdida de JSF.

**Avance local:** interfaz y consumidor C12 presentes, con errores/carga/vacío,
exclusión de envíos duplicados y reconsulta obligatoria ante resultado incierto.
Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.
La SPA no tiene runner; la [matriz F12](../pruebas/p2-funcionario-empresas.md)
es documentación de aceptación, NO pruebas frontend automatizadas. La salida
anterior sigue pendiente de acreditación; no se cierra la fase por el código local.

## Fase 4 — Fiscalizar datos vehiculares en solo lectura (F13 / P2-03)

- [x] Escribir pruebas HTTP del rechazo actual de lectura y alta/edición/baja/
  reactivación para identidad con solo FUNCIONARIO, en la suite F11. Sin ejecución.
- [x] Escribir pruebas positivas de la nueva consulta F13 y mantener la regresión
  de rechazo de mutaciones para identidad con solo ese rol.
- [x] Añadir una operación de lectura específica o autorización por método,
  manteniendo los consumidores actuales. **No** agregar funcionario a los roles
  de toda `VehiculoServicioBean` ni concederle escritura para facilitar el listado.
- [x] Mostrar matrícula, habilitación/baja, número y fechas del permiso, ITV y
  vencimiento; distinguir permiso futuro/vencido y datos ausentes de baja lógica.
- [x] Acordar fecha/zona de referencia y reglas de presentación con negocio;
  no cambiar las reglas temporales de asignación de guías como efecto colateral.
- [x] Integrar listado por empresa, filtros acordados, carga/vacío/error y manejo de 401/403.
- [ ] Ejecutar pruebas y [matriz F13](../pruebas/p2-consulta-vehicular.md): roles reales,
  paginación/JPQL, permisos del ABM intactos, límites temporales y navegador;
  acreditar aceptación integrada P1/P3.

**Avance local:** `ConsultaVehiculosLocal`/EJB/REST separado del ABM y ruta SPA
`/funcionario/empresas/:id/vehiculos` enlazada desde F12. Referencia temporal del
servidor en America/Montevideo y campos/estados acordados en
[ADR-0016](decisiones/0016-consulta-vehicular-funcionario.md).
Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.
La matriz frontend no es automatización; la fase no se declara cerrada en ejecución.

**Salida:** funcionario consulta los datos requeridos; el servidor sigue rechazando
las mutaciones no concedidas, independientemente de los botones visibles.

## Fase 5 — Preparar monitoreo de viajes y pesajes (F14, propiedad de interfaz P2)

### 5.1. Contratos y lecturas P2

- [x] Diseñar e implementar C14 aprobado: consulta paginada de guías para funcionario
  con empresa, solapamiento previsto y estado, y detalle/asignaciones. Pruebas
  escritas primero, NO ejecutadas.
  El actual `GET /guias/empresa` exige vínculo responsable y NO admite funcionario:
  no reutilizarlo eliminando esa garantía ni ampliar roles de toda la clase.
- [x] Separar período previsto, estado persistido e historial real; admitir datos
  históricos/asignaciones ausentes sin errores ni reconstrucciones ficticias.
- [ ] Acordar con P3 lectura de estado/historial y con P4 asociación de pesajes y
  fechas. Las firmas/rutas nuevas son entregables, no contratos que ya existan.

### 5.2. Composición de la interfaz

- [x] Disponer de las APIs P4 existentes de pesajes diarios y recorridos por guía
  como fuentes reutilizables. No implica consulta de pesajes por guía ni integración F14.
- [x] Listar viajes y mostrar detalle con empresa, guía, vehículo y chofer, carga,
  período previsto y estado persistido P2; navegación F12 → Viajes.
- [ ] Incorporar estado/historial de P3 y pesajes de P4 cuando estén acordados y disponibles.
- [ ] Reutilizar `GET /pesajes?matricula=&fecha=` y, si el alcance necesita
  recorrido, la API P4 por guía. La consulta diaria no equivale a consulta de viaje:
  `PesadaRegistradaDTO.guiaId` debe conservar su significado; no atribuir al viaje
  todas las pesadas del vehículo/día ni las que tienen `guiaId=null`.
- [ ] Acordar consulta por guía con P4 si la API diaria resulta insuficiente para
  viajes de varios días/paginación. No trasladar al navegador autorización ni
  reconstrucción de asociaciones que corresponden al servidor.
- [x] Mostrar carga, vacío y error para C14 P2, y fuentes P3/P4 como pendientes de
  integración (no listas vacías ni indisponibilidad de nodos inferida).
- [ ] Al integrar fuentes, mostrar estados separados de carga, sin resultados y fuente no disponible,
  incluida información parcial/última actualización cuando el contrato lo permita.
  No agregar sincronización manual, polling o mapas como obligaciones nuevas.
- [x] Preparar escenarios C14 P2 y fixtures unitarios sintéticos, sin presentarlos
  como viajes reales ni modificar producción. Contratos/fixtures de P3/P4 pendientes.
- [ ] Ejecutar [matriz C14 P2](../pruebas/p2-consulta-viajes.md), pruebas de roles reales,
  JPQL, solapamiento, paginación, JSON y navegador; aceptación compartida pendiente.

**Avance local:** `ConsultaGuiasLocal`/EJB/REST separado de CU2, rutas SPA
`/funcionario/empresas/:id/viajes` y `.../viajes/:guiaId`, según
[ADR-0018](decisiones/0018-consulta-viajes-funcionario-p2.md). Veinte pruebas Java
escritas antes del código; ciclo rojo/verde no ejecutado por restricción de builds.
La SPA no tiene runner; su matriz no equivale a automatización frontend.
No se consumen endpoints P3/P4 supuestos, mapas, polling ni sincronización manual.

**Salida:** consultas P2 e interfaz disponibles según contratos; F14 permanece
**pendiente de integración** si P3 no entrega estado/historial reales. Un mapa, una
guía con fin previsto pasado o una maqueta no cierran este requisito.

## Fase 6 — Integrar ejecución real, offline y concurrencia

**Cobertura:** dependencia final F11, F14 y P2-06; participación P2 en F07/F08.

- [x] Disponer del adaptador P4 existente `AvisoFinDeViajeLocal` →
  `AvisoFinDeViajeBean` → `FinDeViajeMDB`. Su presencia no acredita un emisor P3 conectado.
- [ ] P3 implementa/entrega eventos y transiciones; P2 coordina que inicio y baja
  tomen la misma exclusión del vehículo, revaliden habilitación y confirmen sin
  estados parciales. Acordar orden de bloqueos con los consumidores existentes.
- [ ] Probar inicio simultáneo con DELETE y con PUT de baja: ningún resultado
  debe dejar un viaje iniciado con una baja incompatible. Repetir con vehículo
  distinto, otros estados y rollback; no basta una secuencia de llamadas o mocks.
- [ ] Aplicar la política offline acordada: eventos repetidos, tardíos y conflicto
  con cambios permitidos de guía. P3/móvil manejan eventos y deduplicación; P2
  aporta sus reglas y cambios propios, no rehace el receptor.
- [ ] P3 conecta el fin real con `AvisoFinDeViajeLocal`, reutilizando el adaptador P4.
- [ ] Acreditar fin → aviso → pesajes → consulta del funcionario, incluido nodo caído.
- [ ] Comprobar viajes que atraviesan fechas y guías sucesivas del mismo vehículo,
  sin romper asociación de pesajes por `[inicio, fin)` ni interpretación de `guiaId`.

**Salida:** F11 y F14 funcionan con productores reales; política offline y pruebas
de integración registradas. Una dependencia no entregada se reporta con dueño,
efecto y condición de desbloqueo, sin declararla resuelta desde P2.

## Fase 7 — Aceptación integral y cierre verificable

- [x] Actualizar plan, pendientes, contratos, ADR y procedimientos de las
  implementaciones locales F11/F10, dejando visibles pruebas e integraciones no ejecutadas.
- [ ] Equipo: ejecutar CU2 en WildFly/PostgreSQL: pertenencia, habilitación,
  permiso/ITV durante todo `[inicio, fin)`, peso/volumen independientes, datos
  heredados y límites temporales.
- [ ] Acreditar dos asignaciones incompatibles concurrentes sobre vehículo o
  chofer mediante transacciones independientes: una confirma y la otra se
  rechaza; comprobar rollback y ausencia de datos parciales.
- [ ] Repetir consultas del chofer, paginación empresarial e históricos, ABM/JSF,
  perfil propio y área del funcionario con identidades sintéticas de cada actor.
- [ ] Verificar sesión vencida, autorización directa, accesibilidad por teclado,
  errores y respuestas tardías. Medir cobertura antes de afirmar un porcentaje.
- [ ] Completar el cierre documental con versión desplegada exacta, entorno,
  identidad sintética, resultados ejecutados y evidencia sin secretos; las
  actualizaciones locales anteriores no sustituyen esta aceptación final.
- [ ] Registrar resolución de reparto F15 y cualquier integración P5; no añadir
  F15/F04 al cierre propio P2 si no se acordó esa implementación.

**Salida:** distinguir **implementado**, **validado por bloque** y **cerrado integrado**.
No declarar terminado todo P2 mientras haya requisitos propios o dependencias de
aceptación abiertas; tampoco atribuir a P2 la falta de implementación ajena.

## 3. Matriz de trazabilidad

| Pendiente original | Fases que lo resuelven | Condición de cierre |
| --- | --- | --- |
| P2-01 / F10 | 0, 2, 7 | Perfil propio persistido y autorización por pertenencia acreditada. |
| F11 | 1, 6, 7 | ABM validado y exclusión con inicio real de viaje acreditada. |
| P2-02 / F12 | 0, 3, 7 | Acceso gub.uy y gestión de empresas existentes completos. |
| P2-03 / F13 | 0, 4, 7 | Consulta accesible sin escrituras vehiculares adicionales. |
| P2-04 / CU2, ABM, JSF | 1, 7; regresiones en cada cambio | Reglas, roles, rollback, concurrencia y consumidores acreditados. |
| P2-05 / coherencia y definiciones | 0; aplicación en 2–6 | Criterios y ADR/contratos alineados antes de depender de ellos. |
| P2-06 / offline y estado | 0, 6 | Política aceptada y aplicada por P2/P3/móvil en sus límites. |
| F14 / pantalla del funcionario | 0, 3, 5, 6, 7 | Interfaz P2 integrada con guías, estado/historial y pesajes reales. |
| F15 / reparto | 0, comprobación en 7 | Responsable definido; bloque adicional solo si se asigna a P2. |

## 4. Método, skills y límites de validación

- **Skills aplicadas a la planificación:** `jakarta-ee-wildfly`,
  `contratos-integracion`, `seguridad-jakarta-oidc`, `react-typescript-carga` y
  `jakarta-testing`: separación de capas, compatibilidad, mínimo privilegio,
  reutilización de SPA y evidencia de pruebas por nivel.
- En cada cambio de lógica: prueba primero, implementación mínima y refactor.
  JUnit con dobles no acredita roles del contenedor, JTA, JPQL ni locks reales.
- **El agente no ejecuta builds ni pruebas que compilen.** El equipo valida en
  su procedimiento autorizado; cambiar esta restricción exige autorización
  explícita. No instalar runners ni modificar CI como parte implícita del plan.
- La SPA no tiene script `test`: acordar estrategia antes de automatizar nueva
  lógica frontend. Escenarios de aceptación no equivalen a tests escritos ni a
  ciclo TDD verificado. Mantener esa limitación visible.
- Para documentación: revisión de diff, enlaces locales y `git diff --check`.
  Para código futuro: preservar DTO/fechas ISO-8601, errores publicados, Bearer y
  manejo de 204; sin cambios unilaterales de contratos ni tablas de otros frentes.
- La planificación original no aprobó ADR ni implementó funcionalidad. Al iniciar
  fase 0 se añadió ADR-0013 por elección explícita del usuario y se alinearon fuentes
  documentales; esto no acredita ejecución de pruebas ni cierre funcional.

**Siguiente paso:** el equipo acredita los escenarios preparados F11/F10/F12/F13/C14 P2
en su entorno aislado; acordar fuentes C14-E/C14-P antes de integrar F14, junto con
los acuerdos restantes por bloque de fase 0 antes de depender de ellos.
