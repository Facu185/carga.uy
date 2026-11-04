# Pendientes funcionales del laboratorio 2026

**Fecha de corte:** 22 de octubre de 2026.

**Base de código:** revisión local sobre `c7041f4`, incluyendo los cambios locales
del ABM de vehículos del responsable (ADR-0012), todavía sin validación funcional.

**Alcance:** requisitos funcionales, responsabilidades P1–P5 e integraciones pendientes.

## 1. Fuentes y criterio de lectura

Este documento consolida el contraste de `Laboratorio_2026.pdf`, aportado por el
equipo fuera del repositorio, con el código y la documentación versionada. Las
páginas indicadas son las páginas físicas del PDF. Complementan la letra:

- [Distribución de frentes](frentes.md): fuente de asignaciones, no de progreso.
- [Contratos entre frentes](contratos.md): servicios, rutas e intercambios acordados.
- [Casos de uso críticos](GuiaCUCrit.md): flujos y alternativas del proyecto.
- [Decisiones arquitectónicas](decisiones/README.md): acuerdos y límites existentes.

Estados utilizados:

- **Pendiente:** no se encontró el flujo funcional en el código de este repositorio.
- **Parcial:** existen operaciones o pantallas, pero no cubren el recorrido completo
  exigido para el actor correspondiente.
- **Implementado `[x]`:** el código y la documentación del alcance indicado están
  presentes; NO significa probado, desplegado ni validado de punta a punta.
- **Por confirmar:** depende del repositorio móvil, no revisado, o de validación en ejecución.
- **Sin asignación explícita:** el reparto versionado no identifica específicamente
  un responsable; no significa que nadie lo haya tomado fuera de ese documento.

**Límites:** revisión estática, sin ejecutar aplicación, pruebas ni builds. La
existencia de tablas, interfaces, anotaciones o pruebas no acredita funcionamiento.
La aplicación móvil está en otro repositorio: NO se declara faltante por no estar aquí.
No se evalúan exhaustivamente requisitos no funcionales, infraestructura ni entregables.
Las coordinaciones propuestas abajo no aceptan políticas nuevas. Como actualización
explícita del reparto, el usuario asignó a P2 la interfaz operativa F12–F14 del
funcionario; los productores de datos P3/P4 conservan su responsabilidad.

## 2. Resumen por responsable

| Frente | Pendiente principal | Coordinación necesaria |
| --- | --- | --- |
| P1 | No se identificó un bloque funcional completo ausente de identidad; falta cerrar su integración con los recorridos JSF/OIDC que dependen de ella. | P2/P3 para acceso ciudadano y funcionario; P5 para uso de la auditoría existente. |
| P2 | Perfil empresarial editable por el responsable; gestión de empresas y consulta de permisos/ITV por funcionario; interfaz operativa F12–F14 asignada a P2. ABM vehicular del responsable implementado, con validación pendiente. Detalle: [pendientesP2](pendientesP2.md) y [plan por fases](plan-cierre-p2.md). | P1 para identidad/autorización; P3 para portal compartido, estado/historial y concurrencia con la baja; P4 para pesajes/recorridos. |
| P3 | Receptor central de eventos, fotos, deduplicación, estado e historial del viaje, y emisión del aviso de fin de viaje. | Aplicación móvil, P2 para conflictos y P4 para aviso de fin. |
| P4 | Integración final con emisores/consumidores de P3/P5; no rehacer ingesta, pesajes ni recorridos existentes. | P3 emite fin de viaje; P5 consume información para fiscalizar. |
| P5 | Motor de reglas, casos, notificaciones, descargos, resolución, parametrización y reportes gerenciales. | P4 para posiciones/pesadas; P1 para auditoría; P3 para inserción en el portal. |
| Sin asignación explícita | Validación pública de permisos y listados/estadísticas públicas. | Acordar responsable y actualizar el reparto. |

## 3. Fiscalización y reportes: P5

