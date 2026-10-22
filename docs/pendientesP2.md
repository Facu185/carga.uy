# Pendientes P2 — Núcleo de negocio y backoffice

**Fecha de corte:** 22 de octubre de 2026.
**Base del inventario original:** `c7041f4` más los cambios locales de vehículos de ADR-0012.
**Planificación posterior:** contrastada sobre `c3bc924`; ver el
[plan de cierre por fases](plan-cierre-p2.md), que incluye el área del funcionario a cargo de P2.
**Avance fase 0:** SPA confirmada por el usuario en
[ADR-0013](decisiones/0013-area-funcionario-spa-p2.md); matriz y contratos propuestos
en el [registro de acuerdos](acuerdos-cierre-p2-fase-0.md). No es cierre funcional.
**Inicio fase 1:** [registro de validación inicial](../pruebas/p2-fase-1.md), con
cinco tests HTTP F11 nuevos y escenarios pendientes. Sin ejecución funcional.
**Fase 2 / F10:** implementación local de perfil preparada según
[ADR-0014](decisiones/0014-perfil-propio-empresa.md), con
[aceptación pendiente](../pruebas/p2-perfil-empresa.md).

## 1. Alcance y criterio de cierre

P2 es dueño del negocio de empresas, vehículos, choferes, creación/asignación de
guías y sus validaciones, según [frentes](frentes.md). Este documento desglosa los
pendientes del [inventario general](pendientes-funcionales-laboratorio-2026.md),
conservando sus identificadores F10–F15. El usuario asignó además a P2 la pantalla
operativa del funcionario F12–F14; no se reasignan los servicios de otros frentes.
Las referencias a la letra provienen de ese inventario: no se hizo una nueva
lectura del PDF externo ni una auditoría exhaustiva en esta actualización.

- `[x]` significa **implementación presente**, no prueba superada ni despliegue.
- `[ ]` significa implementación, definición, coordinación o validación pendiente;
  se identifica el tipo en cada sección.
- No se ejecutaron aplicación, pruebas ni builds. Los escenarios del navegador y
  las garantías reales de roles/JTA/JPQL/bloqueos siguen por acreditar.
- No añadir obligaciones nuevas a partir de una propuesta: F15 requiere acordar
  dueño; la pantalla F14 ya fue asignada a P2. La política de conflictos y la
  concreción de contratos requieren acuerdo antes de depender de ellas. La
  elección de SPA F12–F14 ya está registrada en ADR-0013, sin migración global.

## 2. Resumen operativo

| Bloque | Tipo de pendiente | Resultado esperado para cerrar |
| --- | --- | --- |
| F10 — Perfil de empresa del responsable | Implementación local preparada; validación e integración P1/P3 pendientes | Acreditar consulta/edición de su empresa, con pertenencia en servidor y campos administrativos preservados. |
| F11 — Vehículos del responsable | Validación P2 y dependencia P3 | Probar el ABM ya implementado e integrar el estado real del viaje y su concurrencia con baja. |
| F12 — Empresas para funcionario | Interfaz P2 e integración de acceso P1 | Recorrido desde gub.uy hasta consultar/modificar empresas existentes sin retirar seguridad. |
| F13 — Vehículos/permisos/ITV para funcionario | Interfaz P2, consulta y autorización de lectura | Permitir la consulta requerida sin conceder escritura de vehículos al funcionario por extensión accidental. |
| Guías y ABM existentes | Validación y coherencia de contratos | Acreditar reglas, aislamiento y concurrencia real; resolver discrepancias documentales y de criterio de vínculo. |
| Conflictos offline y estado del viaje | Coordinación P2/P3 | Acordar política y contrato; mantener P3 como dueño de eventos/estado/historial. |
| F14 — Pantalla operativa del funcionario | Interfaz e integración a cargo de P2 | Consumir datos P2, estado/historial P3 y posiciones/pesajes P4, sin rehacer sus servicios. |
| F15 | Participación o asignación por acordar | Proveer datos/reglas P2 solo según el reparto que se acuerde. |

## 3. Base ya implementada: no rehacer desde cero

- [x] Servicios, DTO, REST y vistas JSF para ABM de empresas, vehículos y choferes.
- [x] Creación de guía y asignación de vehículo/chofer; validación de empresa,
  habilitación, permiso, ITV, capacidades e intervalos sin solapamiento en código.
- [x] Consultas de guías del chofer y de la empresa, con paginación en esta última.
- [x] Pantalla ciudadana de choferes en Mi empresa y nuevo ABM vehicular en
  `/mi-empresa/vehiculos`, conservando las vistas JSF.
