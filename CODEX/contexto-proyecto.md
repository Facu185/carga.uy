# Contexto del proyecto

## Propósito y fuentes

carga.uy gestiona transporte de carga terrestre para el laboratorio 2026 del Grupo 5 (FIng/UdelaR). La fuente de actores y flujos críticos es [GuiaCUCrit.md](../docs/GuiaCUCrit.md); el [README](../README.md) describe el entorno común. La letra del laboratorio y el SAD se mencionan en las reglas, pero no se incluyen como documentos completos en `docs/`: si la tarea depende de ellos, solicita la fuente, no reconstruyas requisitos de memoria.

Los seis casos críticos son: ingreso con gub.uy y PDI degradada; creación de guías; registro de eventos offline/idempotentes; obtención de pesajes; detección automática de incumplimientos; ciclo de fiscalización. La existencia de un caso documentado NO prueba que esté implementado.

Los [frentes](../docs/frentes.md) distribuyen responsabilidades: P1 identidad/PDI/auditoría; P2 núcleo/guías; P3 frontoffice/eventos; P4 nodos/integración; P5 fiscalización/georreferenciación. Ese documento asigna trabajo, no certifica progreso actual.

## Base técnica observada (2026-10-21)

- [POM raíz](../pom.xml): reactor de ocho módulos Java, release 24, Jakarta EE 11 y JUnit 5; perfil `integracion` para incluir pruebas etiquetadas. Versiones concretas se consultan allí, no se presuponen por esta guía.
- [Dockerfile del central](../docker/central/Dockerfile): WildFly 41.0.0.Final con imagen JDK 25; es distinto del release 24 de compilación. No confundir runtime con versión objetivo de bytecode.
- [carga-front/package.json](../carga-front/package.json): React, TypeScript y Vite; SPA fuera del reactor Maven. El script `build` ejecuta `tsc -b` y Vite; no existe script `test` en ese archivo.
- [Compose](../docker-compose.yml): central, PostGIS/PostgreSQL, Balanzas, Tracking, frontend y simulador OIDC; perfiles adicionales de LDAP, carga y monitoreo. PDI SOAP simulada forma parte del despliegue definido para el central.
- Hay pruebas JUnit en varios módulos y un [script de punta a punta](../pruebas/punta-a-punta.sh). Su presencia no acredita resultados ni cobertura.

## Invariantes que deben conservarse

Consulta el detalle en [reglas](../docs/Estandares/RULES.md) y [contratos](../docs/contratos.md): autorización y pertenencia verificadas en el servidor; contratos coordinados entre frentes; migraciones nuevas con Flyway, no DDL automático; transacciones coherentes; idempotencia en reentregas; integraciones con timeout/reintentos limitados/circuito; secretos externos; datos públicos agregados; auditoría de negocio de solo agregado. No expongas entidades JPA como DTO ni dependas de implementaciones internas desde otros módulos.

## Discrepancias conocidas: investigar antes de extender

Estos son contrastes documentales/estáticos, NO una auditoría funcional ni autorización para corregirlos dentro de otra tarea.

| Tema | Evidencia contrastada | Conducta del agente |
| --- | --- | --- |
| Frontend | [ADR-0004](../docs/decisiones/0004-frontend-react-sobre-api-rest.md) sigue **Propuesta**; existen [SPA](../carga-front/src/App.tsx), contenedor y job frontend. [ADR-0013](../docs/decisiones/0013-area-funcionario-spa-p2.md) acepta solo el área P2 F12–F14 en la SPA existente. Los estándares Java no se amplían globalmente. | Conservar JSF y autenticación P1; no confundir la aceptación acotada con migración general. Navegación compartida y contratos nuevos pendientes en el [registro de fase 0](../docs/acuerdos-cierre-p2-fase-0.md). |
| Autorización de guías | [ADR-0005](../docs/decisiones/0005-autorizacion-y-concurrencia-de-guias.md) fue alineado documentalmente el 2026-10-22 con `VinculoEmpresaUsuario`. Creación/catálogos empresariales en [GuiaServicioBean](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/GuiaServicioBean.java) exigen ACTIVO sin tipo; listado empresarial y ABM vehicular exigen RESPONSABLE/ACTIVO (con excepción administrativa solo en ABM). | No restaurar `usuario.empresa_id` ni unificar silenciosamente los permisos. D0-04 sigue pendiente de acuerdo P1/P2; la corrección documental no cambia lógica. |
| Errores REST | [Estándar API](../docs/Estandares/api-design.md) propone un sobre con código/detalles y distinción 409/422; contrato y [Errores.java](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/Errores.java) usan `{"error":"mensaje"}`; [NegocioExceptionMapper](../carga-web/src/main/java/uy/edu/fing/tse/carga/web/rest/NegocioExceptionMapper.java) devuelve 400. El cliente SPA espera ese formato. | No cambiar forma/códigos publicados unilateralmente; diseñar compatibilidad y coordinar antes. |
| Builds y commits | [CONTRIBUTING](../CONTRIBUTING.md) pide compilar y muestra commits sin prefijo; [AGENTS.md](../AGENTS.md) prohíbe builds para el agente y exige Conventional Commits cuando se soliciten. | Para el agente se aplican las restricciones de AGENTS; no alterar CI ni declarar pruebas verdes sin ejecutarlas. |

## Pendientes no resueltos por esta guía

- La guía de casos críticos mantiene políticas a definir sobre conflictos offline y vencimiento de descargos. Consultar ADR y equipo antes de inventar una transición o regla.
- [ADR-0010](../docs/decisiones/0010-integraciones-de-p4-con-p3-y-p5.md) y contratos describen el enlace de P3 al aviso de fin de viaje y el consumo futuro de pesadas por P5. En el código inspeccionado existen los adaptadores P4 (`AvisoFinDeViajeBean`, `FinDeViajeMDB`), no un flujo completo P3/P5 acreditado.
- El panel JSF sin inicio de sesión figura como limitación conocida en [contratos](../docs/contratos.md) y [ADR-0007](../docs/decisiones/0007-seguridad-de-nodos-y-endpoints-p4.md); no usarlo como patrón seguro para nuevas pantallas.
- El objetivo ~80 % de cobertura, la latencia p95 y el volumen de Tracking son requisitos/objetivos, no resultados medidos en esta preparación. No se ejecutaron app, builds ni pruebas.