### F01. Motor automático de detección — pendiente

**Fuente:** letra, pp. 6–7. **Responsable:** P5.

- [ ] Detectar exceso de carga: comparar el pesaje con el peso del vehículo más
  su capacidad de carga, aplicando la tolerancia configurable.
- [ ] Detectar actividad con Permiso Nacional de Circulación o ITV vencidos.
- [ ] Detectar actividad de Tracking o pesaje sin viaje asignado y en curso.
- [ ] Detectar desvío respecto del recorrido esperado; definir y justificar el criterio.
- [ ] Ejecutar las evaluaciones asíncronamente sin degradar la recepción de eventos.
- [ ] Generar el caso con evidencia y evitar duplicados por reprocesamiento del mismo hecho.
- [ ] Conectar el procesamiento de posiciones y el consumo de pesadas con estas reglas.

**Evidencia:** [PosicionMDB](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/jms/PosicionMDB.java)
valida, deduplica y persiste posiciones; no implementa la detección. El
[contrato de mensajería](contratos.md) identifica la cola `pesadas` sin consumidor P5.
Las tablas preparadas en [V1](../carga-ejb/src/main/resources/db/migration/V1__esquema_base.sql)
no sustituyen el motor.

El cálculo de longitud del recorrido real en
[RecorridoDAO](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/persistencia/RecorridoDAO.java)
NO equivale a medir desvío respecto de un recorrido esperado.

### F02. Notificación a la empresa — pendiente

**Fuente:** letra, p. 7. **Responsable:** P5.

- [ ] Notificar a la empresa transportista cuando se genera un caso.
- [ ] Integrar la notificación con el estado del caso y su trazabilidad.

No se encontró el flujo implementado. El canal y las condiciones concretas de
notificación requieren acuerdo; este documento no elige correo, push u otro mecanismo.

### F03. Consulta de infracciones y descargos de la empresa — pendiente

**Fuente:** letra, p. 5. **Responsable:** P5; coordinación de portal con P3.

- [ ] Consultar las infracciones de los vehículos y viajes de la propia empresa.
- [ ] Acceder al detalle de la evidencia que originó cada caso.
- [ ] Presentar un descargo dentro del plazo establecido.
- [ ] Controlar pertenencia empresarial y plazo en el servidor.
- [ ] Registrar el descargo e integrar el cambio de estado correspondiente.

Existe la tabla `descargo` en V1, pero no se encontraron servicios, recursos ni
pantallas que completen este caso de uso. Falta acordar el plazo y su vencimiento.

### F04. Bandeja y resolución de casos por el funcionario — pendiente

**Fuente:** letra, pp. 6–7. **Responsable:** P5.

- [ ] Bandeja de casos detectados automáticamente.
- [ ] Consulta de evidencia y revisión del descargo presentado.
- [ ] Confirmación o desestimación de la infracción con fundamento obligatorio.
- [ ] Control de transiciones del ciclo `DETECTADA → NOTIFICADA → EN_DESCARGO →
  CONFIRMADA/DESESTIMADA`; impedir transiciones no previstas.
- [ ] Auditoría de creación, cambios de estado y resolución, con actor, acción,
  fundamento y fecha/hora.

La tabla `caso_fiscalizacion` y sus valores de estado existen, pero no la gestión
del ciclo. La infraestructura y las acciones de auditoría ya están preparadas en
[AuditoriaBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/AuditoriaBean.java)
y [AccionAuditada](../carga-api/src/main/java/uy/edu/fing/tse/carga/api/contract/annotation/AccionAuditada.java).
Falta integrarlas desde P5, no crear otra pista independiente.

El tratamiento del vencimiento sin descargo sigue pendiente de acuerdo en el CU6;
no se propone una transición adicional como si ya estuviera aceptada.

### F05. Parametrización de reglas sin redespliegue — pendiente

**Fuente:** letra, p. 8. **Responsable:** P5 por propiedad de `parametro_regla`;
falta explicitar la implementación de su pantalla administrativa.