- [x] Baja vehicular lógica protegida ante guía `EN_CURSO`, incluso vía PUT, y
  operación de reactivación que no sobrescribe los demás datos.
- [x] Pruebas unitarias/regresiones presentes; esto NO acredita cobertura medida
  ni ejecución satisfactoria.

Fuentes: [EmpresaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/EmpresaServicioBean.java),
[VehiculoServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/VehiculoServicioBean.java),
[ChoferServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/ChoferServicioBean.java),
[GuiaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/GuiaServicioBean.java)
y [rutas SPA](../carga-front/src/App.tsx).

## 4. Implementaciones funcionales pendientes de P2

### P2-01 / F10 — Perfil de la empresa del responsable

**Origen:** letra p. 5, según inventario general. **Coordinación:** P1 para
identidad/vínculo; P3 para integrar el portal.

**Situación:** el usuario aprobó C10/ADR-0014 y existe perfil específico con esos
cuatro campos en `/mi-empresa/perfil`. Su EJB requiere vínculo RESPONSABLE/ACTIVO
y empresa activa; el ABM administrativo permanece separado. Ejecución pendiente.

- [x] Implementar la consulta del perfil de su empresa en Mi empresa.
- [x] Implementar edición de los campos empresariales del requisito, reutilizando
  DTO/validaciones existentes y delimitando explícitamente los campos editables.
- [x] Autorizar la modificación en EJB por principal y vínculo responsable activo
  de esa empresa, sin abrir la modificación de cualquier empresa al rol ciudadano.
- [x] Conservar por separado las operaciones administrativas: editar el perfil no
  debe permitir bajas, alteración de vínculos o cambios de estado implícitos.
- [x] Publicar contrato aprobado e implementar consumidor local; escribir primero pruebas positivas y de
  rechazo: empresa ajena, vínculo ausente/revocado, datos inválidos y número duplicado.
- [ ] Acreditar integración compartida con P1/P3 y sus identidades reales de prueba.
- [ ] Validar el flujo autenticado en servidor/navegador, no solo la visibilidad del formulario.

**Cierre:** el responsable puede consultar y guardar su perfil autorizado; otro
usuario no puede modificarlo alterando el ID ni invocando directamente REST.

### P2-02 / F12 — Gestión de empresas por funcionario

**Origen:** letra p. 6. **Interfaz:** P2. **Coordinación:** P1 para acceso; P3 para navegación compartida.

**Situación:** interfaz F12 implementada localmente en `/funcionario/empresas`,
con entrada desde Mi cuenta tras gub.uy, búsqueda/detalle, edición administrativa
y baja por C12 existente. Conserva JSF y callback P1; no necesita puente OIDC JSF.
La bandeja P1 de responsables se reutiliza, pero NO es un alta empresarial.
Sin validación ejecutada; aceptación compartida y coordinación del alta pendientes.

- [x] Elegir SPA existente para F12–F14, según ADR-0013; conservar JSF sin
  aceptar una migración general. No crear cuentas internas de funcionario.
- [ ] Coordinar con P1/P3 la navegación compartida desde el ingreso gub.uy.
- [x] Habilitar navegación local autenticada para consultar/modificar empresas existentes.
- [x] Integrar edición administrativa y baja usando el servicio existente;
  no cambia interfaces ni concede permisos nuevos.
- [x] Reutilizar la bandeja de vínculos responsables P1 sin duplicar su lógica.
- [ ] Coordinar el alta empresarial con P1; no confundirla con vínculos responsables.
- [x] Conservar la llamada a baja existente: desactivación, revocación de vínculos
  activos y auditoría en la misma operación EJB.
- [x] Preparar ocho regresiones Java adicionales y [escenarios F12](../pruebas/p2-funcionario-empresas.md)
  antes de la interfaz; no ejecutados. No hay runner frontend.
- [ ] Acreditar persistencia y rollback real de la baja en la nueva interfaz.
- [ ] Probar permisos reales por actor, validaciones, errores y sesión vencida;
  documentar el recorrido de aceptación elegido.

**Cierre:** un funcionario que ingresó por gub.uy llega a las operaciones de
empresas que tiene autorizadas; no depende de una identidad administrativa distinta.

### P2-03 / F13 — Consulta de vehículos, permiso e ITV por funcionario

**Origen:** letra p. 6. **Interfaz y datos:** P2. **Coordinación:** P1 para permisos y P3 para navegación compartida.

**Situación:** los datos ya existen, pero `VehiculoServicioBean` permite
administrador/ciudadano/chofer, no funcionario. Añadir funcionario a la anotación
de toda la clase también habilitaría operaciones de escritura: no es una solución
de mínimo privilegio para este requisito de consulta.

