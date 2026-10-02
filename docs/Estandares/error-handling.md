# Manejo de errores, auditoría y observabilidad

## Hacer

- Usar el logger de Java del servidor (`java.util.logging.Logger`) o el logger centralizado configurado para WildFly; no usar salida estándar. La configuración de producción debe emitir JSON estructurado.
- Incluir en cada log estructurado, como mínimo: timestamp, nivel, mensaje, `correlation_id` y nombre del componente. Incluir identificadores de empresa, viaje o caso solo cuando sean necesarios y no constituyan datos personales expuestos.
- Crear o aceptar un identificador de correlación en el borde HTTP y propagarlo a EJB, JMS y llamadas REST/SOAP salientes. Cada `TextMessage` de JMS debe llevarlo como propiedad o dentro de su sobre JSON.
- Usar niveles de Java de forma consistente: `FINE` para diagnóstico de desarrollo; `INFO` para eventos de negocio y operaciones completadas; `WARNING` para condiciones recuperables, reintentos o degradación; `SEVERE` para fallas que requieren investigación. Una falla irrecuperable de arranque debe impedir el inicio de forma explícita.
- Separar errores de validación y negocio de errores técnicos. Mapear `NegocioException` y conflictos de estado a respuestas REST seguras; centralizar los errores no previstos con `ExceptionMapper` JAX-RS y mensajes JSF genéricos en la web.
- Registrar el error una sola vez en la frontera que lo maneja. Propagar excepciones entre capas sin duplicar logs ni perder su causa.
- Aplicar timeouts, reintentos limitados y circuito de corte en integraciones remotas. Cuando PDI o Balanzas fallen, dejar el trabajo pendiente o degradar la funcionalidad según el caso de uso, y registrar la condición con el identificador de correlación.
- Mantener una auditoría de solo agregado para toda acción de fiscalización: usuario, acción, fundamento y marca de tiempo. La auditoría es distinta de los logs operativos y no se modifica ni elimina desde la aplicación.
- Exponer health checks y métricas de eventos procesados, latencia, colas y errores por tipo. El informe de performance debe usar esta evidencia.

## No hacer

- No capturar `Exception` para ignorarla. Todo `catch` debe manejar, traducir o propagar el error; si lo maneja en la frontera, debe registrarlo con contexto.
- No mostrar excepciones, stack traces, nombres de clases, rutas internas ni dependencias al cliente REST o a la interfaz JSF.
- No registrar contraseñas, tokens, secretos, cookies de sesión, documentos de identidad, fotos de incidentes ni cuerpos completos de solicitudes. Enmascarar datos personales antes de loguearlos.
- No usar `SEVERE` para resultados esperables, como validación inválida, duplicado idempotente o recurso inexistente.
- No registrar el mismo error en recurso, servicio y cliente remoto. Un log por falla manejada permite investigar sin ruido.