- [ ] Consultar y modificar los umbrales con rol administrador.
- [ ] Permitir configurar al menos tolerancia de exceso de carga y de desvío.
- [ ] Validar valores y aplicar los cambios al motor sin redesplegar.

[V2](../carga-ejb/src/main/resources/db/migration/V2__datos_iniciales.sql) precarga
`exceso_carga.tolerancia_kg` y `desvio_ruta.tolerancia_m`. No se encontró un flujo
de administración ni un motor que los consuma: las semillas no satisfacen el requisito.

### F06. Dashboard y reportes para autoridades — pendiente

**Fuente:** letra, pp. 7–8. **Responsable:** P5.

- [ ] Cantidad de empresas y vehículos habilitados.
- [ ] Viajes realizados por período.
- [ ] Volumen de carga transportado por rubro.
- [ ] Infracciones detectadas por tipo.
- [ ] Tasa de infracciones confirmadas sobre detectadas.
- [ ] Filtros por tipo de carga, ubicación geográfica y período de tiempo.
- [ ] Exportación de los reportes en al menos dos formatos; CSV y PDF son ejemplos,
  no una elección de formatos acordada por este documento.
- [ ] Acceso de autoridades a las consultas y reportes correspondientes.

**Actualización local (2026-11-03):** existe el tablero
[ReporteActividad](../carga-front/src/pages/backoffice/ReporteActividad.tsx),
con actividad agregada por creación de guías y calendario Montevideo (ADR-0020).
Se añadió por solicitud confirmada el filtro opcional de tipo de carga **solo a
actividad**; fiscalización mantiene todos los tipos y casos sin guía por período
de detección, con alcance explícito en pantalla y CSV/PDF. No se equiparan guías
registradas a viajes realizados ni empresas con guías a empresas habilitadas.
Es entrega parcial, sin pruebas ejecutadas ni aceptación integrada: no se marcan
cerrados F06, ubicación, filtros globales o indicadores de habilitación. Ver
[matriz de aceptación](../pruebas/tablero-autoridad.md) y
[seguimiento](../odd/tasks/autoridad-filtro-tipo-carga.md). No sustituir el tablero
por Grafana ni por la descarga de guías del chofer.

**Ampliación local (2026-11-04):** se añadió modo Viajes finalizados por instante
del evento FIN aceptado, separado del registro por creación (predeterminado),
con los mismos filtros de período/tipo de carga. Una guía por viaje, sin inferir
fin de fechas previstas/estado ni usar recepción offline. Los eventos tardíos
pueden actualizar períodos anteriores. Pantalla/CSV/PDF distinguen fecha aplicada
y fiscalización general. [ADR-0023](decisiones/0023-reporte-viajes-finalizados-autoridad.md)
y [seguimiento](../odd/tasks/autoridad-viajes-finalizados.md). Código/pruebas
presentes sin compilación/ejecución: el requisito de viajes realizados queda
pendiente de aceptación; no acredita duración ni carga física transportada.

## 4. Eventos y ejecución del viaje: P3

### F07. Recepción y sincronización de eventos en el central — código presente, aceptación pendiente

**Fuente:** letra, pp. 5–6; CU3. **Responsable:** P3.

- [x] API REST para recibir inicio, fin, carga parcial, descarga parcial e incidente.
- [x] Recepción y persistencia de la fotografía asociada al incidente.
- [x] Validación en código de que el chofer puede registrar eventos sobre ese viaje.
- [x] Persistencia del evento y deduplicación por identificador global y contenido normalizado.
- [ ] Manejo de la sincronización diferida según la política de conflictos acordada.

