# Reglas y restricciones de carga.uy

Este documento traduce las restricciones del laboratorio 2026 y las decisiones del SAD a reglas verificables para el equipo. Ante una discrepancia, prevalecen la letra del laboratorio y el SAD acordado por el grupo. Para implementar un caso de uso crítico se debe consultar además la [guía de casos de uso críticos](../GuiaCUCrit.md).

## Restricciones del producto

| ID | Regla obligatoria |
| --- | --- |
| R001 | Los frontoffices y backoffices deben proveer una vista web. El componente móvil complementa al frontoffice del chofer; no lo sustituye. |
| R002 | Se debe respetar la normativa de protección de datos personales. Las vistas públicas y los reportes públicos solo expondrán información agregada que no permita identificar una empresa, vehículo o chofer. El umbral de agregación debe documentarse y justificarse. |
| R003 | El componente central se desplegará en Elastic Cloud de ANTEL. |
| R004 | Los nodos periféricos se desplegarán en una solución PaaS o IaaS independiente del componente central. |
| R005 | La integración con la PDI se realiza mediante servicios web SOAP. |
| R006 | La comunicación entre el componente móvil y el central se realiza mediante servicios REST sobre HTTPS. |
| R007 | Todos los componentes se distribuyen contenerizados y el entorno local completo debe levantarse con un único comando documentado en el README. |
| R008 | El pipeline de integración continua debe compilar, ejecutar pruebas automatizadas y análisis estático en cada modificación; debe estar exitoso en la entrega final. |

## Reglas de arquitectura e implementación

1. **Plataforma:** el componente central se implementa en Java con Jakarta EE 11 sobre WildFly. Se usan JAX-RS para REST, JAX-WS/JAXB para SOAP, JMS/MDB para procesamiento asíncrono, JPA para persistencia y JSF + PrimeFaces para las vistas web existentes.
2. **Capas y módulos:** los recursos REST, vistas JSF y endpoints SOAP solo adaptan el transporte o la presentación; las reglas de negocio, autorización y transacciones residen en los EJB/servicios. Los contratos y DTO compartidos viven en `carga-api`; sus implementaciones viven en `carga-ejb`.
3. **Contratos:** no se cambia unilateralmente una interfaz, DTO, endpoint, cola ni tabla compartida. Se sigue [contratos.md](../contratos.md) y se coordina el cambio antes de implementarlo.
4. **Esquema:** Flyway gobierna la base de datos. No se habilita generación o actualización automática de esquema por JPA/Hibernate y no se edita una migración ya aplicada; cada cambio se expresa en una migración nueva.
5. **Integraciones:** Tracking se recibe de forma asíncrona: el endpoint acusa recepción tras encolar y un MDB procesa después. Balanzas se consulta de forma síncrona bajo demanda, con timeout, reintentos limitados, mecanismo de corte y comportamiento degradado. La indisponibilidad de PDI o Balanzas no puede dejar indisponible al central.
6. **Idempotencia:** cada evento originado en el móvil, Tracking o una sincronización lleva un identificador estable generado en el origen. El procesamiento debe poder repetirse sin duplicar eventos, pesajes ni casos de fiscalización.
7. **Estado y concurrencia:** las instancias del central no guardan estado de sesión local. Las operaciones que asignan vehículo o chofer, o cambian un caso de fiscalización, deben controlar concurrencia y ejecutarse en una transacción coherente.
8. **Fiscalización:** las reglas de exceso de carga, vehículo sin habilitación, viaje sin guía y desvío de ruta se ejecutan asíncronamente y de forma parametrizable. Cada hecho genera como máximo un caso. Las transiciones permitidas son `detectada → notificada → en descargo → confirmada|desestimada`; ninguna transición fuera de ese ciclo es válida.
9. **Auditoría:** la creación, cambio de estado o resolución de un caso de fiscalización genera un registro de solo agregado con usuario, acción, fundamento y marca de tiempo. La aplicación no puede modificar ni eliminar esos registros.
10. **Configuración y secretos:** URLs, timeouts, credenciales, claves, certificados y umbrales se obtienen de configuración externa. No se versionan secretos ni se los registra en logs.

## Calidad y verificación

- Las operaciones autenticadas autorizan en la capa de servicios, nunca solo ocultando opciones de la interfaz.
- Toda comunicación con el móvil, nodos periféricos y plataformas externas usa HTTPS en los entornos desplegados.
- El percentil 95 de las consultas del frontoffice debe ser menor a 500 ms durante la prueba de performance. Se debe sostener el volumen estimado de Tracking sin pérdida de eventos y documentar el punto de quiebre y los cuellos de botella.
- Los componentes emiten logs estructurados con identificador de correlación, exponen métricas y health checks. Al menos un flujo que atraviese móvil, central y nodo periférico debe contar con trazado distribuido.
- Se apunta a aproximadamente 80 % de cobertura de la lógica de negocio y al menos una prueba por servicio. Las pruebas cubren el ciclo de vida de fiscalización, la idempotencia y los escenarios de caída de PDI y Balanzas.

## Regla de decisión pendiente

Si un caso de uso o el SAD deja una política “a definir” (por ejemplo, conflicto móvil/frontoffice, vencimiento de descargo, autenticación móvil o criterio de desvío), no se inventa una conducta en una clase aislada. Se acuerda, se documenta en un ADR y se actualiza la guía antes de depender de ella.
