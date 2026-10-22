# Pendientes P2 — Núcleo de negocio y backoffice

**Fecha de corte:** 22 de octubre de 2026.
**Base:** `c7041f4` más los cambios locales de vehículos de ADR-0012.

## 1. Alcance y criterio de cierre

P2 es dueño del negocio de empresas, vehículos, choferes, creación/asignación de
guías y sus validaciones, según [frentes](frentes.md). Este documento desglosa los
pendientes del [inventario general](pendientes-funcionales-laboratorio-2026.md),
conservando sus identificadores F10–F15; no reasigna trabajo de otros frentes.
Las referencias a la letra provienen de ese inventario: no se hizo una nueva
lectura del PDF externo ni una auditoría exhaustiva en esta actualización.

- `[x]` significa **implementación presente**, no prueba superada ni despliegue.
- `[ ]` significa implementación, definición, coordinación o validación pendiente;
  se identifica el tipo en cada sección.
- No se ejecutaron aplicación, pruebas ni builds. Los escenarios del navegador y
  las garantías reales de roles/JTA/JPQL/bloqueos siguen por acreditar.
- No añadir obligaciones nuevas a partir de una propuesta: F15 y la pantalla F14
  requieren acordar dueño; la política de conflictos requiere acuerdo y ADR.

## 2. Resumen operativo

| Bloque | Tipo de pendiente | Resultado esperado para cerrar |
| --- | --- | --- |
| F10 — Perfil de empresa del responsable | Implementación propia P2, portal con P3 | Consultar/editar su empresa desde el portal, con autorización por pertenencia en servidor. |
| F11 — Vehículos del responsable | Validación P2 y dependencia P3 | Probar el ABM ya implementado e integrar el estado real del viaje y su concurrencia con baja. |
| F12 — Empresas para funcionario | Integración de acceso y presentación | Recorrido desde gub.uy hasta consultar/modificar empresas existentes sin retirar seguridad. |
| F13 — Vehículos/permisos/ITV para funcionario | Consulta y autorización de lectura | Permitir la consulta requerida sin conceder escritura de vehículos al funcionario por extensión accidental. |
| Guías y ABM existentes | Validación y coherencia de contratos | Acreditar reglas, aislamiento y concurrencia real; resolver discrepancias documentales y de criterio de vínculo. |
| Conflictos offline y estado del viaje | Coordinación P2/P3 | Acordar política y contrato; mantener P3 como dueño de eventos/estado/historial. |
| F14 / F15 | Participación o asignación por acordar | Proveer datos/reglas P2 cuando corresponda, sin asumir como propios todos los flujos. |

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

**Situación:** `EmpresaDTO` ya contiene `nombre`, `razonSocial`, `numeroEmpresa` y
`direccion`. `EmpresaServicioBean.modificar` admite funcionario/administrador,
no al responsable ciudadano. Mi empresa administra choferes y vehículos, no esos datos.

- [ ] Implementar la consulta del perfil de su empresa en Mi empresa.
- [ ] Implementar edición de los campos empresariales del requisito, reutilizando
  DTO/validaciones existentes y delimitando explícitamente los campos editables.
- [ ] Autorizar la modificación en EJB por principal y vínculo responsable activo
  de esa empresa, sin abrir la modificación de cualquier empresa al rol ciudadano.
- [ ] Conservar por separado las operaciones administrativas: editar el perfil no
  debe permitir bajas, alteración de vínculos o cambios de estado implícitos.
- [ ] Coordinar contrato y consumidor, y escribir primero pruebas positivas y de
  rechazo: empresa ajena, vínculo ausente/revocado, datos inválidos y número duplicado.
- [ ] Validar el flujo autenticado en servidor/navegador, no solo la visibilidad del formulario.

**Cierre:** el responsable puede consultar y guardar su perfil autorizado; otro
usuario no puede modificarlo alterando el ID ni invocando directamente REST.

### P2-02 / F12 — Gestión de empresas por funcionario

**Origen:** letra p. 6. **Coordinación:** P1 para acceso; P3 para portal.

**Situación:** existen operaciones de empresa autorizadas para funcionario y
vistas JSF. ADR-0011 no ofrece destino OIDC JSF; la SPA no tiene una pantalla de
gestión de empresas existentes para funcionario. La gestión de solicitudes y
vínculos de P1 no sustituye esa pantalla.

- [ ] Acordar el recorrido de interfaz: integrar las operaciones en el portal
  existente o coordinar un destino OIDC para JSF. No crear cuentas internas de
  funcionario como atajo ni aceptar una migración general sin acuerdo.
- [ ] Habilitar navegación autenticada para consultar y modificar empresas existentes.
- [ ] Integrar las acciones del ABM administrativo que correspondan al alcance P2,
  reutilizando el servicio existente y coordinando el alta con las solicitudes P1.
- [ ] Preservar el comportamiento existente de baja empresarial: desactivación,
  revocación de vínculos activos y auditoría en la misma operación de negocio.
- [ ] Probar permisos reales por actor, validaciones, errores y sesión vencida;
  documentar el recorrido de aceptación elegido.

**Cierre:** un funcionario que ingresó por gub.uy llega a las operaciones de
empresas que tiene autorizadas; no depende de una identidad administrativa distinta.

### P2-03 / F13 — Consulta de vehículos, permiso e ITV por funcionario

**Origen:** letra p. 6. **Coordinación:** P1 para permisos y P3 para presentación.

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

- [ ] Alinear [ADR-0005](decisiones/0005-autorizacion-y-concurrencia-de-guias.md),
  que todavía describe `usuario.empresa_id`, con el contrato y código actuales
  basados en `VinculoEmpresaUsuario`; no restaurar el esquema anterior.
- [ ] Acordar con P1/P2 el tipo de vínculo que habilita crear guías y consultar
  catálogos: `empresaParaOperacion` y los catálogos filtran ACTIVO sin tipo,
  mientras la consulta empresarial de guías y el ABM vehicular exigen
  RESPONSABLE/ACTIVO. Documentar el criterio y su regresión antes de cambiar permisos.
- [ ] Resolver con el equipo el alcance restante de
  [ADR-0004](decisiones/0004-frontend-react-sobre-api-rest.md), aún Propuesta, y su
  contraste con el reparto JSF. ADR-0012 solo autoriza la pantalla vehicular acotada.
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

### F14 — Pantalla operativa del funcionario: dueño por acordar

- [ ] Acordar quién integra la pantalla y qué consultas debe aportar P2.
- [ ] Coordinar datos de empresa/guía/asignación P2 con estado/historial P3 y
  posiciones/pesajes P4; no rehacer los adaptadores que esos frentes ya tienen.

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

1. Validar F11 ya implementado y coordinar su dependencia P3.
2. Completar F10 (perfil propio) y el recorrido funcionario F12/F13.
3. Resolver los criterios de autorización y documentos pendientes de P2-05.
4. Acreditar CU2/ABM/JSF en integración; cerrar la política offline con P3/móvil.
5. Acordar participación concreta en F14/F15 antes de implementar alcances nuevos.

Este orden es una propuesta, no una planificación aceptada. Para cerrar cada
bloque, registrar requisito/contrato, cambio, pruebas realmente ejecutadas,
entorno y resultado; mantener aparte las dependencias que continúan abiertas.
Para el agente siguen prohibidos los builds y las pruebas que compilan; la lista
de validación no constituye autorización para ejecutarlos.