La entrega P3 incorporó `EventoViajeLocal`/`EventoViajeBean`/REST y V10; ya no se
considera un flujo ausente del central. El usuario aprobó aplicar la política
central de [ADR-0019](decisiones/0019-registro-de-eventos-de-viaje.md): implementada
localmente con regresiones escritas, sin ejecución. Coordinación con Federico,
conducta del móvil y [aceptación real](../pruebas/p2-p3-eventos-conflictos.md) pendientes.
`[x]` acredita presencia de código, no seguridad, concurrencia ni sincronización ejecutadas.

### F08. Estado real e historial de ejecución — pendiente

**Fuente:** letra, p. 6; CU3 y reparto P3. **Responsable:** P3.

- [x] Actualizar en código el estado del viaje a partir de eventos válidos.
- [x] Consultar el historial del chofer sobre su guía y el estado resultante.
- [ ] Entregar lectura C14-E para funcionario e integrarla en F14 P2.
- [ ] Mantener coherencia ante reenvíos y sincronización diferida.

[Guia](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/modelo/Guia.java)
se crea en estado `ASIGNADA`; P3 aplica las transiciones mediante eventos y ofrece
historial en GET `/eventos-viaje?guiaId=` solo para CHOFER y guía propia. C14 P2
consulta el estado persistido, no sustituye ese historial. El funcionario aún no
tiene una lectura autorizada C14-E; no se amplía el GET/POST por aprobar ADR-0019.

### F09. Conectar el fin de viaje con la obtención de pesajes — pendiente

**Fuente:** CU3/CU4, contratos y ADR-0010. **Responsable del emisor:** P3.

- [x] Al registrar el fin, invocar en código `AvisoFinDeViajeLocal.avisar(...)` dentro de la
  transacción correspondiente.
- [ ] Verificar el recorrido completo evento de fin → aviso → consulta de pesajes.

P4 ya proporciona [AvisoFinDeViajeBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/AvisoFinDeViajeBean.java)
y [FinDeViajeMDB](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/jms/FinDeViajeMDB.java).
`EventoViajeBean` ya invoca el adaptador al FIN; solo una inserción nueva produce
efectos. Consultar [ADR-0010](decisiones/0010-integraciones-de-p4-con-p3-y-p5.md):
no hace falta rehacer P4. Rollback y cadena completa siguen pendientes de acreditar.

## 5. Funcionalidades de empresa y funcionario: implementación y pendientes

| ID | Requisito y fuente | Qué falta cerrar | Propiedad y coordinación |
| --- | --- | --- | --- |
| F10 | Perfil de empresa del responsable; letra p. 5. | **Implementado localmente, sin validación ejecutada:** `/mi-empresa/perfil`, GET/PUT específicos y autorización RESPONSABLE/ACTIVO/empresa activa, sin exponer campos administrativos. C10/ADR-0014 aprobados por el usuario. Falta acreditar servidor/navegador e integración; ver [aceptación](../pruebas/p2-perfil-empresa.md). | P2 implementa negocio y perfil acotado aprobado; coordina identidad P1 e integración del portal P3. |
| F11 | ABM de vehículos del responsable; letra p. 5. | **Implementado en código** en `/mi-empresa/vehiculos`: datos, permiso/ITV, baja protegida y reactivación. Falta validar ejecución e integración con el estado real del viaje; ver checklist inferior. No requiere agregar OIDC JSF para este recorrido React. | P2 es dueño del ABM; P3 debe mantener EN_CURSO/FINALIZADA y coordinar la exclusión con la baja. |
| F12 | Gestión de empresas por funcionario; letra p. 6. | **Implementado localmente, sin validación ejecutada:** entrada desde Mi cuenta → `/funcionario/empresas`, búsqueda/detalle, edición y baja C12; bandeja responsable P1 reutilizada, sin alta paralela. Falta aceptación compartida, coordinación de alta P1 y acreditar permisos/errores/rollback real; ver [escenarios F12](../pruebas/p2-funcionario-empresas.md). | P2 es dueño del negocio y de la interfaz; coordinar acceso con P1 y navegación compartida con P3. |
| F13 | Listado de vehículos por empresa y vigencia de permisos/ITV para funcionario; letra p. 6. | C13/ADR-0016 confirmado por el usuario; consulta EJB/REST y pantalla local de solo lectura con paginación/filtro y fecha servidor America/Montevideo. No amplía permisos ABM. Validación real y aceptación integrada pendientes. | P2 para interfaz/consulta/datos, coordinado con P1 para autorización y P3 para navegación. |
| F14 | Monitoreo de viajes en curso/realizados y pesajes; letra p. 6. | Integrar una vista operativa del funcionario con estado real e historial del viaje y sus pesajes. El mapa y las posiciones no sustituyen el ciclo del viaje. | P2 integra la pantalla, por asignación explícita del usuario; P3 conserva estado/historial y P4 posiciones/pesajes. |