- [ ] Definir/publicar una operación de lectura autorizada para funcionario por
  empresa, reutilizando datos/DTO existentes cuando sean adecuados.
- [ ] Mostrar matrícula, estado, permiso y sus fechas, ITV y su vencimiento.
- [ ] Explicitar la fecha de referencia y cómo se presenta la vigencia, distinguiendo
  baja lógica, permiso vencido e ITV ausente/vencida; no tratarlos como un único estado.
- [ ] Integrar la consulta en el recorrido autenticado del funcionario.
- [ ] Escribir pruebas que permitan lectura y demuestren que alta, modificación,
  baja y reactivación NO quedan autorizadas al funcionario por este cambio.
- [ ] Validar respuestas REST, filtros y comportamiento con datos ausentes o vencidos.

**Cierre:** el funcionario puede fiscalizar los datos requeridos sin obtener
permisos de gestión vehicular no acordados.

## 5. F11 implementado: validaciones y dependencias pendientes

Alcance confirmado en [ADR-0012](decisiones/0012-abm-vehiculos-del-responsable.md).
No están pendientes otra pantalla de vehículos, adjuntos ni selector multiempresa.
La pantalla exige una única empresa responsable activa; no modifica por sí sola
el modelo o las reglas globales de vínculos P1.

- [ ] Ejecutar regresiones de
  [VehiculoServicioBeanAutorizacionTest](../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/servicio/VehiculoServicioBeanAutorizacionTest.java)
  y [VehiculoRecursoTest](../carga-web/src/test/java/uy/edu/fing/tse/carga/web/rest/VehiculoRecursoTest.java)
  en el entorno/procedimiento autorizado del equipo.
- [ ] Acreditar respuesta HTTP 204 de reactivación y errores de negocio/autorización;
  el test web actual solo cubre declaración de ruta y delegación de denegación.
- [ ] Validar el recorrido completo de [pruebas-abm-vehiculos](pruebas-abm-vehiculos.md):
  altas/ediciones, datos heredados, duplicados, baja y reactivación, carga/vacío/error,
  teclado/foco, sesión vencida, doble envío y respuestas tardías.
- [ ] Comprobar con base y contenedor reales que EN_CURSO del mismo vehículo
  bloquea DELETE y PUT, sin cambios parciales; otros vehículos/estados no lo bloquean.
- [ ] Verificar empresa ajena, vínculo revocado y acceso directo REST sin depender
  del filtro visual; comprobar también excepción administrativa y sus límites.
- [ ] Con P3, integrar la transición real a EN_CURSO/FINALIZADA y serializar inicio
  de viaje/baja usando bloqueo del vehículo y verificación de habilitación. No
  deducir finalización por `fin` previsto ni declarar resuelta esta dependencia.
- [ ] Probar regresión JSF y asignación de guías: el vehículo dado de baja no se
  asigna; reactivar no renueva ni evita la comprobación de permiso/ITV.
- [ ] Coordinar con los consumidores P2/P3 la nueva operación y el rechazo de baja
  en curso antes de integrar los cambios compartidos.

**Estado de pruebas:** escritas, ciclo rojo/verde no ejecutado por restricción de
builds. La SPA no tiene runner de tests configurado: definir esa estrategia con el
equipo si se requiere automatización, sin instalar herramientas por iniciativa del agente.

## 6. Guías, ABM existentes y coherencia documental

### P2-04 — Validaciones e integración del núcleo ya presente

- [ ] Acreditar CU2 con EJB/JTA/base reales: permisos, pertenencia de empresa,
  vehículo/chofer habilitados y vigencia en todo el intervalo `[inicio, fin)`.
- [ ] Verificar capacidades independientes de peso y volumen, datos heredados sin
  capacidad volumétrica, límites temporales y rechazo de solapamientos.
- [ ] Probar dos asignaciones simultáneas sobre el mismo vehículo/chofer con
  transacciones independientes: una confirma y la incompatible se rechaza.
- [ ] Verificar rollback ante fallo y conservación de las consultas del chofer y
  empresa, incluida paginación e historial sin asignaciones actuales.
- [ ] Ejecutar la regresión de empresas/choferes y la
  [comprobación JSF](pruebas-abm-jsf.md), incluido rechazo de accesos ajenos y
  funcionamiento con la identidad real del contenedor.
- [ ] Registrar resultados, limitaciones y cobertura medida antes de afirmar cierre;
  el objetivo del equipo no es un resultado de cobertura ya alcanzado.

### P2-05 — Definiciones/contratos por alinear, no cambios automáticos

