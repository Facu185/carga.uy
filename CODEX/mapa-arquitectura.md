# Mapa de arquitectura y límites

Mapa de navegación contrastado con POM, archivos fuente y configuración al 2026-10-21. No certifica ejecución. El proyecto tiene capas/módulos Jakarta EE; no se lo declara hexagonal por usar interfaces ni se propone una migración general.

## Módulos reales

| Módulo | Responsabilidad y evidencia | Límite a preservar |
| --- | --- | --- |
| [carga-api](../carga-api/src/main/java/uy/edu/fing/tse/carga/api/) | `contract/service`, DTO, excepciones, roles y utilidades de observabilidad compartidas; [POM](../carga-api/pom.xml). | Contratos públicos; no introducir dependencias a EJB/web. No es un dominio puro: usa APIs Jakarta/observabilidad. |
| [carga-ejb](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/) | `modelo` JPA, `servicio` EJB, `persistencia` DAO, `jms`, `integracion`, `seguridad`, arranque y métricas. | Negocio, autorización y transacciones; consumir interfaces entre frentes. Algunos servicios usan `EntityManager` directamente: no imponer DAO nuevos sin necesidad. |
| [carga-web](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/) | Recursos REST, mappers, mecanismo JWT, filtro de correlación, backing beans JSF y SOAP. | Adaptar transporte/presentación; depende de API, no EJB concreto. Algunos recursos P4 son además EJB protegidos por rol (ADR-0007). |
| [carga-ear](../carga-ear/pom.xml) | Empaqueta API en `lib`, EJB y WAR; contexto `/carga`. | Empaquetado, no reglas de negocio. |
| [carga-front](../carga-front/src/) | React/TS: `pages`, `componentes`, `auth`, cliente HTTP y tipos; nginx en despliegue. | Consume REST; mostrar/ocultar rutas no autoriza recursos del servidor. Fuera del EAR y de Maven. |
| [simulador-pdi](../simulador-pdi/src/) | Servicio SOAP `ServicioDnicImpl`, usa contratos API. | Sustituto externo local, no fuente para redefinir la PDI real. |
| [nodo-balanzas](../nodo-balanzas/src/) | WAR REST `PesadasRecurso`, filtro de token; depende de API. | Integración HTTP independiente; no depende de implementaciones del central. |
| [nodo-tracking](../nodo-tracking/src/) | Proceso `NodoTracking`, solo JDK según su POM. | Publica por HTTP, no accede al broker central directamente. |
| [carga-generador](../carga-generador/src/) | Cliente/generador de carga y estadísticas. | Herramienta de performance, no servicio productivo ni evidencia de capacidad por sí sola. |

Dirección de dependencia relevante: `web → api ← ejb`; el EAR los integra. Los nodos/simuladores no importan `carga-ejb`. Verificar los POM afectados antes de añadir dependencias.

## Recorridos de lectura por funcionalidad

Las rutas Java siguientes son relativas a los directorios de paquetes enlazados en la tabla.

