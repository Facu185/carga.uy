# F12 — Empresas desde el área del funcionario

## Alcance y orden de trabajo

Escenarios definidos **antes de escribir la interfaz** el 2026-10-22. La SPA no
tiene runner de pruebas: esta matriz NO es automatización ni ciclo rojo/verde.
Los tests Java de regresión se preparan antes de integrar el consumidor; el
servicio administrativo existente no se reimplementa. No se ejecutan builds.

Entrada: gub.uy → `/mi-cuenta` → `/funcionario/empresas` →
`/funcionario/empresas/:id`. Las rutas SPA no son endpoints REST.
Solo `FUNCIONARIO` accede al área nueva; el administrador conserva sus accesos
anteriores. El backend mantiene `FUNCIONARIO`/`ADMINISTRADOR` para mutaciones.

Se consumen C12: `GET /empresas[?nombre=]`, `PUT /empresas/{id}` (200 DTO) y
`DELETE /empresas/{id}` (204 sin cuerpo), relativos a `/carga/api`.
No existe GET individual: el detalle directo consulta el listado y selecciona
por ID. No hay paginación publicada; esta limitación se mantiene explícita.
No se publica POST de alta ni se cambia autenticación, roles o interfaces Java.

## Matriz pendiente de ejecución por el equipo

Usar entorno aislado, empresas y personas sintéticas, vínculos activos y tokens
emitidos por el flujo real. No reutilizar fixtures de carreras CU2/F11 ni guardar
tokens, cédulas o respuestas sensibles en la evidencia.

| Caso | Resultado esperado |
| --- | --- |
| Funcionario gub.uy, sin vínculo empresarial | Entrada desde Mi cuenta; listado global sin exigir responsabilidad. |
| Mi cuenta con FUNCIONARIO y CIUDADANO (roles emitidos por gub.uy) | Muestra las pestañas Mi cuenta, Empresas y Solicitudes de responsables, y Tus datos. No muestra la tarjeta Área del funcionario/Gestionar empresas, Guías de tu empresa, Registrar guía, Responsabilidad de empresa ni Pedir acceso como funcionario. |
| Navegación compartida del funcionario | Las mismas tres pestañas aparecen en Mi cuenta, listado/detalle de empresas y solicitudes. Mi cuenta queda activa solo en `/mi-cuenta`; Empresas también en el detalle; Solicitudes de responsables solo en su bandeja. Cada enlace conserva su URL existente, recarga e historial. |
| Mi cuenta sin FUNCIONARIO | No muestra las pestañas operativas de funcionario; conserva los accesos propios de ciudadano/chofer. Las rutas de funcionario siguen protegidas. |
| Pestañas con teclado, zoom y pantalla estrecha | Enlaces alcanzables con Tab y activables con Enter; la sección activa se identifica con `aria-current="page"`, peso y subrayado. La barra permite desplazamiento horizontal sin ocultar destinos. |
| Mi cuenta con FUNCIONARIO combinado con CHOFER o vínculo RESPONSABLE previo | Prevalece la presentación de funcionario: no muestra Tus guías, Administración de empresa ni Guías de viajes. No revoca vínculos ni modifica roles del servidor. |
| Funcionario al abrir Mi cuenta y recuperar el foco de la pestaña | No consulta `/solicitudes-empresa/vinculos/mios` ni monta los formularios SolicitudResponsable/SolicitudAcceso; conserva la consulta `/mi-perfil`. |
| Ciudadano o chofer sin FUNCIONARIO, con/sin responsabilidad activa | Conserva sus opciones anteriores, consulta de vínculos, solicitud de responsabilidad y reconsulta al recuperar el foco. |
| Sesión con rol principal FUNCIONARIO y sin colección `roles` | Aplica la misma presentación exclusiva de funcionario mediante el fallback existente. |
| Sesión ciudadana sustituida por una con FUNCIONARIO durante una consulta de vínculos | Desaparecen las opciones empresariales y una respuesta tardía no las vuelve a mostrar ni actualiza el estado de vínculos de la vista de funcionario. |
| URL directa, recarga y volver atrás | Detalle de la misma empresa; filtro de listado recuperable en historial. |
| Sin sesión; sesión realmente vencida | Redirección a `/ingresar`; REST directo sin JWT/vencido responde 401. |
| Ciudadano, chofer, autoridad o administrador sin FUNCIONARIO en ruta nueva | Acceso SPA denegado, sin formulario ni llamadas empresariales desde esa ruta. |
| REST GET con cada uno de los cinco roles | Se mantiene lectura publicada. PUT/DELETE solo funcionario/administrador; otros roles 403 sin cambios. |
| Administrador interno y JSF | Conservan ABM y flujos previos; no se impone un nuevo login gub.uy. |
| Búsqueda parcial por nombre, vacía y sin resultados | Query codificada; carga, vacío y error distinguibles; botón de reconsulta. |
| Cambiar filtro/ruta/cuenta con respuesta anterior demorada | No aparecen filas, borradores ni mensajes de otra consulta/identidad. |
| ID inválido, inexistente; listado o DTO no interpretable | Error explícito; no PUT/DELETE ni GET por ID inventado. |
| Modificación válida, incluida fecha de alta | PUT solo nombre, razón social, número, dirección y fechaAlta; respuesta 200 actualiza la ficha. ID/activa no editables. |
| Fecha de alta ausente | Aviso explícito: el servicio administrativo completa fecha actual al guardar; no es el perfil F10. |
| Nombre vacío, número no positivo/no entero/no representable, fecha futura | UI rechaza lo básico; REST mantiene validaciones existentes 400. Número fuera del rango seguro JS impide edición sin redondearlo. |
| Número duplicado / 400 | Mensaje de negocio, se conserva borrador y se permite corregir; no baja accidental. |
| Cancelar cambios | Restaura la ficha consultada/confirmada, no envía requests. |
| Doble clic durante guardado o confirmación de baja | Una sola mutación; acciones mutuamente excluidas. |
| Cancelar baja con botón/Escape | Ningún DELETE; foco retorna al botón. |
| Baja confirmada | DELETE204 sin parsear cuerpo; reconsulta: empresa inactiva, sin opción de reactivar. Histórico preservado, vínculos activos revocados y auditoría persistida. |
| Empresa ya inactiva | Baja deshabilitada; edición administrativa mantiene comportamiento existente y no reactiva. |
| Fallo red/5xx/respuesta de guardado inválida | Resultado incierto: no reenvío automático; bloquear escrituras hasta reconsultar. |
| 401/403 al consultar o mutar | No conservar formulario operativo; 401 usa cierre de sesión existente. |
| Fallo de auditoría durante baja (entorno con inyección de fallo controlada) | Verificar en transacción nueva rollback de empresa, vínculos y auditoría; revertir mecanismo de fallo del entorno. Un mock NO acredita JTA. |
| Vehículos / Viajes | Vehículos enlaza a la consulta F13/C13 de solo lectura (ver matriz específica); Viajes sigue deshabilitado, sin consumir C14 ni simular viajes. |
| Solicitudes de responsables | Reutiliza componente y endpoints P1 existentes; NO es bandeja de altas de empresas. El alta sigue pendiente de coordinación P1. |
| Título de la bandeja reutilizada | Identifica Área del funcionario en el recorrido nuevo; el backoffice conserva su título y comportamiento anterior. |
| Teclado, zoom y pantalla estrecha | Etiquetas, mensajes anunciados, tabla desplazable, confirmación con foco; no depender de color. |