- [x] Alinear [ADR-0005](decisiones/0005-autorizacion-y-concurrencia-de-guias.md)
  con contrato/código basados en `VinculoEmpresaUsuario`, sin restaurar
  `usuario.empresa_id` ni cambiar criterios de autorización por esta corrección documental.
- [ ] Acordar con P1/P2 el tipo de vínculo que habilita crear guías y consultar
  catálogos: `empresaParaOperacion` y los catálogos filtran ACTIVO sin tipo,
  mientras la consulta empresarial de guías y el ABM vehicular exigen
  RESPONSABLE/ACTIVO. Documentar el criterio y su regresión antes de cambiar permisos.
- [ ] Resolver con el equipo el alcance restante de
  [ADR-0004](decisiones/0004-frontend-react-sobre-api-rest.md), aún Propuesta, y su
  contraste con el reparto JSF. ADR-0012 autoriza la pantalla vehicular acotada;
  ADR-0013 autoriza F12–F14 en SPA, sin aceptar el resto de ADR-0004.
- [ ] Precisar, si el requisito de cambios de viaje lo necesita, qué datos de una
  guía podrán modificarse y en qué estados. La interfaz actual ofrece creación y
  consultas, no edición; no introducir modificación/cancelación como obligación
  nueva sin confirmar el alcance y la política offline.

## 7. Coordinaciones que afectan a P2, sin trasladarle otros frentes

### P2-06 — Conflictos con eventos offline (F07/F08)

- [ ] Acordar con P3 y el responsable móvil la política ante un viaje cambiado en
  frontoffice y eventos registrados sin conexión: identidad de evento, reglas de
  aceptación/rechazo y resolución del conflicto.
- [ ] Registrar el acuerdo en ADR/contratos y derivar pruebas en ambos extremos.
- [ ] Mantener P3 como dueño de recepción, fotos, deduplicación, estado e historial;
  P2 aporta reglas/asignaciones y coordinación transaccional sobre sus entidades.
- [ ] Conservar los contratos de períodos/asignaciones consumidos por P4 para
  vincular pesajes; avisar antes de alterar esas reglas compartidas.

### F14 — Pantalla operativa del funcionario: interfaz a cargo de P2

**Asignación confirmada:** P2 implementa la pantalla por indicación explícita del usuario.

- [x] Elegir tecnología SPA existente para F12–F14, según ADR-0013.
- [ ] Acordar navegación y contratos de lectura para el funcionario;
  la consulta empresarial actual de guías exige vínculo responsable, no habilita
  por sí sola al funcionario.
- [ ] Coordinar datos de empresa/guía/asignación P2 con estado/historial P3 y
  posiciones/pesajes P4; no rehacer los adaptadores que esos frentes ya tienen.
- [ ] Implementar e integrar F12–F14 según el [plan por fases](plan-cierre-p2.md).
  La gestión de accesos de funcionarios P1 no sustituye esta área operativa.

La llamada de fin de viaje a P4 es pendiente P3 (F09), no un nuevo servicio P2.
Motor fiscalizador, casos, descargos y reportes corresponden a P5.

### F15 — Validación pública de autorizaciones: sin asignación explícita

- [ ] Confirmar si P2 será responsable de la consulta/reglas y P3 de la pantalla,
  o registrar el reparto que el equipo acuerde.
- [ ] Solo tras ese acuerdo, delimitar datos públicos y contrato de validación;
  no exponer los DTO/listados internos ni confundir esta función con estadísticas F16.

F15 se incluye como coordinación posible, NO como obligación ya asignada a P2.
Los ABM de rubros/tipos de carga, adjuntos vehiculares y opcionales no elegidos no
se agregan como faltantes obligatorios.

## 8. Orden propuesto y evidencia para cerrar

1. Resolver por bloque autorización, interfaz y contratos de P2-05; acordar reparto F15.
2. Validar F11 ya implementado y registrar su dependencia P3 aún abierta.
3. Validar F10/F12 implementados localmente y acordar C13 antes de construir F13.
4. Preparar consultas e interfaz F14 desde P2 con contratos P3/P4 acordados.
5. Integrar estado real, concurrencia inicio/baja y política offline con P3/móvil.
6. Acreditar CU2/ABM/JSF y los recorridos nuevos en integración; registrar evidencia final.

El desglose, dependencias y criterios por fase están en el
[plan de cierre P2](plan-cierre-p2.md); su secuencia es propuesta, no autorización
para implementar políticas pendientes. Para cerrar cada
bloque, registrar requisito/contrato, cambio, pruebas realmente ejecutadas,
entorno y resultado; mantener aparte las dependencias que continúan abiertas.
Para el agente siguen prohibidos los builds y las pruebas que compilan; la lista
de validación no constituye autorización para ejecutarlos.