### F11. Alcance resuelto en código y cierre pendiente

- [x] Pantalla de vehículos dentro de Mi empresa, accesible con el ingreso
  ciudadano existente y navegación compartida con Choferes.
- [x] Empresa fija desde el único vínculo `RESPONSABLE/ACTIVO`; sin selector
  empresarial ni confianza en IDs del navegador para autorizar operaciones.
- [x] Alta, modificación y listado de vehículos activos y dados de baja, con
  matrícula, marca/modelo, peso, capacidades kg/m³, permiso e ITV; sin adjuntos.
- [x] Baja lógica rechazada por el servidor si el vehículo tiene una guía
  `EN_CURSO`, tanto por DELETE como por PUT con `habilitado=false`.
- [x] Reactivación específica sin sobrescribir datos técnicos, permiso ni ITV.
- [x] Regresiones JUnit escritas para reglas, permisos y fechas nulas, más pruebas
  de ruta declarada/delegación REST; alcance registrado en ADR-0012 y contratos.
- [ ] Ejecutar las pruebas y validar el recorrido en WildFly/PostgreSQL y navegador.
  No se verificó el ciclo rojo/verde por la restricción de builds.
- [ ] Integrar con P3 el mantenimiento real de `EN_CURSO`/`FINALIZADA` y comprobar
  inicio de viaje concurrente con baja: mismo bloqueo y verificación de habilitación.
- [ ] Acreditar regresión de JSF y creación de guías, aislamiento empresarial,
  respuesta HTTP de reactivación y manejo de sesión vencida.

Detalle de comprobación: [pruebas del ABM vehicular](pruebas-abm-vehiculos.md).
Pendientes propios de P2 y dependencias: [pendientesP2](pendientesP2.md).

Evidencias:

- [EmpresaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/EmpresaServicioBean.java):
  modificación restringida a funcionario/administrador.
- [VehiculoServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/VehiculoServicioBean.java):
  ABM, autorización empresarial, control EN_CURSO y reactivación; funcionario
  sigue sin estar incluido en los roles del servicio.
- [Empresa.tsx](../carga-front/src/pages/frontoffice/Empresa.tsx) y
  [VehiculosEmpresa.tsx](../carga-front/src/pages/frontoffice/VehiculosEmpresa.tsx),
  con sus [rutas React](../carga-front/src/App.tsx): gestión de choferes y vehículos,
  todavía sin perfil empresarial editable.
- [ADR-0012](decisiones/0012-abm-vehiculos-del-responsable.md): alcance confirmado
  del ABM vehicular, reglas de baja/reactivación y límites de la integración P3.
- [ADR-0011](decisiones/0011-autenticacion-del-abm-jsf.md): login JSF de cuentas internas,
  sin destino OIDC JSF para ciudadanos/funcionarios.
- [Panel P4](../carga-web/src/main/webapp/panel.xhtml): mapa de flota y consulta de pesajes existentes.

**No declarar inexistentes estos ABM ni sustituirlos por iniciativa propia.** Se
debe acordar cómo cerrar acceso y presentación, conservando autorización en servicios.

## 6. Funciones sin asignación explícita