| Flujo | Archivos para seguir la ejecución | Decisiones |
| --- | --- | --- |
| Identidad | web `seguridad/MecanismoJwt`, recursos de autenticación/gub.uy → EJB `AutenticacionBean`, `IngresoGubUyBean`, `PdiBean` y paquete `seguridad` | Contrato de roles, caso crítico 1; no validar JWT solo en el navegador |
| Crear guía | web `rest/GuiaRecurso` → API `GuiaServicioLocal` → EJB `GuiaServicioBean` → `Guia`, `Vehiculo`, `Chofer`, `VinculoEmpresaUsuario` | ADR-0005 para bloqueo/intervalos; autorización empresarial actual según contrato y código, ver discrepancia |
| Perfil propio F10 | SPA `PerfilEmpresa` → web `rest/EmpresaRecurso` (GET/PUT perfil) → API `PerfilEmpresaServicioLocal` → EJB `PerfilEmpresaServicioBean` y DAO de vínculos P1 | ADR-0014/C10: cuatro campos, RESPONSABLE/ACTIVO y empresa activa; separado de ABM administrativo/remoto, validación real pendiente |
| Empresas funcionario F12 | SPA Mi cuenta → `pages/funcionario/Empresas` / `DetalleEmpresa` → web `rest/EmpresaRecurso` → API `EmpresaServicioLocal` → EJB `EmpresaServicioBean` | ADR-0013/C12: listado completo, edición administrativa y baja existente, sin nuevo GET por ID; bandeja de responsables P1 reutilizada. Enlaces F13/C13 y Viajes/C14 P2 presentes. Aceptación real pendiente |
| Consulta vehicular F13 | SPA `pages/funcionario/Vehiculos` → web `rest/ConsultaVehiculosRecurso` → API `ConsultaVehiculosLocal` → EJB `ConsultaVehiculosBean` | ADR-0016/C13: lectura paginada por empresa FUNCIONARIO/ADMINISTRADOR sin vínculo; referencia America/Montevideo y vigencias separadas de baja. No altera ABM; validación real pendiente |
| Consulta de viajes F14 P2 | SPA `pages/funcionario/Viajes` / `DetalleViaje` → web `rest/ConsultaGuiasRecurso` → API `ConsultaGuiasLocal` → EJB `ConsultaGuiasBean`; detalle válido → `PesajesViaje` → `api/pesajes` → web `rest/PesajeRecurso` → API `SincronizacionPesajesLocal.registradasDeGuia` → EJB/DAO P4 | ADR-0018/C14 y C14-P: lectura por empresa, estado persistido y pesajes ya asociados de varios días, hasta 500 con truncado/disponibilidad. No altera CU2 ni sincroniza; historial P3 para funcionario pendiente. Pruebas Java y matriz frontend sin ejecución; sin runner SPA |
| Tracking | web `rest/IngestaRecurso`/`FiltroTokenNodo` → EJB `IngestaTrackingBean` → cola posiciones → `PosicionMDB` → `PosicionDAO` | ADR-0002, 0007, 0008: recepción asíncrona, token, deduplicación y saturación |
| Pesajes | `AgendaPesajesBean` o web `rest/PesajeRecurso` → `SincronizacionPesajesBean` → `PesajeBean`/`CircuitBreaker` → HTTP Balanzas → `PesadaDAO` y cola pesadas | ADR-0006, 0007, 0009: consulta degradada, persistencia y aviso transaccional, URL desde nodos registrados |
| Fin de viaje | API `AvisoFinDeViajeLocal` → `AvisoFinDeViajeBean` → cola viajes-finalizados → `FinDeViajeMDB` → sincronización | ADR-0010; no confundir adaptador P4 existente con emisor P3 ya integrado |
| Eventos y conflictos P3/P2 | web `rest/EventoViajeRecurso` → API `EventoViajeLocal` → `EventoViajeBean` → `EventoViajeDAO`; FIN → `AvisoFinDeViajeLocal` | ADR-0019 aprobado por usuario: INICIO Vehículo → Guía comparte exclusión con baja P2; lectura actual y cuerpo idempotente normalizado, inserción atómica global. Sin cambios de rol; pruebas escritas sin ejecución, coordinación móvil/aceptación real pendiente |
| Recorridos | web `rest/RecorridoRecurso` → `RecorridoBean` → `RecorridoDAO`/PostGIS; API local para P5 | ADR-0010: topes, distancia y roles distintos para API externa/proceso interno |
| Auditoría del producto | `AuditoriaBean` → `RegistroAuditoriaDAO`/`RegistroAuditoria`; lectura por recurso y SPA | Solo agregado; independiente del agente auditor de código |

## Persistencia, servidor y pruebas

- [Migraciones](../carga-ejb/src/main/resources/db/migration/): V1–V9 presentes en esta revisión; consulta el último número real antes de crear otra. [Migraciones.java](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/arranque/Migraciones.java) aplica Flyway al arrancar. [persistence.xml](../carga-ejb/src/main/resources/META-INF/persistence.xml) declara JTA y `hbm2ddl.auto=none`. ADR-0001 prohíbe editar migraciones ya aplicadas.
- [configure.cli](../docker/central/configure.cli): datasource, JMS/Artemis y servidor; [Compose](../docker-compose.yml) enlaza contenedores y configuración. HTTPS es requisito de entornos desplegados; las URLs HTTP locales no demuestran despliegue seguro en nube.
- [Pruebas EJB](../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/): reglas, servicios, seguridad, JMS, resiliencia y migraciones. `GuiaServicioBeanTest` no sustituye una transacción/lock del contenedor; ADR-0005 reconoce esa limitación.
- [Pruebas web](../carga-web/src/test/), [simulador](../simulador-pdi/src/test/) y [generador](../carga-generador/src/test/) complementan; la prueba de migraciones usa Testcontainers y etiqueta `integracion`. [CI](../.github/workflows/ci.yml) define build Java, frontend e integración/E2E. No se inspeccionaron resultados remotos ni se ejecutaron estas pruebas.