## Evidencia y límites

- La consulta de vehículos de fase 4 se especifica y valida por separado en
  [F13/C13](p2-consulta-vehicular.md); su implementación local no acredita F12 integrada.

- La incompatibilidad del servidor se especifica por separado en
  [ADR-0015](../docs/decisiones/0015-incompatibilidad-funcionario-responsable.md)
  y su [matriz pendiente](incompatibilidad-funcionario-responsable.md). Es una
  restricción posterior al ajuste visual y no retira funciones oficiales de F12.
- Regresión visual especificada antes del ajuste de Mi cuenta: el ingreso OIDC
  conserva CIUDADANO al asignar FUNCIONARIO; antes, ambos bloques se mostraban.
  Se prioriza FUNCIONARIO solo en la presentación de Mi cuenta. No se cambian
  rutas directas, permisos REST ni vínculos existentes; ocultar accesos NO impone
  una incompatibilidad de roles en el servidor. Los escenarios nuevos están
  **pendientes de ejecución**, sin runner frontend ni ciclo rojo/verde verificado.
- `EmpresaServicioBeanSeguridadTest`: lectura y declaraciones de roles por operación.
- `EmpresaServicioBeanAdministracionTest`: edición administrativa, errores previos
  a mutación, baja/revocación y propagación de fallo de auditoría con dobles.
- Las pruebas Java no acreditan interceptores de seguridad, JSON, JPQL ni JTA.
- **Pendiente:** compilar/ejecutar por el equipo, ejecutar toda la matriz en
  WildFly/PostgreSQL y navegador, registrar versión, identidad sintética, resultados
  y evidencia sin secretos. No se han realizado llamadas HTTP ni pruebas funcionales.
- C01 (navegación compartida) y coordinación de altas P1 requieren aceptación
  integrada. No se consideran cerrados por añadir rutas locales.
