# Cierre operativo de P2 — línea base y aceptación

Fecha local: **2026-10-21** (America/Montevideo). Rama: `RoibethGarciaMac`.
HEAD observado: `695a7fc5ff966b0969dbb1ac3c01cb7e9da54a8f`.
Estado: **implementación preparada; operación NO acreditada**. Sin builds, despliegues ni pruebas ejecutadas.

**Referencia vigente al 2026-10-22:** este documento conserva la línea base y la
secuencia histórica de correcciones. Las fases 0–5 de su sección 5 NO son las
fases 0–7 del [plan de cierre vigente](../docs/plan-cierre-p2.md). Para el inicio
actual de fase 1, ver [registro de validación](p2-fase-1.md) y
[suite HTTP F11](p2-integracion.md#suite-http-f11--fase-1-del-plan-vigente).
Los puntos 1/2 de control más abajo describen el diagnóstico previo a los cambios,
no la autorización vehicular actual. ADR-0005 ya fue alineado documentalmente
en la fase 0 vigente; el criterio de tipo de vínculo en guías sigue pendiente.

## 1. Línea base conservada

Estado inicial recibido y contrastado con `git status --short`; TODOS estos cambios preceden esta tarea:

```text
 M carga-api/src/main/java/uy/edu/fing/tse/carga/api/contract/service/GuiaServicio.java
 M carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/modelo/Guia.java
 M carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/GuiaServicioBean.java
 M carga-front/src/App.tsx
 M carga-front/src/pages/frontoffice/MiCuenta.tsx
 M carga-front/src/tipos.ts
 M carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/GuiaRecurso.java
 M docs/contratos.md
?? carga-api/src/main/java/uy/edu/fing/tse/carga/api/dto/guia/GuiaEmpresaDTO.java
?? carga-api/src/main/java/uy/edu/fing/tse/carga/api/dto/guia/PaginaGuiasEmpresaDTO.java
?? carga-api/src/main/java/uy/edu/fing/tse/carga/api/exception/ConsultaGuiasInvalidaException.java
?? carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/servicio/GuiasEmpresaTest.java
?? carga-front/src/pages/frontoffice/GuiasEmpresa.tsx
?? pruebas/guias-empresa.md
```

El listado empresarial local NO se reemplaza. La fase 0 agregó únicamente este documento; los cambios posteriores autorizados se registran por separado, sin atribuirles las modificaciones previas. Al finalizar la implementación, la comparación SHA-256 conservó intactos los 14 archivos anteriores.

## 2. Propiedad, consumidores y puntos de consulta

La división vigente está en [frentes](../docs/frentes.md), y las pre/postcondiciones en el [caso crítico 2](../docs/GuiaCUCrit.md#caso-de-uso-crítico-2--gestionar-viajes-de-carga-crear-guía).

| Frente / límite | Archivos y consumidores observados | Conducta para el cierre |
| --- | --- | --- |
| P2: ABM y guías | Servicios EJB, REST y vistas JSF; interfaces/DTO en `carga-api` | Negocio y autorización en servidor; conservar firmas, formatos y transacciones. |
| P1: identidad y vínculos | [SolicitudesEmpresaLocal](../carga-api/src/main/java/uy/edu/fing/tse/carga/api/contract/service/SolicitudesEmpresaLocal.java), autenticación, principal y mappers compartidos | Consumir mecanismos existentes; no modificarlos ni crear otro flujo de login. |
| P3: portal/eventos | [NuevaGuia](../carga-front/src/pages/frontoffice/NuevaGuia.tsx), portal y consumidor móvil de guías | Autorizada solo la corrección puntual de coordenadas; no tocar eventos, sesión ni navegación compartida. |
| P4: pesajes/recorridos | Lee vehículo y período de guía según [contratos](../docs/contratos.md#pesajes-p4) | No cambiar esquema, períodos, identificadores, colas, nodos ni implementaciones. |
| P5: fiscalización | Usa guía, capacidad, habilitación y recorrido esperado según caso crítico 2 | No cambiar reglas ni resolver políticas de fiscalización. |
| Común | Contratos/ADR, Docker, CI y tablas compartidas | No editar sin consulta; no ejecutar builds ni instalar dependencias. |

Puntos de control y autorización:

1. [VehiculoServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/VehiculoServicioBean.java) permite administrador/ciudadano/chofer sin comprobar vínculo empresarial. [VehiculoRecurso](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/VehiculoRecurso.java) expone listado, alta, modificación y baja. Endurecerlo cambia acceso observable aunque se mantengan firmas/DTO.
2. [VehiculoVista](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/jsf/VehiculoVista.java) usa `listar(null)` en `@PostConstruct`: exigir empresa a no administradores requiere adaptar el consumidor P2 para no provocar una denegación al construir la vista. No cambiar seguridad P1 para ocultarlo.
3. El usuario autorizó explícitamente los puntos 1 y 2: administración global; ciudadanos/choferes solo con vínculo `RESPONSABLE/ACTIVO`, empresa explícita en consultas y autorización sobre empresa actual/destino antes de mutar. Se reutiliza [AccesoDenegadoMapper](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/AccesoDenegadoMapper.java) para `EJBAccessException` (401/403), sin editarlo. La aprobación cubre este arreglo P2, no cambios en identidad ni contratos compartidos.
4. El usuario también autorizó corregir `CampoNumerico` local de `NuevaGuia`: latitud −90..90 y longitud −180..180, opcionales, sin cambiar payload ni peso/volumen. Regresiones de navegador definidas antes del parche en [p2-coordenadas](p2-coordenadas.md); todavía no ejecutadas.
5. [ADR-0005](../docs/decisiones/0005-autorizacion-y-concurrencia-de-guias.md) describe `usuario.empresa_id`; contrato y código de guías usan vínculos activos. No restaurar el modelo anterior ni editar el ADR sin acuerdo.

## 3. Matriz de aceptación pendiente

Cada fila requiere registrar versión desplegada, identidad sintética, entrada, resultado y evidencia sin secretos. Los dos bloques de corrección anteriores están autorizados; cualquier impacto adicional fuera de P2 requiere otra consulta y pausa.

| Operación | Identidad / escenario | Resultado esperado | Evidencia necesaria |
| --- | --- | --- | --- |
| ABM JSF | Administrador interno | Alta/modificación/baja persistidas al recargar; AJAX correcto | Navegador y consulta posterior; [guía JSF](../docs/pruebas-abm-jsf.md). |
| Acceso JSF | Sin sesión, vencida, clave temporal, autoridad | Redirección/cambio de clave/403 conforme al adaptador vigente; sin error al construir vista | Navegación y postback/AJAX en WildFly. |
| Vehículos globales | Administrador | Conserva consulta y gestión global | Regresiones escritas y REST/JSF desplegados. |
| Vehículos propios | Responsable A activo (ciudadano o chofer) | Consulta por empresa A y gestión A; consulta global denegada | JUnit primero; después REST y consumidor JSF sin 500. |
| Vehículos ajenos | Responsable A sobre B; traslado A→B sin vínculo B | Denegación antes de mutar; registro permanece intacto | Regresión de estado y lectura independiente posterior. |
| Vínculo insuficiente | Sin vínculo, pendiente, revocado o no responsable | Sin acceso empresarial no autorizado | Regresión de permisos y EJB real con principal. |
| Crear guía válida | Responsable A; vehículo/chofer A habilitados | Guía y asignación confirmadas juntas; consultas posteriores coherentes | Creación REST y consultas responsable/chofer. |
| Crear guía inválida | Empresa/vehículo/chofer inactivos, empresa ajena, permiso/ITV vencidos | Rechazo seguro sin guía parcial | Casos separados con lectura posterior. |
| Carga/fechas | Sobrepeso/volumen, datos obligatorios, origen/destino, fin≤inicio | Rechazo según reglas actuales del servicio | JUnit y REST; conservar formato de errores publicado. |
| Solapamiento | Viaje vigente del mismo vehículo o chofer | Rechazo; guía previa intacta | Casos separados por recurso. |
| Guías del chofer | Chofer A y chofer B | Cada uno obtiene solo sus guías desde su principal | REST sin selector de chofer del cliente. |
| Listado empresarial | Responsables A/B; múltiples páginas y fechas iguales | A nunca ve B; orden `inicio DESC, id DESC`, total y paginación coherentes | [Escenarios del listado](guias-empresa.md), REST y SPA. |
| Históricos | Asignada/finalizada; chofer inactivo; guía sin asignaciones | Se incluyen sin deducir estado por fecha ni provocar 500 | Preparación controlada y consulta desplegada. |
| Coordenadas | Negativas, límites, fuera de rango y vacías | Rangos correctos; negativas conservadas; opcionales siguen opcionales | Navegador/payload y lectura posterior tras corrección autorizada. |
| Carrera vehículo | Dos solicitudes, mismo vehículo y distintos choferes | Exactamente una asignación compatible confirmada; sin guía parcial | Barrera de inicio, HTTP/transacciones independientes y lectura posterior. |
| Carrera chofer | Dos solicitudes, mismo chofer y distintos vehículos | Exactamente una asignación compatible confirmada; sin guía parcial | Igual procedimiento; no inferir locks desde mocks. |
| Frontera temporal | Mismo recurso: `[t0,t1)` y `[t1,t2)` | Ambas asignaciones consecutivas permitidas | REST y persistencia; repetir en zona horaria explícita. |
| Compatibilidad | Consumidores P3/P4/P5 de guía/período | Firmas/DTO/ISO-8601 y período conservados | Comparación estática y comprobaciones del equipo sin editar otros frentes. |

Las [pruebas unitarias de guías](../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/servicio/GuiaServicioBeanTest.java) existentes no demuestran roles/JTA ni exclusión concurrente real.

## 4. Datos y procedimiento de validación segura

- Equipo: usar despliegue y base AISLADOS, versión exacta de los cambios y datos sintéticos identificables por ejecución; no usar producción ni reutilizar credenciales reales.
- Identidades requeridas: administrador interno existente; funcionario autorizado para aprobar solicitudes; responsables A/B por OIDC; choferes A1/A2 y B1 con cédulas sintéticas coherentes con sus principales; usuario sin vínculo y variantes pendientes/revocadas.
- Preparar empresas A/B y vínculos mediante solicitudes y aprobación P1 existentes ([contrato de solicitudes](../carga-api/src/main/java/uy/edu/fing/tse/carga/api/contract/service/SolicitudesEmpresaLocal.java)); no insertar vínculos por SQL, crear cuentas internas de ciudadanos ni modificar P1 para facilitar pruebas.
- Registrar vehículos A1/A2 y B1, choferes y catálogos mediante mecanismos existentes. Usar capacidades conocidas, permiso/ITV cubriendo todo el intervalo de prueba y fechas futuras con zona explícita. Separar fixtures inválidos y evitar que un escenario contamine otro.
- Preparar históricos no generables por una operación pública SOLO mediante fixture controlado acordado para la base aislada; no agregar endpoints ni cambiar estados desde la UI para simular eventos P3.
- Para concurrencia, sincronizar dos clientes con barrera de inicio y recoger ambos resultados; no usar esperas arbitrarias como prueba. Comprobar asignaciones y ausencia de escrituras parciales mediante lecturas independientes del servidor/base aislada autorizada.
- No guardar tokens, contraseñas, cookies ni datos personales en evidencias. Registrar códigos, IDs sintéticos, conteos y correlación segura; limpieza del entorno a cargo del equipo sin modificar recursos ajenos.
- JSF conserva login interno administrativo: [ADR-0011](../docs/decisiones/0011-autenticacion-del-abm-jsf.md) NO agrega ingreso OIDC JSF. Probar ciudadanos/choferes por REST y, para EJB/JSF, solo con identidad de prueba autorizada del contenedor; no prometer un puente OIDC.

## 5. Progreso y condición de cierre

| Fase | Estado | Siguiente acción |
| --- | --- | --- |
| 0: alcance/línea base | Preparada en este documento | Conservar inventario y completar evidencia durante las fases siguientes. |
| 1: autorización vehículos | Implementada, ejecución pendiente | EJB con vínculo responsable activo y vista JSF con filtro propio; verificar 13 pruebas del servicio y 8 de coordinación de vista. |
| 2: formulario/pantallas | Coordenadas y adaptación JSF implementadas; navegador pendiente | Ejecutar los 8 escenarios de [p2-coordenadas](p2-coordenadas.md) y la matriz ABM; resto de pantallas sin cambios. |
| 3: pruebas P2 | Preparadas, sin ejecución | CU2: 33 pruebas (24 nuevas); 6 pruebas HTTP opt-in en [p2-integracion](p2-integracion.md). No se modificó E2E general ni CI. |
| 4: ejecución desplegada | Pendiente, equipo | Compilar/desplegar por su procedimiento y ejecutar matriz sobre esa versión. |
| 5: cierre documental | Inventario/procedimientos preparados; cierre final pendiente | Registrar evidencia de fase 4; reconciliación ADR-0005 sigue requiriendo acuerdo antes de editar fuentes compartidas. |

### Cambios posteriores a la línea base

- Autorización: [VehiculoServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/VehiculoServicioBean.java); conserva firmas y DTO, usa el DAO P1 sin editarlo y denegación `EJBAccessException` con mapper existente.
- Coordinación: [VehiculoVista](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/jsf/VehiculoVista.java) y [vehiculos.xhtml](../carga-web/src/main/webapp/vehiculos.xhtml); filtro empresarial, sin llamada global no administrativa y limpieza ante acceso denegado. No se ocultan fallas técnicas.
- Formulario: [NuevaGuia](../carga-front/src/pages/frontoffice/NuevaGuia.tsx), solo límites numéricos locales; payload, sesión y rutas intactos.
- Regresiones nuevas: [VehiculoServicioBeanAutorizacionTest](../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/servicio/VehiculoServicioBeanAutorizacionTest.java) (13) y [VehiculoVistaTest](../carga-web/src/test/java/uy/edu/fing/tse/carga/web/jsf/VehiculoVistaTest.java) (8). Se ampliaron las unitarias CU2 de 9 a 33, sin cambiar producción de guías.
- Integración nueva: [P2ContraDespliegueTest](../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/servicio/P2ContraDespliegueTest.java) (6), con fixtures y opt-in explícitos, sin llamadas HTTP predeterminadas ni nuevas dependencias. Deja cinco guías sintéticas si todos los casos pasan; no hace limpieza no autorizada.
- Procedimientos: este documento, [p2-coordenadas](p2-coordenadas.md) y [p2-integracion](p2-integracion.md).

### Comprobaciones estáticas realizadas

`git diff --check` sin errores; `vehiculos.xhtml` bien formado como XML; destinos locales de los enlaces de los tres documentos existentes; comparación de hashes de los 14 archivos previos sin diferencias. Se revisaron los diffs y las firmas usadas. Estas comprobaciones NO acreditan compilación, AJAX, permisos efectivos, transacciones ni concurrencia.

Sin builds ni runner frontend instalado por el agente. **Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.** Hasta satisfacer la matriz en el entorno desplegado: **implementación preparada, operación no acreditada**; bloqueos de otros frentes se consultan, no se corrigen unilateralmente.
