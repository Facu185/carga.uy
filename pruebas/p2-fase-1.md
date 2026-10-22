# Fase 1 P2 — Registro inicial de validación

**Fecha:** 2026-10-22 (America/Montevideo).
**Base:** `c3bc92495d5b04c88f0fa26211f3494db237e2f8` más cambios locales de
fase 0 y la suite F11 indicada abajo; no hay un nuevo commit ni versión desplegada acreditada.
**Estado:** iniciada; preparación de pruebas y contraste estático, **sin ejecución funcional**.

Corresponde a la [fase 1 del plan vigente](../docs/plan-cierre-p2.md#fase-1--acreditar-la-base-existente-sin-rehacer-f11).
La solicitud de iniciar esta fase no aprueba D0-04 ni los contratos/políticas
pendientes del [registro de fase 0](../docs/acuerdos-cierre-p2-fase-0.md).
No se rehace F11 ni se implementan F10/F12/F13/F14 en esta entrega.

## 1. Material reutilizado y alcance de esta pasada

- [Matriz P2](p2-cierre.md): aceptación, identidades y aislamiento; su numeración
  histórica de fases no corresponde a la del plan vigente.
- [Integración aislada](p2-integracion.md): suite CU2 existente, opt-in y evidencia.
- [ABM vehicular](../docs/pruebas-abm-vehiculos.md): diez escenarios de servidor/navegador.
- [Regresión JSF](../docs/pruebas-abm-jsf.md): ingreso, postbacks/AJAX, roles y ABM.

Se contrastaron `VehiculoRecurso`, `VehiculoServicioBean`, `VehiculoDTO`, entidad
vehicular y tests vecinos. También se localizaron servicios/REST de empresas y
choferes y sus pruebas disponibles: esto no acredita su ABM persistido ni supone
una auditoría independiente o exhaustiva.

**Brecha de evidencia identificada:** `VehiculoRecursoTest` solo comprueba ruta y
delegación; no ejercita 204 ni transporte. `P2ContraDespliegueTest` cubre CU2,
pero no baja/reactivación F11. Se añade
[P2VehiculosContraDespliegueTest](../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/servicio/P2VehiculosContraDespliegueTest.java)
con cinco tests HTTP opt-in, sin nuevas dependencias, cambios productivos ni
fixtures compartidas con las carreras CU2.

## 2. Registro por escenario

Todos los resultados funcionales siguientes están **NO EJECUTADOS**. “Escrita”
describe código de prueba, no un resultado satisfactorio ni cobertura medida.

| ID | Escenario / criterio | Preparación disponible | Resultado y pendiente |
| --- | --- | --- | --- |
| F1-01 | Alta 201, edición 200, baja lógica 204, edición inactiva sin reactivar, reactivación repetible 204 sin cuerpo. | Nuevo test `altaEdicionDuplicadoBajaYReactivacionPersistenSinRenovarVigencias`; lecturas posteriores y comparación de campos. | NO EJECUTADO; equipo despliega y ejecuta HTTP. |
| F1-02 | Matrícula duplicada y peso negativo no dejan altas/cambios parciales observables. | Incluidos en F1-01; compara toda la flota antes/después del rechazo 400. | NO EJECUTADO; otros bordes de entrada siguen en matriz vehicular y unitarias. |
| F1-03 | Reactivar no renueva fechas ni completa datos heredados. | Fechas vencidas en F1-01; test `reactivarHeredadoConFechasNulasConservaTodosSusOtrosDatos` con fechas/volumen nulos. | NO EJECUTADO; reprovisionar heredado dado de baja antes de repetir. |
| F1-04 | EN_CURSO del mismo vehículo rechaza DELETE y PUT sin alterar campos, para responsable y administrador. | Test `guiaPersistidaEnCursoImpideDeleteYPutInclusoAlAdministrador`; verifica guía y vehículo persistidos antes/después. | NO EJECUTADO; depende de fixture acordada, no de cambiar estados desde la suite. |
| F1-05 | Empresa ajena y falta de sesión rechazan GET/POST/PUT/DELETE/reactivar; traslado a empresa no autorizada tampoco modifica. | Test `empresaAjenaYSinSesionRechazanAccesoDirectoSinMutaciones`; 403/401 y lectura administrativa de control. | NO EJECUTADO. |
| F1-06 | Vínculo revocado e identidad solo FUNCIONARIO no permiten lectura ni mutaciones vehiculares actuales. | Test `vinculoRevocadoYRolFuncionarioNoHabilitanLecturaNiMutacionesVehiculares`; rol y vínculo comprobados por contratos P1 de lectura. | NO EJECUTADO. La futura consulta F13 requiere su contrato separado. |
| F1-07 | EN_CURSO de otro vehículo y estados CREADA/ASIGNADA/FINALIZADA no bloquean baja. | Regla y dobles existentes; escenarios 5/9 de la matriz vehicular. | NO EJECUTADO en contenedor/base; no cubierto por nueva suite HTTP. |
| F1-08 | Baja excluye vehículo al asignar guías; reactivación no elude permiso/ITV. | CU2 unitario y escenarios 6/10 de la matriz vehicular. | NO EJECUTADO integrado; la suite F11 no crea guías. |
| F1-09 | Carga/vacío/error, teclado/foco, sesión vencida, doble envío y respuestas tardías. | Matriz vehicular 1/2/8/10 y procedimientos JSF. | NO EJECUTADO en navegador; sin runner frontend instalado. |
| F1-10 | ABM empresarial y de choferes, revocación/auditoría de baja empresarial y regresión JSF. | Matriz P2, pruebas JSF y tests existentes; `EmpresaServicioBeanSeguridadTest` comprueba anotaciones de lectura, no el ABM ni permisos efectivos. | NO EJECUTADO; registrar cada alta/edición/baja/reactivación aplicable, rechazo y efecto persistido. |
| F1-11 | Asignaciones simultáneas incompatibles; rollback/ausencia de parciales. | Suite CU2 opt-in existente y pruebas de servicio. | NO EJECUTADO; seleccionar suite y fixtures independientes. |
| F1-12 | Carrera inicio real P3 vs baja DELETE/PUT. | Dependencia registrada ADR-0012 y fase 6. | PENDIENTE DE INTEGRACIÓN P3; la nueva suite no inicia ni finaliza viajes. |

## 3. Ejecución pendiente y condiciones de seguridad

El equipo prepara un despliegue aislado de la versión exacta y configura las
fixtures/variables de [integración F11](p2-integracion.md#suite-http-f11--fase-1-del-plan-vigente).
El agente no ejecutó compilación, tests, HTTP ni contenedores. La exclusión
`integracion`, el opt-in propio de F11 y la confirmación de aislamiento evitan
llamadas predeterminadas; no prueban por sí solos que un destino sea desechable.

Los tests hacen solicitudes y lecturas independientes. Si se ejecutan, permitirán
observar status, serialización, denegaciones y datos visibles por esas APIs;
no inspeccionan todas las tablas ni prueban exhaustivamente JTA/locks/rollback.
F1-12 requiere transacciones reales coordinadas con el productor P3.

La suite deja un vehículo sintético nuevo y el heredado reactivado si todo pasa.
Ante fallo puede dejar escrituras confirmadas; no hay limpieza automática ni
reintentos de mutaciones. La recuperación del entorno queda a cargo del equipo.

## 4. Defectos y evidencia

**No hay defectos funcionales reproducidos por ejecución en esta pasada.** Se
identificó una brecha de pruebas y se preparó su cobertura; no se declara ausencia
de defectos ni se corrige lógica sin una regresión previa.

Para un defecto que encuentre el equipo, registrar: ID F1, severidad/prioridad,
versión exacta, alias de fixture, precondición, solicitud/acción mínima, esperado,
observado, efectos persistidos y referencia de correlación/informe. Sin tokens,
cookies, datos personales ni cuerpos completos. Añadir la regresión antes del fix.

| Dato de ejecución funcional | Valor actual |
| --- | --- |
| Versión desplegada / WildFly / PostgreSQL | No aportada ni comprobada. |
| Entorno aislado / identidades / fixtures | Requisitos documentados; no provisionados por el agente. |
| Informe JUnit / navegador / HTTP | No generado; tests y escenarios sin ejecutar. |
| Resultado F11 | Implementación existente; validación funcional pendiente, no cierre integrado. |

Las comprobaciones estáticas se registran en la entrega: no equivalen a tests
verdes, compilación ni aceptación del usuario. **Pruebas escritas, ciclo rojo/verde
no ejecutado por restricción de builds.** La fase 1 permanece abierta.

## 5. Comprobaciones estáticas realizadas en esta entrega

- `git diff --check`: sin errores.
- Seis documentos de esta fase: UTF-8/LF y sin espacios finales; 79 enlaces locales
  y seis anclas comprobados contra archivos y encabezados existentes.
- Revisión textual de la suite: cinco métodos `@Test`, 13 variables documentadas,
  opt-in propio anterior a la creación del cliente HTTP y delimitadores Java balanceados.
  Esto NO valida sintaxis Java completa, tipos ni ejecución del opt-in.
- Sin cambios en `src/main`, POM, dependencias frontend o CI respecto del estado
  inicial de esta fase; la preparación no modifica políticas pendientes de fase 0.
- SHA-256 del archivo de suite revisado:
  `5deb0ab8d56de163af61a5cb3f7d738b2b4a88ee8d017f98d3dca5e8d42f9ddf`.
  El equipo debe registrar además el artefacto realmente desplegado; este hash
  identifica la fuente preparada, no un EAR compilado o prueba superada.