| ID | Función pendiente | Fuente | Reparto sugerido, no acordado |
| --- | --- | --- | --- |
| F15 | Validación pública de permisos/autorizaciones de empresas transportistas para quienes contratan sus servicios. Falta consulta pública funcional y pantalla. | Letra, pp. 2 y 5. | P2: consulta y reglas; P3: interfaz pública. |
| F16 | Listados de información pública y estadísticas agregadas. No se encontró publicación funcional de estos datos. | Letra, pp. 2 y 5; protección de datos, pp. 10–11. | P5: agregaciones; P3: publicación en el portal. |

- [ ] Confirmar si alguien ya tomó F15/F16 fuera de la documentación.
- [ ] Acordar responsables explícitos y actualizar el reparto.
- [ ] Definir qué se publica y cómo se agrega para no identificar empresas,
  vehículos o choferes en los listados estadísticos públicos.
- [ ] Distinguir el alcance de la validación pública de autorizaciones del de los
  listados estadísticos; acordar los campos de cada consulta sin publicar datos internos indiscriminadamente.

El portal ciudadano genérico de P3 y los reportes para autoridades de P5 no
constituyen una asignación específica de estas dos funciones públicas.

## 7. Móvil: pendiente de confirmar en el otro repositorio

Los siguientes puntos NO se clasifican como ausentes; falta contrastar el código móvil:

| Verificación | Fuente | Relación con este repositorio |
| --- | --- | --- |
| Autenticación gub.uy de la aplicación. | Letra p. 5. | El central tiene inicio/callback y destino móvil; falta confirmar el cliente. |
| Consulta de guía asignada y disponibilidad local después de descargarla. | Letra pp. 5–6. | Existe `GET /guias/mis-guias`; falta confirmar consumo y almacenamiento móvil. |
| Registro de inicio, fin, cargas/descargas e incidentes con foto. | Letra p. 5. | Confirmar interfaz móvil; receptor central pendiente F07. |
| Persistencia local sin conectividad y sincronización al reconectar. | Letra p. 6. | Confirmar cola móvil y compatibilidad con F07/F08. |
| Identificador generado en el dispositivo y reenvío sin duplicados. | Letra p. 6. | Confirmar generación móvil; deduplicación central pendiente F07. |
| Manejo de conflictos con cambios del frontoffice. | Letra p. 6. | Confirmar política y aplicación en ambos extremos; no basta la cola local. |

La referencia central para las guías es
[GuiaRecurso](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/GuiaRecurso.java);
para identidad, [IngresoGubUyRecurso](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/IngresoGubUyRecurso.java).
La asignación documentada del flujo de eventos corresponde a P3; el reparto interno
del repositorio móvil no fue verificado.

## 8. Definiciones y coordinación pendientes

No resolver silenciosamente estas políticas dentro de una implementación:

| Definición pendiente | Frentes involucrados | Motivo |
| --- | --- | --- |
| Aceptación compartida de política central y entrega móvil ante conflictos offline. | Federico con Roibeth (P3/P2). | ADR-0019 aprobado por el usuario e implementado localmente; conducta móvil y pruebas reales pendientes. No autoriza edición/cancelación de guías. |
| Plazo de descargo y comportamiento al vencer sin presentación. | P5. | CU6 deja el comportamiento exacto por definir. |
| Criterio y justificación del desvío de ruta y representación del recorrido esperado. | P5, con datos de P2/P4. | Exigido por la letra; longitud recorrida no lo resuelve. |
| Canal y condiciones de notificación del caso. | P5; coordinación con portal/móvil cuando corresponda. | Falta concretar el flujo sin imponer un canal no acordado. |
| Integración de los recorridos de empresa y funcionario (F10/F12/F13). | P1/P2; P3 para portal. | F10/F12/F13 implementados localmente, pendientes de validación compartida; F12 integra entrada Mi cuenta sin cambiar callback. ADR-0013/0014/0016 acotan SPA conservando JSF; ADR-0004 sigue Propuesta. Alta P1 y C14 pendientes en el [registro de fase 0](acuerdos-cierre-p2-fase-0.md). F11 mantiene ADR-0012. |
| Validación de inicio de viaje concurrente con baja de vehículo. | P3 y P2. | Inicio local ya toma Vehículo → Guía y revalida baja actual; DELETE/PUT comparten el vehículo. Ejecutar carreras y rollback reales; no inferir estado por fechas. |
| Contratos e integración de la pantalla operativa del funcionario, ya asignada a P2. | P2/P3/P4, con P1 para acceso; P5 si se acuerda integrar casos. | P2 compone la interfaz; hay que acordar lecturas y disponibilidad de productores sin trasladar sus servicios a P2. |

Las políticas nuevas de negocio o arquitectura deben acordarse y registrarse en
los contratos/ADR pertinentes antes de depender de ellas. Este inventario no es un ADR.

## 9. Base existente que no debe contarse como faltante desde cero

- **P1:** OIDC, perfil ciudadano, consulta SOAP PDI con ingreso degradado, cuentas
  internas, asignación de roles, LDAP y pista de auditoría.
- **P2:** ABM base de empresas/vehículos/choferes, creación y asignación de guías,
  validaciones de capacidad/vigencias y consultas de guías de chofer/empresa.
  F11 incorpora pantalla ciudadana en Mi empresa, baja lógica protegida y
  reactivación; implementación presente, validaciones funcionales aún pendientes.
- **P4:** Balanzas, ingesta Tracking mediante JMS, deduplicación, recorridos
  ordenados, consultas PostGIS, monitoreo y administración de nodos.
- **Rubros y tipos de carga:** la letra, p. 8, los declara precargados y NO exige
  funcionalidad de gestión. No agregar un ABM como supuesto faltante obligatorio.
- **Opcionales:** el reparto elige LDAP y PostGIS. Los demás opcionales de la letra
  no se incorporan a este inventario como obligaciones pendientes.

Se acredita presencia de implementación, no funcionamiento, cobertura ni integración
completa. Tampoco se deducen viajes realizados solo porque terminó su fecha prevista.

## 10. Orden sugerido de cierre y validaciones pendientes

Prioridad propuesta, no planificación aceptada por el equipo:

1. Cerrar F07/F08 y el contrato con móvil, y conectar F09 con P4.
2. Cerrar el recorrido P5: detección → notificación → descargo → resolución,
   con parametrización, deduplicación y auditoría.
3. Completar F10 y F12–F14; validar F11 ya implementado e integrarlo con P3.
   Desglose de trabajo P2 en [pendientesP2](pendientesP2.md).
4. Completar reportes F06 y asignar explícitamente F15/F16.
5. Confirmar móvil y verificar los flujos completos en un entorno de integración.

Validaciones funcionales todavía por acreditar:

- [ ] Reenvío de eventos y pesadas sin duplicar eventos de viaje ni casos.
- [ ] Transiciones de casos válidas y rechazo de las no previstas, con auditoría.
- [ ] Acceso por actor y aislamiento entre empresas, incluidos los recorridos JSF/OIDC.
- [ ] ABM vehicular del responsable: baja protegida, reactivación, vigencias y
  concurrencia con inicio de viaje; implementación F11 no equivale a prueba superada.
- [ ] Fin de viaje conectado con pesajes y fiscalización.
- [ ] Operación offline y resolución de conflictos entre móvil y central.
- [ ] Reportes con filtros, indicadores y dos formatos de exportación.
- [ ] Cambio de umbrales sin redespliegue y aplicación efectiva en las reglas.

Esta actualización documental incorpora los cambios de código locales de F11,
contrastados estáticamente. Durante esta actualización solo se editó documentación;
no se ejecutaron pruebas ni builds y no se certifica funcionamiento ni cumplimiento
de requisitos no funcionales como rendimiento, cobertura o despliegue.
